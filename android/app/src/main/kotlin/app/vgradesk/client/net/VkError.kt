package app.vgradesk.client.net

/** Ошибки VK API и сети. Тексты показываются пользователю через [app.vgradesk.client.core.toUiError]. */
sealed class VkError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : VkError("Нет соединения с сетью", cause)
    class SessionExpired : VkError("Сессия истекла")
    class AccessDenied(val code: Int, val vkMessage: String) : VkError(vkMessage)
    class RateLimited : VkError("Слишком много запросов")
    class Captcha : VkError("VK запросил проверку (капчу)")
    class Server(val code: Int, val vkMessage: String) : VkError(vkMessage)
    class Malformed(cause: Throwable?) : VkError("Не удалось разобрать ответ VK", cause)
}
