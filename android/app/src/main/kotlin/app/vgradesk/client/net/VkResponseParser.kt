package app.vgradesk.client.net

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Чистая (без Android) разборка ответов VK API: удобно проверять юнит-тестами. */
object VkResponseParser {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Возвращает содержимое поля `response` или бросает [VkError]. */
    fun parse(text: String): JsonElement {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw VkError.Malformed(e)
        }
        val error = root["error"]
        if (error is JsonObject) throw mapError(error)
        return root["response"] ?: throw VkError.Malformed(null)
    }

    fun mapError(error: JsonObject): VkError {
        val code = error["error_code"]?.jsonPrimitive?.intOrNull ?: -1
        val message = error["error_msg"]?.jsonPrimitive?.contentOrNull ?: "Ошибка VK API"
        return when (code) {
            5 -> VkError.SessionExpired()
            6, 9 -> VkError.RateLimited()
            14 -> VkError.Captcha()
            7, 15, 27, 28, 200, 201, 203, 204 -> VkError.AccessDenied(code, message)
            else -> VkError.Server(code, message)
        }
    }

    inline fun <reified T> decode(element: JsonElement): T =
        try {
            json.decodeFromJsonElement<T>(element)
        } catch (e: SerializationException) {
            throw VkError.Malformed(e)
        } catch (e: IllegalArgumentException) {
            throw VkError.Malformed(e)
        }
}
