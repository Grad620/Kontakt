package app.kontakt.client.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Фабрика ViewModel без внешних библиотек DI. */
inline fun <reified VM : ViewModel> vmFactory(crossinline builder: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
    }

private val RU = Locale.forLanguageTag("ru")
private val TIME = DateTimeFormatter.ofPattern("HH:mm", RU)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", RU)
private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yy", RU)
private val POST_DATE = DateTimeFormatter.ofPattern("d MMM, HH:mm", RU)

fun formatChatTime(epochSeconds: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
    if (epochSeconds <= 0L) return ""
    val moment = Instant.ofEpochSecond(epochSeconds).atZone(now.zone)
    return when {
        moment.toLocalDate() == now.toLocalDate() -> moment.format(TIME)
        moment.year == now.year -> moment.format(DAY_MONTH)
        else -> moment.format(FULL_DATE)
    }
}

fun formatPostDate(epochSeconds: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
    if (epochSeconds <= 0L) return ""
    return Instant.ofEpochSecond(epochSeconds).atZone(now.zone).format(POST_DATE)
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss", RU)

/** Время для журнала диагностики (по часовому поясу телефона). */
fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).format(CLOCK)
