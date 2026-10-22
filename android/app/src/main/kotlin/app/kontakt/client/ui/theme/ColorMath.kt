package app.kontakt.client.ui.theme

import app.kontakt.client.core.AccentId
import app.kontakt.client.core.KontaktStyleId
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

/**
 * Нейтральные цвета одного стиля в одной теме (0xRRGGBB). Не зависят от акцента.
 * Роли совпадают с ролями Material 3, чтобы стандартные компоненты красились сами.
 */
data class NeutralSet(
    /** Фон экрана. */
    val background: Int,
    /** Самый глубокий слой (поля ввода на карточке, подложки). */
    val surfaceLowest: Int,
    /** Фон карточек, списков и нижней панели. */
    val card: Int,
    /** Меню, диалоги, нижние листы. */
    val container: Int,
    /** Чипы, поля поиска, кнопки второго плана, дорожки переключателей. */
    val field: Int,
    val fieldStrong: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    /** Обводки, на которые нужно смотреть (контраст с карточкой не меньше 1,4). */
    val outline: Int,
    /** Тонкие разделители. */
    val outlineVariant: Int,
)

/**
 * Нейтральные цвета интерфейса: по набору на стиль и тему. Читаемость текста и различимость слоёв
 * проверяются тестами (StylePaletteTest).
 */
object Neutrals {
    // Kontakt (в духе Telegram): светлые списки на светлом сером, тёмная тема — глубокий сине-графитовый.
    private val KONTAKT_LIGHT = NeutralSet(
        background = 0xF0F2F5, surfaceLowest = 0xFFFFFF, card = 0xFFFFFF, container = 0xFFFFFF,
        field = 0xE7ECF1, fieldStrong = 0xDDE4EB,
        onSurface = 0x17212B, onSurfaceVariant = 0x5F6B79, outline = 0xB8C2CC, outlineVariant = 0xE1E4E8,
    )
    private val KONTAKT_DARK = NeutralSet(
        background = 0x0E1621, surfaceLowest = 0x0A111A, card = 0x17212B, container = 0x1C2733,
        field = 0x202B36, fieldStrong = 0x2B3946,
        onSurface = 0xE6EDF3, onSurfaceVariant = 0x9EABB8, outline = 0x536170, outlineVariant = 0x293744,
    )

    // VK Classic: серый фон страницы и плоские белые блоки; в тёмной теме — графитовые.
    private val CLASSIC_LIGHT = NeutralSet(
        background = 0xEDEEF0, surfaceLowest = 0xFFFFFF, card = 0xFFFFFF, container = 0xFFFFFF,
        field = 0xE7E9ED, fieldStrong = 0xDADDE2,
        onSurface = 0x0A0A0B, onSurfaceVariant = 0x5F6A77, outline = 0xC2C7CE, outlineVariant = 0xE3E5E8,
    )
    private val CLASSIC_DARK = NeutralSet(
        background = 0x19191A, surfaceLowest = 0x111112, card = 0x222223, container = 0x2A2A2C,
        field = 0x303133, fieldStrong = 0x3B3C3F,
        onSurface = 0xE1E3E6, onSurfaceVariant = 0x9A9DA3, outline = 0x4A4B4E, outlineVariant = 0x363739,
    )

    fun of(style: KontaktStyleId, dark: Boolean): NeutralSet = when (style) {
        KontaktStyleId.Kontakt -> if (dark) KONTAKT_DARK else KONTAKT_LIGHT
        KontaktStyleId.VkClassic -> if (dark) CLASSIC_DARK else CLASSIC_LIGHT
    }

    fun background(dark: Boolean): Int = of(KontaktStyleId.Kontakt, dark).background
}

/** Три спокойных акцента Kontakt. Белый в светлой теме использует графитовую заливку для читаемых кнопок. */
object AccentPalette {
    private const val GRAPHITE = 0x2B2D33
    private const val CLASSIC_BLUE = 0x4A76A8

    fun nominal(id: AccentId): Int = when (id) {
        AccentId.White -> 0xF3F5F6
        AccentId.Sky -> 0x4A9FE3
        AccentId.Blue -> 0x356AE6
        AccentId.Green -> 0x399B70
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

    /**
     * Цвет заливки (кнопки, выбранная вкладка, шапка VK Classic) в данной теме.
     * В VK Classic «Синий» — классический синий цвет шапки VK (#4A76A8): белый текст на нём читается.
     */
    fun fill(id: AccentId, dark: Boolean, style: KontaktStyleId = KontaktStyleId.Kontakt): Int = when {
        style == KontaktStyleId.VkClassic && id == AccentId.Blue && !dark -> CLASSIC_BLUE
        id == AccentId.White && !dark -> GRAPHITE
        else -> nominal(id)
    }

    fun onFill(id: AccentId, dark: Boolean, style: KontaktStyleId = KontaktStyleId.Kontakt): Int =
        ColorMath.onColor(fill(id, dark, style))

    /** Цвет акцентного текста и значков на фоне и на карточках: контраст не ниже 4,5 с обоими. */
    fun text(id: AccentId, dark: Boolean, style: KontaktStyleId = KontaktStyleId.Kontakt): Int {
        val n = Neutrals.of(style, dark)
        val onBackground = ColorMath.ensureContrast(fill(id, dark, style), n.background, 4.5, towardsDark = !dark)
        return ColorMath.ensureContrast(onBackground, n.card, 4.5, towardsDark = !dark)
    }

    /** Бледная подложка акцента (выбранные чипы, мягкие выделения, непрочитанные чаты в VK Classic). */
    fun container(id: AccentId, dark: Boolean, style: KontaktStyleId = KontaktStyleId.Kontakt): Int =
        ColorMath.mix(Neutrals.of(style, dark).background, fill(id, dark, style), if (dark) 0.30 else 0.18)

    fun onContainer(id: AccentId, dark: Boolean, style: KontaktStyleId = KontaktStyleId.Kontakt): Int =
        ColorMath.ensureContrast(fill(id, dark, style), container(id, dark, style), 4.5, towardsDark = !dark)

    /**
     * Цвет шапки VK Classic. В светлой теме это заливка акцента (белый/чёрный текст по [onFill]),
     * в тёмной — цвет карточки, как в тёмной теме самого VK: акцент остаётся в значках и индикаторах.
     */
    fun classicHeader(id: AccentId, dark: Boolean): Int =
        if (dark) Neutrals.of(KontaktStyleId.VkClassic, true).card else fill(id, false, KontaktStyleId.VkClassic)

    /** Цвет текста и значков на шапке VK Classic. */
    fun onClassicHeader(id: AccentId, dark: Boolean): Int =
        if (dark) Neutrals.of(KontaktStyleId.VkClassic, true).onSurface else onFill(id, false, KontaktStyleId.VkClassic)
}
