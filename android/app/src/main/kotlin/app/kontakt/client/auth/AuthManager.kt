package app.kontakt.client.auth

import androidx.lifecycle.LifecycleOwner
import app.kontakt.client.BuildConfig
import com.vk.id.VKID
import com.vk.id.AccessToken
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    data object SignedIn : AuthState
    data class SignedOut(val message: String? = null) : AuthState
}

/** VK ID SDK authentication; tokens are stored encrypted by TokenStore. */
class AuthManager(private val store: TokenStore) {
    val isConfigured: Boolean = BuildConfig.VK_ID_CONFIGURED
    @Volatile private var cached: String? = store.load()
    @Volatile private var lifecycleOwner: LifecycleOwner? = null
    private val _state = MutableStateFlow<AuthState>(if (cached != null) AuthState.SignedIn else AuthState.SignedOut())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached
    fun attachLifecycleOwner(owner: LifecycleOwner) { lifecycleOwner = owner }
    fun detachLifecycleOwner(owner: LifecycleOwner) {
        if (lifecycleOwner === owner) lifecycleOwner = null
    }

    fun signIn() {
        if (!isConfigured || _busy.value) {
            if (!isConfigured) {
                _state.value = AuthState.SignedOut("VK ID не настроен в сборке. Проверьте VK_CLIENT_ID и VK_CLIENT_SECRET в GitHub Actions.")
            }
            return
        }
        val owner = lifecycleOwner
        if (owner == null) {
            _state.value = AuthState.SignedOut("Экран входа ещё запускается. Попробуйте ещё раз.")
            return
        }
        _busy.value = true
        try {
            val scopes = BuildConfig.VK_SCOPES
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
            val params = VKIDAuthParams {
                if (scopes.isNotEmpty()) this.scopes = scopes
            }
            VKID.instance.authorize(owner, object : VKIDAuthCallback {
                override fun onAuth(accessToken: AccessToken) {
                    _busy.value = false
                    try {
                        val token = accessToken.token
                        store.save(token)
                        cached = token
                        _state.value = AuthState.SignedIn
                    } catch (_: Exception) {
                        cached = null
                        _state.value = AuthState.SignedOut(
                            "VK выполнил вход, но Kontakt не смог безопасно сохранить сессию. Повторите попытку."
                        )
                    }
                }

                override fun onFail(fail: VKIDAuthFail) {
                    _busy.value = false
                    val detail = fail::class.simpleName.orEmpty()
                    val description = fail.description.take(180)
                    val message = if (detail.contains("Canceled", ignoreCase = true)) {
                        "Вход отменён. Нажмите «Продолжить через VK», чтобы попробовать снова."
                    } else {
                        "VK ID не завершил вход ($detail): $description"
                    }
                    _state.value = AuthState.SignedOut(message)
                }
            }, params)
        } catch (e: Exception) {
            _busy.value = false
            _state.value = AuthState.SignedOut(
                "Не удалось запустить VK ID (${e.javaClass.simpleName}). Проверьте настройки приложения и подпись."
            )
        }
    }

    fun onSessionExpired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }

    fun signOut() = wipe(null)

    private fun wipe(message: String?) {
        // Clear the local encrypted session. The next sign-in is delegated to VK ID SDK.
        store.clear()
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }
}
