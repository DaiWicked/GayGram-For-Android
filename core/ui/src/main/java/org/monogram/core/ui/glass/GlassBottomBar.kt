package org.monogram.core.ui.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.kyant.backdrop.Backdrop

/** One destination in [GlassBottomBar]. The label is user-visible, so it arrives translated. */
data class GlassBarTab(val icon: ImageVector, val label: String)

private val BarHeight = 56.dp
private val BarShape = RoundedCornerShape(percent = 50)
private val BarInnerPadding = 6.dp
private val IndicatorHeight = BarHeight - BarInnerPadding * 2

/**
 * The floating bar from the design guide: a capsule of glass with one sliding indicator, used for
 * the app's top-level destinations. It samples [backdrop], so the content it floats over has to
 * keep scrolling underneath it instead of stopping above the bar.
 */
@Composable
fun GlassBottomBar(
    backdrop: Backdrop,
    tabs: List<GlassBarTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val indicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = BarShape,
        surface = GlassTokens.TabBar,
        tint = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(BarHeight),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(BarInnerPadding)) {
            val slot = maxWidth / tabs.size
            val selected = selectedIndex.coerceIn(0, tabs.lastIndex)
            val indicatorOffset by animateDpAsState(
                targetValue = slot * selected,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "barIndicator",
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slot)
                    .height(IndicatorHeight)
                    .background(indicatorColor, BarShape),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val active = index == selected
                    val color = if (active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .width(slot)
                            .height(IndicatorHeight)
                            .selectable(
                                selected = active,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = color,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
