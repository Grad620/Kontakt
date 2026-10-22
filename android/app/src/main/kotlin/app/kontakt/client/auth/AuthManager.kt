package app.kontakt.client.auth

import androidx.activity.result.ActivityResultLauncher
import app.kontakt.client.BuildConfig
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAuthenticationResult
import com.vk.api.sdk.auth.VKScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    data object SignedIn : AuthState
    data class SignedOut(val message: String? = null) : AuthState
}

/** Official classic VK Android SDK token flow. Passwords are entered only in VK's auth UI. */
class AuthManager(private val store: TokenStore) {
    val isConfigured: Boolean = BuildConfig.VK_STANDALONE_CONFIGURED
    @Volatile private var cached: String? = store.load()
    private var launcher: ActivityResultLauncher<Collection<VKScope>>? = null
    private val _state = MutableStateFlow<AuthState>(if (cached != null) AuthState.SignedIn else AuthState.SignedOut())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached
    fun attachLauncher(value: ActivityResultLauncher<Collection<VKScope>>) { launcher = value }
    fun detachLauncher() { launcher = null }

    fun onLegacyAuthSetupFailure(error: Throwable) {
        _busy.value = false
        _state.value = AuthState.SignedOut("Не удалось подготовить вход VK (${error.javaClass.simpleName}). Проверьте ID Standalone-приложения.")
    }

    fun signIn() {
        if (!isConfigured || _busy.value) return
        val authLauncher = launcher
        if (authLauncher == null) {
            _state.value = AuthState.SignedOut("Экран входа ещё запускается. Попробуйте ещё раз.")
            return
        }
        _busy.value = true
        try {
            authLauncher.launch(setOf(
                VKScope.MESSAGES, VKScope.FRIENDS, VKScope.PHOTOS, VKScope.DOCS,
                VKScope.STATUS, VKScope.WALL, VKScope.GROUPS, VKScope.VIDEO,
                VKScope.AUDIO, VKScope.STORIES, VKScope.PAGES, VKScope.NOTES,
                VKScope.NOTIFICATIONS,
            ))
        } catch (e: Exception) {
            _busy.value = false
            _state.value = AuthState.SignedOut("Не удалось открыть вход VK (${e.javaClass.simpleName}). Проверьте настройки приложения и подпись.")
        }
    }

    fun onLegacyAuthResult(result: VKAuthenticationResult) {
        _busy.value = false
        when (result) {
            is VKAuthenticationResult.Success -> try {
                val token = result.token.accessToken
                store.save(token)
                cached = token
                _state.value = AuthState.SignedIn
            } catch (_: Exception) {
                cached = null
                _state.value = AuthState.SignedOut("VK выполнил вход, но Kontakt не смог безопасно сохранить сессию. Повторите попытку.")
            }
            is VKAuthenticationResult.Failed -> {
                _state.value = AuthState.SignedOut("VK не завершил авторизацию: ${result.exception.message ?: "вход отменён или отклонён"}.")
            }
        }
    }

    fun onSessionExpired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }
    fun signOut() { wipe(null) }
    private fun wipe(message: String?) {
        runCatching { VK.logout() }
        store.clear()
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }
}
