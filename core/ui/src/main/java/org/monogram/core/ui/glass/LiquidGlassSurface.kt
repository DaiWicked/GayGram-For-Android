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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

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
    enabled: Boolean = true,
    fallback: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable BoxScope.() -> Unit,
) {
    // Layoutlib renders neither RenderEffect nor AGSL, so the preview gets the tier that draws.
    val tier = if (LocalInspectionMode.current) GlassTier.Tint else PlatformCapabilities.tier(enabled)
    if (tier == GlassTier.Off || tier == GlassTier.Tint) {
        val fill = if (tier == GlassTier.Off) fallback else fallback.copy(alpha = GlassTokens.TintAlpha)
        Box(modifier.clip(shape).background(fill), content = content)
        return
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val refracted = tier == GlassTier.Refraction && shape.canRefract()
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(surface.blurRadius(dark).toPx())
                if (refracted) {
                    lens(
                        refractionHeight = surface.refractionHeight.toPx(),
                        refractionAmount = surface.refractionAmount.toPx(),
                        depthEffect = surface.depthEffect
                    )
                }
            },
            highlight = { Highlight.Default },
            shadow = null,
            onDrawSurface = {
                if (tint != Color.Unspecified) {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = GlassTokens.TintAlpha))
                }
            }
        ),
        content = content
    )
}

// lens() throws for any shape it cannot derive a rounded-rect signed distance field from.
private fun Shape.canRefract(): Boolean = this is CornerBasedShape || this is AbsoluteRoundedCornerShape
