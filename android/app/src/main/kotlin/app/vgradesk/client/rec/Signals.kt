package app.vgradesk.client.rec

import app.vgradesk.client.data.FeedPost
import kotlin.math.min

/**
 * Сигналы, которые приложение реально получает. Лайки/комментарии/репосты/подписки в VK сюда не входят:
 * для их чтения и записи нужны права VK, которых у приложения может не быть.
 */
enum class SignalKind(val value: Double) {
    /** Пользователь задержался на публикации (значение зависит от секунд). */
    DWELL(0.0),

    /** Публикацию пролистнули за долю секунды. */
    QUICK_SKIP(-0.25),

    /** Кнопка «Интересно» (хранится только в приложении). */
    INTERESTED(3.0),

    /** «Интересно» нажали повторно и сняли. */
    INTEREST_REMOVED(-2.5),

    /** Кнопка «Не интересно». */
    NOT_INTERESTED(-4.0),
}

data class Signal(val kind: SignalKind, val post: FeedPost, val seconds: Double = 0.0)

object SignalClassifier {
    private const val QUICK_SKIP_MS = 1_200L
    private const val DWELL_MS = 3_000L

    /** Менее 1,2 с на экране — пролистнули; 1,2–3 с — нейтрально; дольше — интерес. */
    fun fromDwell(millis: Long): SignalKind? = when {
        millis < QUICK_SKIP_MS -> SignalKind.QUICK_SKIP
        millis < DWELL_MS -> null
        else -> SignalKind.DWELL
    }

    fun valueOf(signal: Signal): Double = when (signal.kind) {
        SignalKind.DWELL -> min(signal.seconds, 15.0) / 15.0 * 1.6 + 0.2
        else -> signal.kind.value
    }
}

/**
 * Считает, сколько каждая карточка пробыла на экране. Пока экран не активен (свернули приложение,
 * ушли на другую вкладку), время не идёт.
 */
class DwellTracker(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val onDwell: (key: String, millis: Long) -> Unit,
) {
    private var current: Set<String> = emptySet()
    private val started = HashMap<String, Long>()
    private var active = false

    fun onVisible(keys: Set<String>) {
        val now = clock()
        if (active) {
            for (key in started.keys.toList()) {
                if (key !in keys) {
                    val since = started.remove(key) ?: continue
                    onDwell(key, now - since)
                }
            }
            for (key in keys) if (key !in started) started[key] = now
        }
        current = keys
    }

    fun resume() {
        if (active) return
        active = true
        val now = clock()
        for (key in current) started[key] = now
    }

    fun pause() {
        if (!active) return
        val now = clock()
        for ((key, since) in started.toList()) onDwell(key, now - since)
        started.clear()
        active = false
    }
}
