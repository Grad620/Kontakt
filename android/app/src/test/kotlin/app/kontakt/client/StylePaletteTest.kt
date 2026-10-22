package app.kontakt.client

import app.kontakt.client.core.AccentId
import app.kontakt.client.core.AppearanceState
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.core.NavConfig
import app.kontakt.client.core.KontaktStyleId
import app.kontakt.client.core.ThemeMode
import app.kontakt.client.ui.theme.AccentPalette
import app.kontakt.client.ui.theme.ColorMath
import app.kontakt.client.ui.theme.Neutrals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Палитры обоих стилей оформления: читаемость текста и различимость слоёв во всех 4 комбинациях стиль × тема. */
class StylePaletteTest {
    private val combos = KontaktStyleId.entries.flatMap { style -> listOf(false, true).map { dark -> style to dark } }

    @Test
    fun primaryTextIsHighlyReadableOnBackgroundAndCards() {
        for ((style, dark) in combos) {
            val n = Neutrals.of(style, dark)
            for ((name, surface) in listOf("фон" to n.background, "карточка" to n.card, "меню" to n.container)) {
                val ratio = ColorMath.contrast(n.onSurface, surface)
                assertTrue("основной текст на «$name» ($style, dark=$dark): контраст $ratio", ratio >= 7.0)
            }
        }
    }

    @Test
    fun secondaryTextIsReadableOnBackgroundAndCards() {
        for ((style, dark) in combos) {
            val n = Neutrals.of(style, dark)
            for ((name, surface) in listOf("фон" to n.background, "карточка" to n.card, "поле" to n.field)) {
                val ratio = ColorMath.contrast(n.onSurfaceVariant, surface)
                assertTrue("вторичный текст на «$name» ($style, dark=$dark): контраст $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun cardsAreDistinguishableFromBackground() {
        for ((style, dark) in combos) {
            val n = Neutrals.of(style, dark)
            val ratio = ColorMath.contrast(n.card, n.background)
            assertTrue("карточка сливается с фоном ($style, dark=$dark): контраст $ratio", ratio >= 1.05)
        }
    }

    @Test
    fun outlineIsVisibleAndVariantIsSubtler() {
        for ((style, dark) in combos) {
            val n = Neutrals.of(style, dark)
            val strong = ColorMath.contrast(n.outline, n.card)
            val soft = ColorMath.contrast(n.outlineVariant, n.card)
            assertTrue("обводка почти невидна ($style, dark=$dark): $strong", strong >= 1.4)
            assertTrue("тонкий разделитель заметнее обводки ($style, dark=$dark)", soft <= strong)
        }
    }

    @Test
    fun accentTextAndFillAreReadableInEveryStyleThemeAndAccent() {
        for ((style, dark) in combos) {
            val n = Neutrals.of(style, dark)
            for (id in AccentId.entries) {
                val fillRatio = ColorMath.contrast(AccentPalette.fill(id, dark, style), AccentPalette.onFill(id, dark, style))
                assertTrue("текст на акценте $id ($style, dark=$dark): $fillRatio", fillRatio >= 4.5)
                val text = AccentPalette.text(id, dark, style)
                for (surface in listOf(n.background, n.card)) {
                    val ratio = ColorMath.contrast(text, surface)
                    assertTrue("акцентный текст $id ($style, dark=$dark): $ratio", ratio >= 4.5)
                }
                val onContainer = ColorMath.contrast(
                    AccentPalette.onContainer(id, dark, style),
                    AccentPalette.container(id, dark, style),
                )
                assertTrue("текст на подложке акцента $id ($style, dark=$dark): $onContainer", onContainer >= 4.5)
            }
        }
    }

    @Test
    fun classicHeaderTextIsReadableForEveryAccentAndTheme() {
        for (dark in listOf(false, true)) {
            for (id in AccentId.entries) {
                val ratio = ColorMath.contrast(AccentPalette.classicHeader(id, dark), AccentPalette.onClassicHeader(id, dark))
                assertTrue("текст на шапке VK Classic $id (dark=$dark): $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun classicDarkHeaderIsTheCardColorAndLightHeaderIsTheAccent() {
        val dark = Neutrals.of(KontaktStyleId.VkClassic, true).card
        for (id in AccentId.entries) {
            assertEquals(dark, AccentPalette.classicHeader(id, dark = true))
            assertEquals(AccentPalette.fill(id, false, KontaktStyleId.VkClassic), AccentPalette.classicHeader(id, dark = false))
        }
    }

    @Test
    fun classicBlueIsTheClassicHeaderBlueAndKontaktBlueIsUntouched() {
        assertEquals(0x4A76A8, AccentPalette.fill(AccentId.Blue, dark = false, style = KontaktStyleId.VkClassic))
        assertEquals(AccentPalette.nominal(AccentId.Blue), AccentPalette.fill(AccentId.Blue, dark = false, style = KontaktStyleId.Kontakt))
        assertEquals(
            AccentPalette.nominal(AccentId.Green),
            AccentPalette.fill(AccentId.Green, dark = false, style = KontaktStyleId.VkClassic),
        )
    }

    @Test
    fun stylesDifferInBackgroundButKeepTheirOwnIdentity() {
        for (dark in listOf(false, true)) {
            assertNotEquals(
                Neutrals.of(KontaktStyleId.Kontakt, dark).background,
                Neutrals.of(KontaktStyleId.VkClassic, dark).background,
            )
        }
        assertEquals(Neutrals.of(KontaktStyleId.Kontakt, false).background, Neutrals.background(false))
    }

    @Test
    fun styleParsingFallsBackToKontaktOnUnknownOrMissingValue() {
        assertEquals(KontaktStyleId.VkClassic, KontaktStyleId.parse("VkClassic"))
        assertEquals(KontaktStyleId.Kontakt, KontaktStyleId.parse("Kontakt"))
        assertEquals(KontaktStyleId.Kontakt, KontaktStyleId.parse(null))
        assertEquals(KontaktStyleId.Kontakt, KontaktStyleId.parse(""))
        assertEquals(KontaktStyleId.Kontakt, KontaktStyleId.parse("vkclassic"))
        assertEquals(KontaktStyleId.Kontakt, KontaktStyleId.parse("Telegram"))
    }

    @Test
    fun appearanceStateDefaultsToKontaktStyle() {
        val state = AppearanceState(ThemeMode.System, AccentId.Blue, EffectsMode.Full, NavConfig.DEFAULT)
        assertEquals(KontaktStyleId.Kontakt, state.style)
        assertEquals(KontaktStyleId.VkClassic, state.copy(style = KontaktStyleId.VkClassic).style)
    }
}
