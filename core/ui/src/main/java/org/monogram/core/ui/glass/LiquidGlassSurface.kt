package org.monogram.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Draws what is behind this composable, so [backdrop] must be recorded by an ancestor through
 * `Modifier.layerBackdrop(...)`. Without that ancestor the surface stays fully transparent.
 */
@Composable
fun LiquidGlassSurface(
    backdrop: Backdrop,
    shape: Shape,
    modifier: Modifier = Modifier,
    surface: GlassSurface = GlassTokens.Panel,
    tint: Color = Color.Unspecified,
    flatTint: Color = Color.Unspecified,
    thickness: Dp = 0.dp,
    lift: Dp = 0.dp,
    enabled: Boolean = true,
    fallback: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable BoxScope.() -> Unit,
) {
    // Layoutlib renders neither RenderEffect nor AGSL, so the preview gets the tier that draws.
    val tier = if (LocalInspectionMode.current) GlassTier.Tint else PlatformCapabilities.tier(enabled)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    if (tier == GlassTier.Off || tier == GlassTier.Tint) {
        val fill = if (tier == GlassTier.Off) fallback else fallback.copy(alpha = GlassTokens.FallbackAlpha)
        Box(modifier.clip(shape).background(fill), content = content)
        return
    }
    val refracted = tier == GlassTier.Refraction && shape.canRefract()
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                // The blur and the lens read past the rounded corner, so the layer has to be padded
                // by at least the larger of the two or the result is clipped to a hard edge.
                padding = maxOf(padding, surface.blurRadius(dark).toPx(), refractionPadding(surface, refracted))
                blur(surface.blurRadius(dark).toPx())
                if (refracted) {
                    lens(
                        refractionHeight = surface.refractionHeight.toPx(),
                        refractionAmount = surface.refractionAmount.toPx(),
                        depthEffect = surface.depthEffect
                    )
                }
            },
            highlight = { glassHighlight(dark) },
            shadow = if (lift > 0.dp) {
                { Shadow(radius = lift, color = Color.Black.copy(alpha = if (dark) 0.20f else 0.10f)) }
            } else {
                null
            },
            innerShadow = if (thickness > 0.dp) {
                { InnerShadow(radius = thickness, color = Color.Black.copy(alpha = 0.12f)) }
            } else {
                null
            },
            onDrawSurface = {
                if (flatTint != Color.Unspecified) {
                    drawRect(flatTint)
                } else if (tint != Color.Unspecified) {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = GlassTokens.tintAlpha(dark)))
                }
            }
        ),
        content = content
    )
}

private fun Density.refractionPadding(surface: GlassSurface, refracted: Boolean): Float =
    if (refracted) surface.refractionHeight.toPx() else 0f

// lens() throws for any shape it cannot derive a rounded-rect signed distance field from.
private fun Shape.canRefract(): Boolean = this is CornerBasedShape || this is AbsoluteRoundedCornerShape

/**
 * A lit rim, not a white outline. On a light surface a white line has nowhere to go — the increment is
 * alpha times the remaining headroom, which is under 20 of 255 on a near-white background — so the light
 * theme gets a dark hairline instead, the way a physical edge reads there.
 */
private fun glassHighlight(dark: Boolean): Highlight =
    Highlight(
        alpha = if (dark) 0.5f else 0.24f,
        style = if (dark) {
            HighlightStyle.Default
        } else {
            HighlightStyle.Default(color = Color.Black, blendMode = BlendMode.SrcOver)
        },
    )
