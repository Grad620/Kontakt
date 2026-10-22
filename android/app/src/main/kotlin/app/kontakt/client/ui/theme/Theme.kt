package app.kontakt.client.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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

/** Цвета, которых нет в Material 3: акцентный текст, «стекло» панелей и детали стиля VK Classic. */
@Immutable
data class KontaktColors(
    /** Акцент как цвет текста и значков на фоне экрана и карточек (контраст не ниже 4,5). */
    val accentText: Color,
    /** Заливка стеклянной панели (полупрозрачная). */
    val glass: Color,
    /** Верхний блик на кромке панели. */
    val glassHighlight: Color,
    /** Нижняя, едва заметная кромка панели. */
    val glassEdge: Color,
    val glassShadow: Color,
    /** Шапка VK Classic: заливка (светлая тема — акцент, тёмная — цвет карточки) и цвет текста на ней. */
    val classicHeader: Color,
    val onClassicHeader: Color,
    /** Бледная подложка акцента: непрочитанные диалоги в VK Classic. */
    val accentSoft: Color,
)

/**
 * Токены стиля: форма, плотность и устройство навигации. Все компоненты берут их отсюда, поэтому
 * стиль добавляется набором токенов, а экраны не переписываются.
 *
 * Kontakt — в духе Telegram: мягкие карточки, нижняя панель вкладок, крупные аватары.
 * VK Classic — мобильный VK до обновления 28.08.2026: цветная шапка, меню слева, плоские белые блоки
 * на сером фоне, компактные строки.
 */
@Immutable
data class KontaktStyle(
    val id: String,
    /** true — шапка с кнопкой меню и боковая шторка вместо нижней панели вкладок. */
    val classic: Boolean,
    val cardShape: Shape,
    val cardBordered: Boolean,
    val rowDividers: Boolean,
    /** Горизонтальный отступ карточек ленты от краёв экрана (в VK Classic блоки на всю ширину). */
    val screenPadding: Dp,
    /** Вертикальный зазор между карточками ленты. */
    val postGap: Dp,
    /** Внутренний отступ блока настроек и профиля. */
    val sectionPadding: Dp,
    val chatRowHeight: Dp,
    val chatAvatar: Dp,
    /** Подсвечивать строки непрочитанных диалогов. */
    val unreadTint: Boolean,
)

object KontaktStyles {
    val Default = KontaktStyle(
        id = "kontakt",
        classic = false,
        cardShape = RoundedCornerShape(14.dp),
        cardBordered = false,
        rowDividers = false,
        screenPadding = 12.dp,
        postGap = 6.dp,
        sectionPadding = 14.dp,
        chatRowHeight = 76.dp,
        chatAvatar = 52.dp,
        unreadTint = false,
    )
    val VkClassic = KontaktStyle(
        id = "vk_classic",
        classic = true,
        cardShape = RectangleShape,
        cardBordered = false,
        rowDividers = true,
        screenPadding = 0.dp,
        postGap = 8.dp,
        sectionPadding = 12.dp,
        chatRowHeight = 68.dp,
        chatAvatar = 48.dp,
        unreadTint = true,
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
    classicHeader = Color(0xFF4A76A8),
    onClassicHeader = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFDDE6F8),
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

private fun colorScheme(accent: AccentId, dark: Boolean, style: KontaktStyleId): ColorScheme {
    val n = Neutrals.of(style, dark)
    val fill = rgb(AccentPalette.fill(accent, dark, style))
    val onFill = rgb(AccentPalette.onFill(accent, dark, style))
    val container = rgb(AccentPalette.container(accent, dark, style))
    val onContainer = rgb(AccentPalette.onContainer(accent, dark, style))
    return if (dark) {
        darkColorScheme(
            primary = fill, onPrimary = onFill, primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = fill, onSecondary = onFill, secondaryContainer = container, onSecondaryContainer = onContainer,
            tertiary = fill, onTertiary = onFill, tertiaryContainer = container, onTertiaryContainer = onContainer,
            background = rgb(n.background), onBackground = rgb(n.onSurface),
            surface = rgb(n.card), onSurface = rgb(n.onSurface),
            surfaceVariant = rgb(n.field), onSurfaceVariant = rgb(n.onSurfaceVariant),
            surfaceTint = Color.Transparent,
            outline = rgb(n.outline), outlineVariant = rgb(n.outlineVariant),
            error = Color(0xFFFF8A80), onError = Color(0xFF3B0A06),
            surfaceContainerLowest = rgb(n.surfaceLowest), surfaceContainerLow = rgb(n.card),
            surfaceContainer = rgb(n.container), surfaceContainerHigh = rgb(n.field),
            surfaceContainerHighest = rgb(n.fieldStrong),
        )
    } else {
        lightColorScheme(
            primary = fill, onPrimary = onFill, primaryContainer = container, onPrimaryContainer = onContainer,
            secondary = fill, onSecondary = onFill, secondaryContainer = container, onSecondaryContainer = onContainer,
            tertiary = fill, onTertiary = onFill, tertiaryContainer = container, onTertiaryContainer = onContainer,
            background = rgb(n.background), onBackground = rgb(n.onSurface),
            surface = rgb(n.card), onSurface = rgb(n.onSurface),
            surfaceVariant = rgb(n.field), onSurfaceVariant = rgb(n.onSurfaceVariant),
            surfaceTint = Color.Transparent,
            outline = rgb(n.outline), outlineVariant = rgb(n.outlineVariant),
            error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
            surfaceContainerLowest = rgb(n.surfaceLowest), surfaceContainerLow = rgb(n.card),
            surfaceContainer = rgb(n.container), surfaceContainerHigh = rgb(n.field),
            surfaceContainerHighest = rgb(n.fieldStrong),
        )
    }
}

private fun kontaktColors(accent: AccentId, dark: Boolean, style: KontaktStyleId): KontaktColors =
    KontaktColors(
        accentText = rgb(AccentPalette.text(accent, dark, style)),
        glass = if (dark) Color(0xCC1B1F25) else Color(0xCCFFFFFF),
        glassHighlight = if (dark) Color(0x38FFFFFF) else Color(0xB3FFFFFF),
        glassEdge = if (dark) Color(0x10FFFFFF) else Color(0x14000000),
        glassShadow = if (dark) Color(0x99000000) else Color(0x40000000),
        classicHeader = rgb(AccentPalette.classicHeader(accent, dark)),
        onClassicHeader = rgb(AccentPalette.onClassicHeader(accent, dark)),
        accentSoft = rgb(AccentPalette.container(accent, dark, style)),
    )

/** Kontakt: чистая гротескная типографика; подписи разделов — моноширинные, как на техничных экранах. */
private val KontaktTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/** VK Classic: плотнее и мельче, как мобильная версия VK. */
private val ClassicTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal),
)

private val KontaktShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private val ClassicShapes = Shapes(
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(5.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
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

    // Строка состояния: в VK Classic (светлая тема) под ней цветная шапка. Светлые значки нужны,
    // когда шапка тёмная; в остальных случаях значки подстраиваются под тему.
    val classicHeaderIsDark = style == KontaktStyleId.VkClassic && !dark &&
        AccentPalette.onClassicHeader(accent, false) == ColorMath.WHITE
    val statusIconsLight = dark || classicHeaderIsDark

    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, dark, statusIconsLight) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { statusIconsLight },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
        onDispose { }
    }

    val scheme = remember(accent, dark, style) { colorScheme(accent, dark, style) }
    val extra = remember(accent, dark, style) { kontaktColors(accent, dark, style) }
    val classic = style == KontaktStyleId.VkClassic

    CompositionLocalProvider(
        LocalKontaktColors provides extra,
        LocalKontaktStyle provides KontaktStyles.from(style),
        LocalEffects provides effects,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (classic) ClassicTypography else KontaktTypography,
            shapes = if (classic) ClassicShapes else KontaktShapes,
            content = content,
        )
    }
}
