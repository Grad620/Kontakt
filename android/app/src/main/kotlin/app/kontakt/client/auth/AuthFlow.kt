package app.kontakt.client.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    data object SignedIn : AuthState
    data class SignedOut(val message: String? = null) : AuthState
}

/** Куда [AuthFlow] сохраняет сессию. В приложении это зашифрованное хранилище `TokenStore`. */
interface SessionSink {
    fun save(token: String)
    fun clear()
}

/**
 * Состояние входа без зависимостей от Android и VK SDK.
 *
 * Правила, которые проверяются юнит-тестами:
 * - одновременно идёт не больше одной попытки входа ([begin] возвращает false, пока предыдущая не закончилась);
 * - каждая попытка заканчивается одним из [succeeded], [failed], [reject] или [abandoned], и после любого из них
 *   `busy` снова false — экран не остаётся в вечной загрузке;
 * - токен считается принятым только после успешного сохранения; при ошибке сохранения сессии нет;
 * - токен и его значение не попадают ни в состояние, ни в сообщения.
 */
class AuthFlow(
    private val sink: SessionSink,
    initialToken: String?,
) {
    @Volatile private var cached: String? = initialToken
    private val _state = MutableStateFlow<AuthState>(
        if (initialToken != null) AuthState.SignedIn else AuthState.SignedOut()
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun token(): String? = cached

    /** Начинает попытку. Возвращает false, если предыдущая ещё не закончилась. */
    fun begin(): Boolean {
        if (!_busy.compareAndSet(expect = false, update = true)) return false
        // Сообщение прошлой ошибки больше не актуально.
        if (_state.value is AuthState.SignedOut) _state.value = AuthState.SignedOut()
        return true
    }

    /** Попытка не дошла до VK (не настроено, SDK не запустился и т. п.). */
    fun reject(message: String) {
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }

    fun succeeded(token: String) {
        _busy.value = false
        try {
            require(token.isNotBlank()) { "Пустой токен" }
            sink.save(token)
            cached = token
            _state.value = AuthState.SignedIn
        } catch (_: Exception) {
            cached = null
            _state.value = AuthState.SignedOut(SAVE_FAILED_MESSAGE)
        }
    }

    fun failed(info: AuthFailureInfo) {
        _busy.value = false
        _state.value = AuthState.SignedOut(info.message)
    }

    /** Результат не пришёл за отведённое время: снимаем загрузку, чтобы можно было повторить. */
    fun abandoned() {
        if (!_busy.value) return
        _busy.value = false
        _state.value = AuthState.SignedOut(ABANDONED_MESSAGE)
    }

    fun expired() {
        if (_state.value is AuthState.SignedOut) return
        wipe("Сессия истекла. Войдите снова.")
    }

    fun signOut() = wipe(null)

    private fun wipe(message: String?) {
        runCatching { sink.clear() }
        cached = null
        _busy.value = false
        _state.value = AuthState.SignedOut(message)
    }

    companion object {
        const val SAVE_FAILED_MESSAGE =
            "VK выполнил вход, но Kontakt не смог безопасно сохранить сессию. Повторите попытку."
        const val ABANDONED_MESSAGE =
            "VK не вернул результат входа. Нажмите «Войти через VK», чтобы повторить."
    }
}
