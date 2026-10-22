package app.kontakt.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
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

/** Нижняя стеклянная панель Kontakt с круглыми иконками; в VK Classic её заменяет боковое меню. */
val GlassNavBarHeight = 76.dp

@Composable
fun GlassNavBar(
    items: List<NavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panelShape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .shadow(
                elevation = 18.dp,
                shape = panelShape,
                ambientColor = Kontakt.colors.glassShadow,
                spotColor = Kontakt.colors.glassShadow,
            )
            .clip(panelShape)
            .background(Kontakt.colors.glass)
            .border(
                width = 1.dp,
                color = Kontakt.colors.glassHighlight.copy(alpha = 0.65f),
                shape = panelShape,
            )
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 5.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.route == selectedRoute
                val light = Kontakt.effects == EffectsMode.Light
                val circleColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    animationSpec = if (light) snap() else tween(180),
                    label = "tabCircle",
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = if (light) snap() else tween(160),
                    label = "tabTint",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.06f else 1f,
                    animationSpec = if (light) snap() else spring(dampingRatio = 0.48f, stiffness = 520f),
                    label = "tabScale",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) })
                        .semantics { contentDescription = item.label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(23.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (selected) Kontakt.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
