package app.vgradesk.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.vgradesk.client.core.EffectsMode
import app.vgradesk.client.ui.theme.Kontakt

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Высота плавающей панели без системного отступа: круглые кнопки 52 dp + внутренние и внешние поля. */
val GlassNavBarHeight = 52.dp + 8.dp * 2 + 12.dp * 2

/**
 * Плавающая «стеклянная» панель навигации: полупрозрачная заливка, световой блик по кромке и мягкая тень.
 * ВАЖНО: размытия содержимого под панелью здесь НЕТ — это просто полупрозрачная заливка. Настоящее размытие
 * (отдельная задача в NEXT_STEPS) требует отрисовки содержимого в слой и его размытия.
 * В облегчённом режиме панель непрозрачная, без тени и без анимации.
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
    val shape = RoundedCornerShape(36.dp)

    var barModifier = Modifier.clip(shape)
    if (!light) {
        barModifier = Modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = colors.glassShadow,
                spotColor = colors.glassShadow,
            )
            .clip(shape)
    }
    val fill = if (light) MaterialTheme.colorScheme.surfaceContainerHigh else colors.glass
    val edge = if (light) {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))
    } else {
        Brush.verticalGradient(listOf(colors.glassHighlight, colors.glassEdge))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = barModifier
                .background(fill)
                .border(width = 1.dp, brush = edge, shape = shape)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavCircle(item = item, selected = item.route == selectedRoute, onClick = { onSelect(item.route) })
            }
        }
    }
}

@Composable
private fun NavCircle(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val instant = Kontakt.effects == EffectsMode.Light
    val background by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Transparent,
        animationSpec = if (instant) snap() else tween(durationMillis = 220),
        label = "navCircleBackground",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = if (instant) snap() else tween(durationMillis = 220),
        label = "navCircleTint",
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = item.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
