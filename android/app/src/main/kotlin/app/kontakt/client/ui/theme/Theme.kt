package app.kontakt.client.ui.theme

import android.graphics.Color as AndroidColor
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
import androidx.activity.compose.LocalActivity
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kontakt.client.core.AccentId
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.core.KontaktStyleId
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
    val VkClassic = KontaktStyle(
        id = "vk_classic",
        cardShape = RoundedCornerShape(8.dp),
        cardBordered = true,
        rowDividers = true,
        screenPadding = 12.dp,
        postGap = 4.dp,
    )

    fun from(id: KontaktStyleId): KontaktStyle = when (id) {
        KontaktStyleId.Kontakt -> Default
        KontaktStyleId.VkClassic -> VkClassic
    }
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
            background = Color(0xFF17212B), onBackground = Color(0xFFE6EDF3),
            surface = Color(0xFF17212B), onSurface = Color(0xFFE6EDF3),
            surfaceVariant = Color(0xFF202B36), onSurfaceVariant = Color(0xFF9EABB8),
            surfaceTint = Color.Transparent,
            outline = Color(0xFF536170), outlineVariant = Color(0xFF293744),
            error = Color(0xFFFF8A80), onError = Color(0xFF3B0A06),
            surfaceContainerLowest = Color(0xFF111A23), surfaceContainerLow = Color(0xFF202B36),
            surfaceContainer = Color(0xFF222F3B), surfaceContainerHigh = Color(0xFF263442),
            surfaceContainerHighest = Color(0xFF2D3B49),
        )
    } else {
        lightColorScheme(
            primary = fill, onPrimary = onFill, primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = fill, onSecondary = onFill, secondaryContainer = container, onSecondaryContainer = onContainer,
            tertiary = fill, onTertiary = onFill, tertiaryContainer = container, onTertiaryContainer = onContainer,
            background = Color(0xFFF0F2F5), onBackground = Color(0xFF17212B),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF17212B),
            surfaceVariant = Color(0xFFE7ECF1), onSurfaceVariant = Color(0xFF6B7785),
            surfaceTint = Color.Transparent,
            outline = Color(0xFFB8C2CC), outlineVariant = Color(0xFFE1E4E8),
            error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFE7ECF1), surfaceContainerHigh = Color(0xFFDDE4EB),
            surfaceContainerHighest = Color(0xFFD3DCE5),
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
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun VgTheme(
    mode: ThemeMode,
    accent: AccentId,
    effects: EffectsMode,
    style: KontaktStyleId = KontaktStyleId.Kontakt,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // Иконки статус-бара и панели навигации подстраиваются под выбранную тему.
    val activity = LocalActivity.current as? ComponentActivity
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
        LocalKontaktStyle provides KontaktStyles.from(style),
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
