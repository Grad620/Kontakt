package app.kontakt.client.auth

import app.kontakt.client.BuildConfig
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object SignedIn : AuthState
    data class SignedOut(val message: String? = null) : AuthState
}

/**
 * Вход через официальный VK ID SDK (пароль вводится только на стороне VK, не в форме VG).
 * Запрашиваются только права из BuildConfig.VK_SCOPES; по умолчанию — базовый доступ.
 */
class AuthManager(private val store: TokenStore) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // VK ID needs both the public app ID and its protected client secret.
    val isConfigured: Boolean = BuildConfig.VK_ID_CONFIGURED

    @Volatile
    private var cached: String? = store.load()

    private val _state = MutableStateFlow<AuthState>(
        if (cached != null) AuthState.SignedIn else AuthState.SignedOut(),
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached

    fun signIn() {
        if (!isConfigured || _busy.value) return
        _busy.value = true

        val callback = object : VKIDAuthCallback {
            override fun onAuth(accessToken: AccessToken) {
                // Сначала надёжно сохраняем токен; не переводим UI в SignedIn,
                // если Android Keystore недоступен или шифрование завершилось ошибкой.
                try {
                    store.save(accessToken.token)
                    cached = accessToken.token
                    _state.value = AuthState.SignedIn
                } catch (_: Exception) {
                    cached = null
                    _state.value = AuthState.SignedOut(
                        "VK авторизовал вас, но Kontakt не смог безопасно сохранить сессию. Попробуйте ещё раз.",
                    )
                } finally {
                    _busy.value = false
                }
            }

            override fun onFail(fail: VKIDAuthFail) {
                _busy.value = false
                _state.value = AuthState.SignedOut(
                    if (fail is VKIDAuthFail.Canceled) {
                        "Вход отменён."
                    } else {
                        "Не удалось войти: ${fail.description}"
                    },
                )
            }
        }

        val requestedScopes = BuildConfig.VK_SCOPES
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        scope.launch {
            try {
                VKID.instance.authorize(
                    callback = callback,
                    params = VKIDAuthParams { this.scopes = requestedScopes },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _busy.value = false
                _state.value = AuthState.SignedOut("Не удалось открыть вход через VK ID.")
            }
        }
    }

    /** Токен недействителен (ответ VK с кодом 5): удаляем его и просим войти заново. */
    fun onSessionExpired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }

    /** Выход пользователя. Токен удаляется с устройства. */
    fun signOut() {
        wipe(null)
    }

    private fun wipe(message: String?) {
        store.clear()
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }
}
