package app.kontakt.client.auth

import androidx.activity.ComponentActivity
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

/** Classic VK Standalone SDK authentication; tokens are stored encrypted by TokenStore. */
class AuthManager(private val store: TokenStore) {
    val isConfigured: Boolean = BuildConfig.VK_STANDALONE_CONFIGURED
    @Volatile private var cached: String? = store.load()
    @Volatile private var activity: ComponentActivity? = null
    private var loginLauncher: ActivityResultLauncher<ArrayList<VKScope>>? = null
    private val _state = MutableStateFlow<AuthState>(
        if (cached != null) AuthState.SignedIn else AuthState.SignedOut()
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached

    /**
     * Register the Activity Result launcher while the Activity is being created.
     * Do not call VK.login() from signIn(): it registers a launcher and must run
     * before the Activity reaches STARTED.
     */
    fun attachLifecycleOwner(owner: ComponentActivity) {
        activity = owner
        loginLauncher = VK.login(owner) { result ->
            _busy.value = false
            when (result) {
                is VKAuthenticationResult.Success -> {
                    val token = result.token.accessToken
                    runCatching {
                        store.save(token)
                        cached = token
                        _state.value = AuthState.SignedIn
                    }.onFailure {
                        cached = null
                        _state.value = AuthState.SignedOut(
                            "Вход выполнен, но Kontakt не смог безопасно сохранить сессию. Повторите попытку."
                        )
                    }
                }
                is VKAuthenticationResult.Failed -> {
                    _state.value = AuthState.SignedOut(
                        "VK не завершил авторизацию: ${result.exception.message ?: result.exception.javaClass.simpleName}"
                            .take(200)
                    )
                }
            }
        }
    }

    fun detachLifecycleOwner(owner: ComponentActivity) {
        if (activity === owner) {
            activity = null
            loginLauncher = null
        }
    }

    fun signIn() {
        if (!isConfigured || _busy.value) {
            if (!isConfigured) {
                _state.value = AuthState.SignedOut(
                    "Приложение VK Standalone не настроено. Добавьте числовой VK_STANDALONE_APP_ID в GitHub Actions."
                )
            }
            return
        }
        val owner = activity
        val launcher = loginLauncher
        if (owner == null || launcher == null) {
            _state.value = AuthState.SignedOut("Экран входа ещё запускается. Попробуйте ещё раз.")
            return
        }
        val scopes = BuildConfig.VK_SCOPES.split(',').map(String::trim).filter(String::isNotEmpty)
        val requestedScopes = scopes.mapNotNull { scope ->
            when (scope.lowercase()) {
                "friends" -> VKScope.FRIENDS
                "wall" -> VKScope.WALL
                "photos" -> VKScope.PHOTOS
                "audio" -> VKScope.AUDIO
                "messages" -> VKScope.MESSAGES
                "groups" -> VKScope.GROUPS
                "docs" -> VKScope.DOCS
                "video" -> VKScope.VIDEO
                "status" -> VKScope.STATUS
                "stories" -> VKScope.STORIES
                "pages" -> VKScope.PAGES
                "notes" -> VKScope.NOTES
                "notifications" -> VKScope.NOTIFICATIONS
                else -> null
            }
        }
        _busy.value = true
        try {
            launcher.launch(ArrayList(requestedScopes))
        } catch (e: Exception) {
            _busy.value = false
            _state.value = AuthState.SignedOut(
                "Не удалось запустить авторизацию VK (${e.javaClass.simpleName}). Проверьте ID приложения, пакет и SHA-1."
            )
        }
    }

    fun onSessionExpired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }

    fun signOut() = wipe(null)

    private fun wipe(message: String?) {
        store.clear()
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }
}
