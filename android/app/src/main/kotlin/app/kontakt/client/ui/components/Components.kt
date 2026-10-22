package app.kontakt.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.core.UiError
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.KontaktIcons
import app.kontakt.client.ui.theme.VgSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val AvatarColors = listOf(
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFEF6C00),
    Color(0xFF8E24AA), Color(0xFF3949AB), Color(0xFFD81B60),
)

fun initialsOf(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Круглый аватар: инициалы, а поверх — картинка (грузится уже нужного размера, кешируется Coil). */
@Composable
fun Avatar(url: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AvatarColors[(name.hashCode() and Int.MAX_VALUE) % AvatarColors.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(initialsOf(name), color = Color.White, style = MaterialTheme.typography.labelLarge)
        if (!url.isNullOrBlank()) {
            val request = remember(url, sizePx) {
                ImageRequest.Builder(context).data(url).size(sizePx).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Нижний отступ, который нужно оставить в списках, чтобы панель навигации не закрывала последние элементы. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/** Открыть экран настроек. null — на этом экране кнопки настроек нет. */
val LocalOpenSettings = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Открыть боковое меню (VK Classic). null — меню на этом экране недоступно. */
val LocalOpenDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * VK Classic: цвет шапки продолжается под строкой состояния на всех экранах, в том числе на входе,
 * поэтому светлые значки строки состояния всегда лежат на цветном фоне. В стиле Kontakt ничего не меняется.
 */
@Composable
fun StatusBarBackdrop(content: @Composable () -> Unit) {
    if (!Kontakt.style.classic) {
        content()
        return
    }
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Kontakt.colors.classicHeader),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.statusBars),
        ) { content() }
    }
}

/** Шапка VK Classic: заливка акцента (в тёмной теме — цвет карточки), значок меню или «назад» слева. */
@Composable
private fun ClassicBar(
    title: String,
    modifier: Modifier,
    navIcon: ImageVector?,
    navDescription: String,
    onNav: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = Kontakt.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.classicHeader)
            .statusBarsPadding()
            .heightIn(min = 52.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navIcon != null && onNav != null) {
            HeaderAction(navIcon, navDescription, onNav)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onClassicHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        CompositionLocalProvider(LocalContentColor provides colors.onClassicHeader) { actions() }
    }
}

/** Заголовок раздела: учитывает строку состояния Android, показывает кнопку меню (VK Classic) или настроек. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val openSettings = LocalOpenSettings.current
    val openDrawer = LocalOpenDrawer.current
    if (Kontakt.style.classic) {
        ClassicBar(title, modifier, KontaktIcons.Menu, "Меню", openDrawer, actions)
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
        if (openSettings != null) {
            HeaderAction(KontaktIcons.Settings, "Настройки", openSettings)
        }
    }
}

/** Заголовок вложенного экрана со стрелкой «назад». */
@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    if (Kontakt.style.classic) {
        ClassicBar(title, modifier, KontaktIcons.Back, "Назад", onBack) {}
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 56.dp)
            .padding(start = VgSpacing.xs, end = VgSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderAction(KontaktIcons.Back, "Назад", onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = VgSpacing.xs),
        )
    }
}

@Composable
fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val tint = if (Kontakt.style.classic) Kontakt.colors.onClassicHeader else MaterialTheme.colorScheme.onSurface
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Какой список повторяют «скелетоны» во время первой загрузки. */
enum class SkeletonKind { Chats, Posts }

/**
 * Заглушки вместо крутилки: форма будущего списка видна сразу, поэтому загрузка воспринимается быстрее.
 * В облегчённом режиме эффектов мерцания нет.
 */
@Composable
fun SkeletonList(kind: SkeletonKind, modifier: Modifier = Modifier) {
    val light = Kontakt.effects == EffectsMode.Light
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val alpha = if (light) 0.6f else pulse
    val bar = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    val style = Kontakt.style
    Column(modifier.fillMaxSize()) {
        when (kind) {
            SkeletonKind.Chats -> repeat(9) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(style.chatRowHeight)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(style.chatAvatar).clip(CircleShape).background(bar))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
                        Box(Modifier.fillMaxWidth(0.8f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bar))
                    }
                }
            }
            SkeletonKind.Posts -> repeat(3) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = style.screenPadding, vertical = style.postGap / 2),
                    shape = style.cardShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(bar))
                            Spacer(Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
                                Box(Modifier.width(90.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bar))
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
                        Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(bar))
                    }
                }
            }
        }
    }
}

@Composable
fun StateMessage(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(VgSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Kontakt.colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Kontakt.colors.accentText, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(VgSpacing.lg))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(VgSpacing.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(VgSpacing.lg))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val title = when (error.kind) {
        UiError.Kind.Offline -> "Нет соединения"
        UiError.Kind.AccessDenied -> "Нет доступа к данным"
        UiError.Kind.SessionExpired -> "Сессия истекла"
        UiError.Kind.Other -> "Что-то пошло не так"
    }
    val canRetry = error.kind == UiError.Kind.Offline || error.kind == UiError.Kind.Other
    StateMessage(
        title = title,
        text = error.message,
        modifier = modifier,
        actionLabel = if (canRetry) "Повторить" else null,
        onAction = if (canRetry) onRetry else null,
        icon = KontaktIcons.Info,
    )
}

/** Блок контента: плоский белый блок на сером (VK Classic) или мягкая карточка (Kontakt). */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    titleTextAlign: TextAlign = TextAlign.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = Kontakt.style
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = style.cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = if (style.cardBordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(style.sectionPadding)) {
            Text(
                text = if (style.classic) title else title.uppercase(),
                style = if (style.classic) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                color = if (style.classic) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = titleTextAlign,
                modifier = if (titleTextAlign == TextAlign.Center) Modifier.fillMaxWidth() else Modifier,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Пояснение внутри экрана: значок и короткий текст на мягкой подложке акцента. */
@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier) {
    val style = Kontakt.style
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = style.screenPadding, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Kontakt.colors.accentSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            KontaktIcons.Info,
            contentDescription = null,
            tint = Kontakt.colors.accentText,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Строка-ссылка: значок, название, пояснение и стрелка. Вся строка нажимается. */
@Composable
fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Kontakt.colors.accentText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            KontaktIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Поле поиска: заливка вместо рамки (капсула в Kontakt, слегка скруглённый прямоугольник в VK Classic). */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = if (Kontakt.style.classic) RoundedCornerShape(6.dp) else CircleShape
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(colors.surfaceVariant),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    KontaktIcons.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            KontaktIcons.Close,
                            contentDescription = "Очистить поиск",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}

/** Круглый (Kontakt) или слегка скруглённый (VK Classic) счётчик непрочитанного. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val shape = if (Kontakt.style.classic) RoundedCornerShape(4.dp) else CircleShape
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Переключатель вкладок внутри экрана (например, «Для вас» и «Подписки»): подчёркивание плавно перетекает
 * на выбранную вкладку. В облегчённом режиме эффектов переключается мгновенно.
 */
@Composable
fun SegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val light = Kontakt.effects == EffectsMode.Light
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    targetValue = if (selected) Kontakt.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = if (light) snap() else tween(160),
                    label = "segmentTint",
                )
                val spec: AnimationSpec<Dp> = if (light) snap() else spring(dampingRatio = 0.75f, stiffness = 500f)
                val indicator by animateDpAsState(
                    targetValue = if (selected) 44.dp else 0.dp,
                    animationSpec = spec,
                    label = "segmentIndicator",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = tint,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(indicator)
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(Kontakt.colors.accentText),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
