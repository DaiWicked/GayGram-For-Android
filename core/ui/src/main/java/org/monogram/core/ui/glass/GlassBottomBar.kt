package org.monogram.core.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import kotlin.math.abs
import kotlin.math.roundToInt

/** One destination in [GlassBottomBar]. The label is user-visible, so it arrives translated. */
data class GlassBarTab(val icon: ImageVector, val label: String)

private val BarHeight = 64.dp
private val BarShape = RoundedCornerShape(percent = 50)
private val BarInnerPadding = 4.dp
private val IndicatorHeight = BarHeight - BarInnerPadding * 2

// Critically damped: the indicator must arrive without overshooting. A bouncy spring reads as the
// chip snapping back rather than as the finger having placed it.
private val IndicatorSpring = spring<Float>(dampingRatio = 1f, stiffness = 500f)

// Only the copy of the row that the indicator samples is scaled up. Scaling the row the user sees
// would move the labels under the finger instead of inside the glass.
private val LocalBarTabScale = staticCompositionLocalOf { { 1f } }

/**
 * The floating bar from the design guide: a capsule of glass with one indicator that can be dragged
 * across it, used for the app's top-level destinations. It samples [backdrop], so the content it
 * floats over has to keep scrolling underneath instead of stopping above the bar.
 *
 * The indicator is glass only while a finger is on it. At rest it is a flat scrim with a hairline
 * edge: a lens that never turns off would bend the labels and the page into each other, which reads
 * as noise rather than as a selected tab.
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
    val barFill = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
    val indicatorFill = if (dark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val accent = MaterialTheme.colorScheme.primary
    val resting = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val selected = selectedIndex.coerceIn(0, tabs.lastIndex)
    // The gesture lambda outlives the composition that created it, so it has to read the latest
    // selection through a state holder instead of capturing the value it started with.
    val currentSelected by rememberUpdatedState(selected)

    // The indicator refracts the labels it sits on, not just the page behind the bar, so the row is
    // drawn twice: once for the eye and once, invisible, into a layer the glass samples. The hidden
    // copy is the one that grows on press, which is why the labels only swell inside the pill.
    val tabsBackdrop = rememberLayerBackdrop()
    val indicatorBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    var pressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var pressStartIndex by remember { mutableIntStateOf(selected) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "barPress",
    )

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
            val slotPx = with(density) { slot.toPx() }
            val travelPx = with(density) { (maxWidth - slot).toPx() }
            // One animated value drives both cases: while dragging it chases the finger, and on
            // release it is already standing where the finger left it, so nothing jumps back.
            val targetOffsetPx = if (dragging && slotPx > 0f) {
                (pressStartIndex * slotPx + dragPx).coerceIn(0f, travelPx)
            } else {
                (selected * slotPx).coerceIn(0f, travelPx)
            }
            val offsetPx by animateFloatAsState(
                targetValue = targetOffsetPx,
                animationSpec = IndicatorSpring,
                label = "indicatorOffset",
            )
            val indicatorOffset = with(density) { offsetPx.toDp() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(tabs.size, slotPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            pressStartIndex = currentSelected
                            dragPx = 0f
                            pressed = true
                            var lastX = down.position.x
                            var moved = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dx = change.position.x - lastX
                                lastX = change.position.x
                                if (!moved && abs(change.position.x - down.position.x) >
                                    viewConfiguration.touchSlop
                                ) {
                                    moved = true
                                    dragging = true
                                }
                                if (moved) {
                                    dragPx += dx
                                    change.consume()
                                }
                            }
                            pressed = false
                            if (moved) {
                                val target = if (slotPx > 0f) {
                                    (pressStartIndex + dragPx / slotPx).roundToInt()
                                } else {
                                    currentSelected
                                }
                                dragPx = 0f
                                dragging = false
                                val index = target.coerceIn(0, tabs.lastIndex)
                                if (index != currentSelected) onSelect(index)
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(slot)
                        .height(IndicatorHeight)
                        .graphicsLayer { alpha = 1f - pressProgress * 0.4f }
                        .clip(BarShape)
                        .background(indicatorFill)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = BarShape,
                        )
                )
                // Recorded before the indicator so the layer is there on the first frame that draws
                // it. The copy rebuilds the bar's own material — same blur, same fill — instead of
                // leaving the page showing behind the labels: the indicator has to refract the bar
                // it sits on, not the conversation underneath the bar.
                CompositionLocalProvider(
                    LocalBarTabScale provides { 1f + 0.2f * pressProgress },
                ) {
                    TabRow(
                        tabs = tabs,
                        selected = selected,
                        slot = slot,
                        itemHeight = IndicatorHeight,
                        colorFor = { accent },
                        interaction = false,
                        onSelect = onSelect,
                        modifier = Modifier
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { BarShape },
                                effects = {
                                    padding = maxOf(
                                        padding,
                                        GlassTokens.TabBar.blurRadius(dark).toPx(),
                                    )
                                    vibrancy()
                                    blur(GlassTokens.TabBar.blurRadius(dark).toPx())
                                },
                                highlight = null,
                                shadow = null,
                                onDrawSurface = { drawRect(barFill) },
                            ),
                    )
                }
                if (pressProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(slot)
                            .height(IndicatorHeight)
                            .graphicsLayer { alpha = pressProgress }
                            .drawBackdrop(
                                backdrop = indicatorBackdrop,
                                shape = { BarShape },
                                effects = {
                                    padding = maxOf(padding, 24.dp.toPx())
                                    vibrancy()
                                    lens(
                                        refractionHeight = 10.dp.toPx() * pressProgress,
                                        refractionAmount = 14.dp.toPx() * pressProgress,
                                        depthEffect = true,
                                        chromaticAberration = true,
                                    )
                                },
                                highlight = { indicatorHighlight(dark) },
                                shadow = null,
                                innerShadow = {
                                    InnerShadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.10f))
                                },
                            ),
                    )
                }
                TabRow(
                    tabs = tabs,
                    selected = selected,
                    slot = slot,
                    itemHeight = IndicatorHeight,
                    colorFor = { index -> if (index == selected) accent else resting },
                    interaction = true,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun TabRow(
    tabs: List<GlassBarTab>,
    selected: Int,
    slot: Dp,
    itemHeight: Dp,
    colorFor: (Int) -> Color,
    interaction: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = LocalBarTabScale.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            val color = colorFor(index)
            Column(
                modifier = Modifier
                    .width(slot)
                    .height(itemHeight)
                    .then(
                        if (interaction) {
                            Modifier.selectable(selected = active, role = Role.Tab) {
                                onSelect(index)
                            }
                        } else {
                            Modifier.clearAndSetSemantics {}
                        },
                    )
                    .graphicsLayer {
                        val grown = scale()
                        scaleX = grown
                        scaleY = grown
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = if (interaction) tab.label else null,
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
