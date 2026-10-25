package app.kontakt.client.auth

import app.kontakt.client.BuildConfig
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Вход через официальный VK ID SDK (OAuth 2.1 + PKCE). Токен хранится зашифрованным в [TokenStore].
 *
 * Вход запускается не от Activity, а от области процесса: SDK сам открывает окно VK из контекста приложения,
 * а результат возвращается в статический слушатель SDK. Поэтому поворот экрана или пересоздание Activity,
 * пока открыто окно VK, не теряют результат. Состояние (`state`, `busy`) ведёт [AuthFlow].
 */
class AuthManager(
    private val store: TokenStore,
    private val sdkInitError: String? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    val isConfigured: Boolean = BuildConfig.VK_ID_CONFIGURED

    private val flow = AuthFlow(
        sink = object : SessionSink {
            override fun save(token: String) = store.save(token)
            override fun clear() = store.clear()
        },
        initialToken = store.load(),
    )
    private var watchdog: Job? = null

    val state: StateFlow<AuthState> get() = flow.state
    val busy: StateFlow<Boolean> get() = flow.busy

    fun token(): String? = flow.token()

    fun signIn() {
        if (flow.busy.value) return
        if (!isConfigured) {
            flow.reject(
                "Приложение VK не настроено в этой сборке: не задан числовой ID приложения " +
                    "(переменная VK_STANDALONE_APP_ID в GitHub Actions).",
            )
            return
        }
        if (sdkInitError != null) {
            flow.reject("Не удалось запустить VK ID ($sdkInitError). Эта сборка не может начать вход.")
            return
        }
        if (!flow.begin()) return

        val requestedScopes = BuildConfig.VK_SCOPES
            .split(',', ' ')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        val params = VKIDAuthParams {
            // Пустой набор: SDK запрашивает только базовое право. Права, которые не включены для приложения
            // в кабинете VK ID, делают запрос недействительным, поэтому по умолчанию их не просим.
            if (requestedScopes.isNotEmpty()) scopes = requestedScopes
        }
        val callback = object : VKIDAuthCallback {
            override fun onAuth(accessToken: AccessToken) {
                watchdog?.cancel()
                flow.succeeded(accessToken.token)
            }

            override fun onFail(fail: VKIDAuthFail) {
                watchdog?.cancel()
                flow.failed(describeAuthFailure(fail.toKind(), fail.description))
            }
        }

        watchdog?.cancel()
        watchdog = scope.launch {
            delay(AUTH_TIMEOUT_MS)
            flow.abandoned()
        }
        scope.launch {
            try {
                VKID.instance.authorize(callback, params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                watchdog?.cancel()
                flow.reject(
                    "Не удалось запустить вход VK (${e.javaClass.simpleName}" +
                        sanitizeDiagnostic(e.message, MAX_EXCEPTION_DETAIL).let { if (it.isEmpty()) "" else ": $it" } +
                        "). Проверьте настройки приложения.",
                )
            }
        }
    }

    fun onSessionExpired() = flow.expired()

    fun signOut() {
        flow.signOut()
        if (isConfigured && sdkInitError == null) {
            // Best-effort: отзывает токен на стороне VK и стирает копию, которую хранит сам SDK.
            scope.launch {
                runCatching {
                    VKID.instance.logout(object : VKIDLogoutCallback {
                        override fun onSuccess() = Unit
                        override fun onFail(fail: VKIDLogoutFail) = Unit
                    })
                }
            }
        }
    }

    private fun VKIDAuthFail.toKind(): AuthFailureKind = when (this) {
        is VKIDAuthFail.Canceled -> AuthFailureKind.CANCELED
        is VKIDAuthFail.FailedApiCall -> AuthFailureKind.API_CALL
        is VKIDAuthFail.FailedOAuth -> AuthFailureKind.OAUTH
        is VKIDAuthFail.FailedOAuthState -> AuthFailureKind.OAUTH_STATE
        is VKIDAuthFail.FailedRedirectActivity -> AuthFailureKind.REDIRECT
        is VKIDAuthFail.NoBrowserAvailable -> AuthFailureKind.NO_BROWSER
        else -> AuthFailureKind.OTHER
    }

    private companion object {
        /** Окно VK может быть открыто долго (SMS, подтверждение), но не бесконечно. */
        const val AUTH_TIMEOUT_MS = 5L * 60 * 1000
        const val MAX_EXCEPTION_DETAIL = 120
    }
}
