package app.kontakt.client.data

import app.kontakt.client.net.VkApi
import app.kontakt.client.net.VkError

data class ApiCapabilityResult(
    val title: String,
    val method: String,
    val available: Boolean,
    val detail: String,
)

/**
 * Проверяет реальные ответы VK API текущего пользователя.
 * Не отправляет сообщения, не изменяет данные и никогда не читает/выводит токен.
 */
class ApiCapabilityProbe(private val api: VkApi) {
    suspend fun run(): List<ApiCapabilityResult> = listOf(
        probe("Профиль", "users.get"),
        probe("Группы пользователя", "groups.get", mapOf("count" to "1", "extended" to "1")),
        probe("Диалоги", "messages.getConversations", mapOf("count" to "1", "extended" to "1")),
        probe("Лента подписок", "newsfeed.get", mapOf("filters" to "post", "count" to "1")),
        probe("Поиск публикаций", "newsfeed.search", mapOf("q" to "новости", "count" to "1")),
        probe("Музыка VK", "audio.get", mapOf("count" to "1")),
    )

    private suspend fun probe(
        title: String,
        method: String,
        params: Map<String, String> = emptyMap(),
    ): ApiCapabilityResult = try {
        api.call(method, params, retries = 0)
        ApiCapabilityResult(title, method, true, "VK вернул успешный ответ")
    } catch (error: VkError) {
        val detail = when (error) {
            is VkError.ProfileTypeUnavailable -> "Код 1051: этот тип токена не поддерживает метод"
            is VkError.AccessDenied -> "Код ${error.code}: ${error.vkMessage}"
            is VkError.Server -> "Код ${error.code}: ${error.vkMessage}"
            is VkError.SessionExpired -> "Сессия истекла — войдите снова"
            is VkError.RateLimited -> "VK ограничил частоту запросов"
            is VkError.Captcha -> "VK требует проверку CAPTCHA"
            is VkError.Network -> "Сетевая ошибка"
            is VkError.Malformed -> "VK вернул ответ неожиданного формата"
        }
        ApiCapabilityResult(title, method, false, detail)
    } catch (error: Exception) {
        ApiCapabilityResult(title, method, false, "Ошибка клиента: ${error.javaClass.simpleName}")
    }
}
