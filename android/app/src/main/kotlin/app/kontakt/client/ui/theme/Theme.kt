package app.kontakt.client.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kontakt.client.core.AccentId
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.core.ThemeMode

/** Отступы Kontakt: все значения кратны 4 dp. */
object VgSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Цвета, которых нет в Material 3: акцентный текст и «стекло» плавающих панелей. */
@Immutable
data class KontaktColors(
    /** Акцент как цвет текста и значков на фоне экрана (контраст не ниже 4,5). */
    val accentText: Color,
    /** Заливка стеклянной панели (полупрозрачная). */
    val glass: Color,
    /** Верхний блик на кромке панели. */
    val glassHighlight: Color,
    /** Нижняя, едва заметная кромка панели. */
    val glassEdge: Color,
    val glassShadow: Color,
)

/**
 * Токены стиля: форма и плотность компонентов. Все компоненты берут их отсюда, поэтому альтернативный
 * стиль (например, VK Classic — только по просьбе пользователя) добавляется новым набором токенов,
 * а не переписыванием экранов. Сейчас существует один стиль.
 */
@Immutable
data class KontaktStyle(
    val id: String,
    val cardShape: Shape,
    val cardBordered: Boolean,
    val rowDividers: Boolean,
    val screenPadding: Dp,
    val postGap: Dp,
)

object KontaktStyles {
    val Default = KontaktStyle(
        id = "kontakt",
        cardShape = RoundedCornerShape(20.dp),
        cardBordered = false,
        rowDividers = false,
        screenPadding = 16.dp,
        postGap = 6.dp,
    )
}

private val DefaultColors = KontaktColors(
    accentText = Color(0xFF356AE6),
    glass = Color(0xCCFFFFFF),
    glassHighlight = Color(0x99FFFFFF),
    glassEdge = Color(0x14000000),
    glassShadow = Color(0x33000000),
)

val LocalKontaktColors = staticCompositionLocalOf { DefaultColors }
val LocalKontaktStyle = staticCompositionLocalOf { KontaktStyles.Default }
val LocalEffects = staticCompositionLocalOf { EffectsMode.Full }

/** Короткий доступ: `Kontakt.colors`, `Kontakt.style`, `Kontakt.effects`. */
object Kontakt {
    val colors: KontaktColors @Composable @ReadOnlyComposable get() = LocalKontaktColors.current
    val style: KontaktStyle @Composable @ReadOnlyComposable get() = LocalKontaktStyle.current
    val effects: EffectsMode @Composable @ReadOnlyComposable get() = LocalEffects.current
}

private fun rgb(value: Int): Color = Color(0xFF000000.toInt() or value)

private fun colorScheme(accent: AccentId, dark: Boolean): ColorScheme {
    val fill = rgb(AccentPalette.fill(accent, dark))
    val onFill = rgb(AccentPalette.onFill(accent, dark))
    val container = rgb(AccentPalette.container(accent, dark))
    val onContainer = rgb(AccentPalette.onContainer(accent, dark))
    return if (dark) {
        darkColorScheme(
            primary = fill, onPrimary = onFill, primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = fill, onSecondary = onFill, secondaryContainer = container, onSecondaryContainer = onContainer,
            tertiary = fill, onTertiary = onFill, tertiaryContainer = container, onTertiaryContainer = onContainer,
            background = Color(0xFF0B0D10), onBackground = Color(0xFFE6E9ED),
            surface = Color(0xFF0B0D10), onSurface = Color(0xFFE6E9ED),
            surfaceVariant = Color(0xFF21262C), onSurfaceVariant = Color(0xFFA6AEB9),
            surfaceTint = Color.Transparent,
            outline = Color(0xFF6F7782), outlineVariant = Color(0xFF2F353C),
            error = Color(0xFFFF8A80), onError = Color(0xFF3B0A06),
            surfaceContainerLowest = Color(0xFF07080A), surfaceContainerLow = Color(0xFF14171B),
            surfaceContainer = Color(0xFF191D22), surfaceContainerHigh = Color(0xFF21262C),
            surfaceContainerHighest = Color(0xFF2A3037),
        )
    } else {
        lightColorScheme(
            primary = fill, onPrimary = onFill, primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = fill, onSecondary = onFill, secondaryContainer = container, onSecondaryContainer = onContainer,
            tertiary = fill, onTertiary = onFill, tertiaryContainer = container, onTertiaryContainer = onContainer,
            background = Color(0xFFF6F7F9), onBackground = Color(0xFF15181C),
            surface = Color(0xFFF6F7F9), onSurface = Color(0xFF15181C),
            surfaceVariant = Color(0xFFE4E8ED), onSurfaceVariant = Color(0xFF5B6470),
            surfaceTint = Color.Transparent,
            outline = Color(0xFF8A93A0), outlineVariant = Color(0xFFD3D9E0),
            error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F3F6),
            surfaceContainer = Color(0xFFEBEEF2), surfaceContainerHigh = Color(0xFFE4E8ED),
            surfaceContainerHighest = Color(0xFFDDE2E8),
        )
    }
}

private fun kontaktColors(accent: AccentId, dark: Boolean): KontaktColors =
    if (dark) {
        KontaktColors(
            accentText = rgb(AccentPalette.text(accent, true)),
            glass = Color(0xCC1B1F25),
            glassHighlight = Color(0x38FFFFFF),
            glassEdge = Color(0x10FFFFFF),
            glassShadow = Color(0x99000000),
        )
    } else {
        KontaktColors(
            accentText = rgb(AccentPalette.text(accent, false)),
            glass = Color(0xCCFFFFFF),
            glassHighlight = Color(0xB3FFFFFF),
            glassEdge = Color(0x14000000),
            glassShadow = Color(0x40000000),
        )
    }

private val VgTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val VgShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VgTheme(
    mode: ThemeMode,
    accent: AccentId,
    effects: EffectsMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // Иконки статус-бара и панели навигации подстраиваются под выбранную тему.
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
        onDispose { }
    }

    val scheme = remember(accent, dark) { colorScheme(accent, dark) }
    val extra = remember(accent, dark) { kontaktColors(accent, dark) }

    CompositionLocalProvider(
        LocalKontaktColors provides extra,
        LocalKontaktStyle provides KontaktStyles.Default,
        LocalEffects provides effects,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = VgTypography,
            shapes = VgShapes,
            content = content,
        )
    }
}
