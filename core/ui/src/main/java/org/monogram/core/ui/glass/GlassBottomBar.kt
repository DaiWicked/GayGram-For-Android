package org.monogram.core.ui.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow

/** One destination in [GlassBottomBar]. The label is user-visible, so it arrives translated. */
data class GlassBarTab(val icon: ImageVector, val label: String)

private val BarHeight = 64.dp
private val BarShape = RoundedCornerShape(percent = 50)
private val BarInnerPadding = 4.dp
private val IndicatorHeight = BarHeight - BarInnerPadding * 2

/**
 * The floating bar from the design guide: a capsule of glass with one sliding indicator, used for
 * the app's top-level destinations. It samples [backdrop], so the content it floats over has to
 * keep scrolling underneath it instead of stopping above the bar.
 *
 * The indicator is glass of its own rather than a painted pill: it refracts the page behind the bar,
 * which is what separates a swimming capsule from a grey rounded rectangle.
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
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barFill = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.40f)
    val indicatorFill = if (dark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = BarShape,
        surface = GlassTokens.TabBar,
        flatTint = barFill,
        thickness = 6.dp,
        lift = 10.dp,
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
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { BarShape },
                        effects = {
                            padding = maxOf(padding, 24.dp.toPx())
                            vibrancy()
                            lens(
                                refractionHeight = 10.dp.toPx(),
                                refractionAmount = 14.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = true,
                            )
                        },
                        highlight = { indicatorHighlight(dark) },
                        shadow = null,
                        innerShadow = {
                            InnerShadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.10f))
                        },
                        onDrawSurface = { drawRect(indicatorFill) },
                    ),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val active = index == selected
                    val color = if (active) {
                        MaterialTheme.colorScheme.primary
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
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
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

// The indicator sits inside a lit capsule, so it gets a tighter rim than the bar around it.
private fun indicatorHighlight(dark: Boolean): Highlight = Highlight(
    width = 1.dp,
    alpha = if (dark) 0.75f else 0.5f,
    style = if (dark) {
        HighlightStyle.Default
    } else {
        HighlightStyle.Default(color = Color.Black, blendMode = BlendMode.SrcOver)
    },
)
