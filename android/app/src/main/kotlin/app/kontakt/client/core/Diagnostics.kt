package app.kontakt.client.core

import app.kontakt.client.net.VkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Одна запись журнала. Содержит только название метода и вид ошибки: ни токенов, ни текста сообщений,
 * ни содержимого ответов VK. Хранится в памяти и исчезает при закрытии приложения.
 */
data class DiagEvent(val timeMs: Long, val method: String, val kind: String, val code: Int? = null)

class DiagnosticsLog(
    private val capacity: Int = 50,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val _events = MutableStateFlow<List<DiagEvent>>(emptyList())
    val events: StateFlow<List<DiagEvent>> = _events.asStateFlow()

    fun record(method: String, error: Throwable) {
        val (kind, code) = describe(error)
        val event = DiagEvent(clock(), method, kind, code)
        _events.update { (listOf(event) + it).take(capacity) }
    }

    fun clear() {
        _events.value = emptyList()
    }

    companion object {
        fun describe(error: Throwable): Pair<String, Int?> = when (error) {
            is VkError.Network -> "нет соединения или тайм-аут" to null
            is VkError.SessionExpired -> "сессия истекла" to 5
            is VkError.AccessDenied -> "нет доступа к методу" to error.code
            is VkError.RateLimited -> "лимит запросов VK" to 6
            is VkError.Captcha -> "VK запросил капчу" to 14
            is VkError.Server -> "ошибка VK" to error.code
            is VkError.Malformed -> "не удалось разобрать ответ" to null
            else -> "внутренняя ошибка" to null
        }
    }
}
