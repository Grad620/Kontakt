package app.kontakt.client.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/** Нижний отступ, который нужно оставить в списках, чтобы плавающая панель не закрывала последние элементы. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/** Открыть экран настроек. null — на этом экране кнопки настроек нет. */
val LocalOpenSettings = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Заголовок раздела: учитывает строку состояния Android и показывает кнопку настроек. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val openSettings = LocalOpenSettings.current
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
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun StateMessage(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(VgSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
    )
}

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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (style.cardBordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(if (style.id == "vk_classic") 10.dp else 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = Kontakt.colors.accentText,
                textAlign = titleTextAlign,
                modifier = if (titleTextAlign == TextAlign.Center) Modifier.fillMaxWidth() else Modifier,
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
