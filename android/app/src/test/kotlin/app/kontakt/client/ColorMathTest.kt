package app.kontakt.client

import app.kontakt.client.core.AccentId
import app.kontakt.client.ui.theme.AccentPalette
import app.kontakt.client.ui.theme.ColorMath
import app.kontakt.client.ui.theme.Neutrals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test
    fun contrastOfBlackAndWhiteIs21() {
        assertEquals(21.0, ColorMath.contrast(0x000000, 0xFFFFFF), 0.01)
        assertEquals(1.0, ColorMath.contrast(0x777777, 0x777777), 1e-9)
    }

    @Test
    fun textOnAccentFillIsReadableForEveryAccentInBothThemes() {
        for (dark in listOf(false, true)) {
            for (id in AccentId.entries) {
                val ratio = ColorMath.contrast(AccentPalette.fill(id, dark), AccentPalette.onFill(id, dark))
                assertTrue("текст на акценте $id (dark=$dark): контраст $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun accentTextIsReadableOnBackgroundForEveryAccentInBothThemes() {
        for (dark in listOf(false, true)) {
            for (id in AccentId.entries) {
                val ratio = ColorMath.contrast(AccentPalette.text(id, dark), Neutrals.background(dark))
                assertTrue("акцентный текст $id (dark=$dark): контраст $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun softContainerTextIsReadable() {
        for (dark in listOf(false, true)) {
            for (id in AccentId.entries) {
                val ratio = ColorMath.contrast(AccentPalette.onContainer(id, dark), AccentPalette.container(id, dark))
                assertTrue("текст на подложке $id (dark=$dark): контраст $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun fillOfEveryAccentIsVisibleAgainstBackground() {
        for (dark in listOf(false, true)) {
            for (id in AccentId.entries) {
                val ratio = ColorMath.contrast(AccentPalette.fill(id, dark), Neutrals.background(dark))
                assertTrue("заливка $id (dark=$dark) сливается с фоном: контраст $ratio", ratio >= 1.5)
            }
        }
    }

    @Test
    fun whiteAccentTurnsGraphiteInLightThemeOnly() {
        assertNotEquals(AccentPalette.nominal(AccentId.White), AccentPalette.fill(AccentId.White, dark = false))
        assertEquals(AccentPalette.nominal(AccentId.White), AccentPalette.fill(AccentId.White, dark = true))
    }

    @Test
    fun skyAndBlueAreClearlyDifferent() {
        val ratio = ColorMath.contrast(AccentPalette.nominal(AccentId.Sky), AccentPalette.nominal(AccentId.Blue))
        assertTrue("голубой и синий должны различаться: $ratio", ratio >= 1.5)
    }

    @Test
    fun mixInterpolates() {
        assertEquals(0x808080, ColorMath.mix(0x000000, 0xFFFFFF, 0.5) and 0xFFFFFF and 0x808080)
        assertEquals(0x000000, ColorMath.mix(0x000000, 0xFFFFFF, 0.0))
        assertEquals(0xFFFFFF, ColorMath.mix(0x000000, 0xFFFFFF, 1.0))
    }
}
