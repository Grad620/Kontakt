package app.kontakt.client.auth

/** Тип неудачи входа через VK ID SDK. Не зависит от самого SDK, чтобы логику можно было проверить юнит-тестами. */
enum class AuthFailureKind {
    /** Пользователь закрыл окно входа или вернулся в приложение без результата. */
    CANCELED,

    /** Запрос к серверу VK ID (обмен кода на токен) завершился ошибкой. */
    API_CALL,

    /** VK вернул ошибку авторизации. */
    OAUTH,

    /** Значение `state` в ответе не совпало с отправленным. */
    OAUTH_STATE,

    /** Не удалось вернуться из VK в приложение (redirect). */
    REDIRECT,

    /** На телефоне нет подходящего браузера для окна входа. */
    NO_BROWSER,

    OTHER,
}

/** Разобранный неудачный результат входа. */
data class AuthFailureInfo(
    val kind: AuthFailureKind,
    /** Текст для экрана входа. Не содержит токенов, кодов и секретов. */
    val message: String,
) {
    /** Окно закрыто без результата. Это не доказанная ошибка настроек. */
    val cancelled: Boolean get() = kind == AuthFailureKind.CANCELED
}

/**
 * Превращает результат `VKIDAuthFail` в понятный текст. Описание от SDK проходит через [sanitizeDiagnostic]:
 * оно может содержать адрес со значениями `code`, `state`, `device_id` или токен, а они не должны попасть
 * ни на экран, ни в логи.
 */
fun describeAuthFailure(kind: AuthFailureKind, description: String?): AuthFailureInfo {
    val detail = sanitizeDiagnostic(description)
    val suffix = if (detail.isEmpty()) "" else ": $detail"
    val message = when (kind) {
        AuthFailureKind.CANCELED ->
            "Вход не завершён: окно VK закрыто без результата. Если VK показал ошибку, перепишите её текст."
        AuthFailureKind.API_CALL ->
            "VK не принял запрос на вход (обмен кода на токен)$suffix"
        AuthFailureKind.OAUTH ->
            "VK вернул ошибку авторизации$suffix"
        AuthFailureKind.OAUTH_STATE ->
            "Ответ VK не прошёл проверку безопасности. Повторите вход."
        AuthFailureKind.REDIRECT ->
            "Не удалось вернуться из VK в Kontakt$suffix. Проверьте, что в сборке указан верный ID приложения."
        AuthFailureKind.NO_BROWSER ->
            "На телефоне не найден браузер для окна входа VK. Включите или установите Chrome и повторите."
        AuthFailureKind.OTHER ->
            "VK не завершил вход$suffix"
    }
    return AuthFailureInfo(kind, message)
}

/**
 * Подготавливает техническую строку к показу: убирает значения чувствительных параметров
 * (`access_token`, `code`, `code_verifier`, `state`, `device_id` и т. п.), прячет длинные токеноподобные
 * последовательности, схлопывает пробелы и обрезает до [maxLength].
 */
fun sanitizeDiagnostic(raw: String?, maxLength: Int = DEFAULT_DIAGNOSTIC_LENGTH): String {
    if (raw.isNullOrBlank()) return ""
    var text = raw
    text = SENSITIVE_PAIR.replace(text) { match -> "${match.groupValues[1]}=<скрыто>" }
    text = JSON_SENSITIVE_PAIR.replace(text) { match -> "\"${match.groupValues[1]}\":\"<скрыто>\"" }
    text = LONG_TOKEN.replace(text, "<скрыто>")
    text = WHITESPACE.replace(text, " ").trim()
    return if (text.length <= maxLength) text else text.take(maxLength) + "…"
}

private const val DEFAULT_DIAGNOSTIC_LENGTH = 160

private const val SENSITIVE_KEYS =
    "access_token|refresh_token|id_token|token|code|code_verifier|code_challenge|state|device_id|" +
        "client_secret|secret|password|silent_token|uuid"

/** `key=value` и `key: value` в адресах и текстах; значение до `&`, пробела, кавычки или конца строки. */
private val SENSITIVE_PAIR = Regex("(?i)\\b($SENSITIVE_KEYS)\\s*[=:]\\s*[^&\\s\"',;]+")

/** `"key":"value"` в тексте ответа сервера. */
private val JSON_SENSITIVE_PAIR = Regex("(?i)\"($SENSITIVE_KEYS)\"\\s*:\\s*\"[^\"]*\"")

/** Последовательность из 24 и более «токеновых» символов подряд. */
private val LONG_TOKEN = Regex("[A-Za-z0-9_\\-.~+/]{24,}={0,2}")

private val WHITESPACE = Regex("\\s+")
