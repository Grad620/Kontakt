package app.kontakt.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Плоская нижняя панель вкладок на всю ширину (стиль Kontakt); в VK Classic её заменяет боковое меню. */
val GlassNavBarHeight = 62.dp

@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), thickness = 0.7.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val light = Kontakt.effects == EffectsMode.Light
                // Выбранная вкладка — акцентным текстом (читаем на любом из восьми акцентов), а не сырой заливкой.
                val tint by animateColorAsState(
                    targetValue = if (selected) Kontakt.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = if (light) snap() else tween(150),
                    label = "tabTint",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.1f else 1f,
                    animationSpec = if (light) snap() else spring(dampingRatio = 0.45f, stiffness = 600f),
                    label = "tabScale",
                )
                val pill by animateColorAsState(
                    targetValue = if (selected) Kontakt.colors.accentSoft else Color.Transparent,
                    animationSpec = if (light) snap() else tween(180),
                    label = "tabPill",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) })
                        .semantics { contentDescription = item.label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 29.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                    }
                    Text(
                        item.label,
                        color = tint,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
