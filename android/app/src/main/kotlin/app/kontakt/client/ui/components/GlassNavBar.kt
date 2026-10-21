package app.kontakt.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kontakt.client.core.EffectsMode
import app.kontakt.client.ui.theme.Kontakt

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Высота компактной нижней панели без системного inset. */
val GlassNavBarHeight = 68.dp + 8.dp

/**
 * Компактная навигация: иконка + подпись, без огромных круглых кнопок.
 * Активная вкладка получает короткую цветную капсулу и лёгкий spring-переход.
 * Панель не притворяется настоящим blur: поверхность остаётся лёгкой и читаемой.
 */
@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kontakt.colors
    val light = Kontakt.effects == EffectsMode.Light
    val shape = RoundedCornerShape(24.dp)
    val fill = if (light) MaterialTheme.colorScheme.surfaceContainerHigh else colors.glass
    val edge = if (light) {
        Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant),
        )
    } else {
        Brush.verticalGradient(listOf(colors.glassHighlight, colors.glassEdge))
    }

    var panelModifier: Modifier = Modifier
    if (!light) {
        panelModifier = panelModifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = colors.glassShadow,
            spotColor = colors.glassShadow,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = panelModifier
                .fillMaxWidth()
                .clip(shape)
                .background(fill)
                .border(1.dp, edge, shape)
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavDestination(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavDestination(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val instant = Kontakt.effects == EffectsMode.Light
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = if (instant) snap() else spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "navIconScale",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = if (instant) snap() else tween(durationMillis = 180),
        label = "navIconTint",
    )
    val indicator by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = 0.13f) else Color.Transparent,
        animationSpec = if (instant) snap() else tween(durationMillis = 200),
        label = "navIndicator",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = if (instant) snap() else tween(durationMillis = 160),
        label = "navLabelTint",
    )

    Column(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = item.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 30.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        Text(
            text = item.label,
            color = labelColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
