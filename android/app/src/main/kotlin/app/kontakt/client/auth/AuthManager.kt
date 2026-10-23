package app.kontakt.client.auth

import app.kontakt.client.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    data object SignedIn : AuthState
    data class SignedOut(val message: String? = null) : AuthState
}

/** VK authentication state. The official SDK owns the sign-in UI; Kontakt only receives the resulting token. */
class AuthManager(private val store: TokenStore) {
    val isConfigured: Boolean = BuildConfig.VK_ID_CONFIGURED
    @Volatile private var cached: String? = store.load()
    private var launcher: (() -> Unit)? = null
    private val _state = MutableStateFlow<AuthState>(if (cached != null) AuthState.SignedIn else AuthState.SignedOut())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached
    fun attachLauncher(value: () -> Unit) { launcher = value }
    fun detachLauncher() { launcher = null }

    fun signIn() {
        if (!isConfigured || _busy.value) return
        val startAuth = launcher
        if (startAuth == null) {
            _state.value = AuthState.SignedOut("Экран входа ещё запускается. Попробуйте ещё раз.")
            return
        }
        _busy.value = true
        try {
            startAuth()
        } catch (e: Exception) {
            _busy.value = false
            _state.value = AuthState.SignedOut("Не удалось открыть окно авторизации VK (${e.javaClass.simpleName}). Проверьте настройки приложения и подпись.")
        }
    }

    fun onVkAuthSuccess(token: String) {
        _busy.value = false
        try {
            require(token.isNotBlank()) { "VK ID returned an empty access token" }
            store.save(token)
            cached = token
            _state.value = AuthState.SignedIn
        } catch (_: Exception) {
            cached = null
            _state.value = AuthState.SignedOut("VK ID выполнил вход, но Kontakt не смог безопасно сохранить сессию. Повторите попытку.")
        }
    }

    /** User-initiated cancellation is not an authentication error; return to the signed-out state quietly. */
    fun onVkAuthCanceled() {
        _busy.value = false
        _state.value = AuthState.SignedOut()
    }

    fun onVkAuthFailure(details: String) {
        _busy.value = false
        _state.value = AuthState.SignedOut("VK не завершил авторизацию: $details")
    }

    fun onSessionExpired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }
    fun signOut() { wipe(null) }
    private fun wipe(message: String?) {
        store.clear()
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }
}
