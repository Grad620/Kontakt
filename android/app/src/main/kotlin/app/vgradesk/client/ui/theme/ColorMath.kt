package app.vgradesk.client.ui.theme

import app.vgradesk.client.core.AccentId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Чистая математика цвета (RGB в виде 0xRRGGBB): контраст, смешивание, подбор читаемого цвета текста. */
object ColorMath {
    const val WHITE = 0xFFFFFF
    const val INK = 0x101114

    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    fun luminance(rgb: Int): Double {
        fun lin(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(r(rgb)) + 0.7152 * lin(g(rgb)) + 0.0722 * lin(b(rgb))
    }

    /** Контраст по WCAG: от 1 до 21. Для текста нужно не меньше 4,5, для значков — не меньше 3. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** t = 0 — цвет [from], t = 1 — цвет [to]. */
    fun mix(from: Int, to: Int, t: Double): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt().coerceIn(0, 255)
        return (ch(r(from), r(to)) shl 16) or (ch(g(from), g(to)) shl 8) or ch(b(from), b(to))
    }

    /** Цвет значков и текста поверх заливки [fill]: белый или почти чёрный — тот, что контрастнее. */
    fun onColor(fill: Int): Int = if (contrast(fill, WHITE) >= contrast(fill, INK)) WHITE else INK

    /**
     * Делает [color] достаточно контрастным к [background], смешивая его с чёрным ([towardsDark]) или белым.
     * Нужен, чтобы акцентный цвет оставался читаемым как цвет текста и значков.
     */
    fun ensureContrast(color: Int, background: Int, minRatio: Double, towardsDark: Boolean): Int {
        val target = if (towardsDark) 0x000000 else WHITE
        var t = 0.0
        var result = color
        while (contrast(result, background) < minRatio && t < 1.0) {
            t += 0.05
            result = mix(color, target, t)
        }
        return result
    }
}

/** Нейтральные цвета интерфейса (не зависят от акцента). */
object Neutrals {
    const val LIGHT_BACKGROUND = 0xF6F7F9
    const val DARK_BACKGROUND = 0x0B0D10
    fun background(dark: Boolean) = if (dark) DARK_BACKGROUND else LIGHT_BACKGROUND
}

/** Восемь акцентов Kontakt. Значения — из ТЗ; белый в светлой теме рисуется графитовым, иначе он невиден. */
object AccentPalette {
    private const val GRAPHITE = 0x2B2D33

    fun nominal(id: AccentId): Int = when (id) {
        AccentId.White -> 0xF5F5F7
        AccentId.Sky -> 0x45A9F5
        AccentId.Blue -> 0x356AE6
        AccentId.Green -> 0x39B86B
        AccentId.DarkGreen -> 0x16845B
        AccentId.Birch -> 0x83CFC0
        AccentId.Violet -> 0x7F55E3 // чуть темнее значения из ТЗ (#865CE8): белый текст на кнопке должен читаться (контраст 4,9)
        AccentId.Lilac -> 0xC39BEF
    }

    fun label(id: AccentId): String = when (id) {
        AccentId.White -> "Белый"
        AccentId.Sky -> "Голубой"
        AccentId.Blue -> "Синий"
        AccentId.Green -> "Зелёный"
        AccentId.DarkGreen -> "Тёмно-зелёный"
        AccentId.Birch -> "Берёзовый"
        AccentId.Violet -> "Фиолетовый"
        AccentId.Lilac -> "Сиреневый"
    }

    /** Цвет заливки (кнопки, выбранная вкладка) в данной теме. */
    fun fill(id: AccentId, dark: Boolean): Int =
        if (id == AccentId.White && !dark) GRAPHITE else nominal(id)

    fun onFill(id: AccentId, dark: Boolean): Int = ColorMath.onColor(fill(id, dark))

    /** Цвет акцентного текста и значков на фоне интерфейса: контраст не ниже 4,5. */
    fun text(id: AccentId, dark: Boolean): Int =
        ColorMath.ensureContrast(fill(id, dark), Neutrals.background(dark), 4.5, towardsDark = !dark)

    /** Бледная подложка акцента (выбранные чипы, мягкие выделения). */
    fun container(id: AccentId, dark: Boolean): Int =
        ColorMath.mix(Neutrals.background(dark), fill(id, dark), if (dark) 0.30 else 0.18)

    fun onContainer(id: AccentId, dark: Boolean): Int =
        ColorMath.ensureContrast(fill(id, dark), container(id, dark), 4.5, towardsDark = !dark)
}
