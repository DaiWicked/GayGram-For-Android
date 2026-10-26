package org.monogram.core.ui.glass

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.monogram.core.ui.theme.MonogramTheme

/**
 * Every [GlassTokens] preset over a high-contrast chart, so a preset can be judged on a device.
 *
 * The backdrop is deliberately loud: a glass surface over a flat colour is indistinguishable from a
 * plain translucent fill, because there is nothing behind it for the blur and the lens to displace.
 */
@Composable
fun GlassShowcase(modifier: Modifier = Modifier) {
    val backdrop = rememberLayerBackdrop()
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Box(modifier.fillMaxSize()) {
        GlassChart(Modifier.layerBackdrop(backdrop).fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "API ${Build.VERSION.SDK_INT} · ${PlatformCapabilities.supportedTier}" +
                    " · shader ${PlatformCapabilities.supportsRuntimeShader}" +
                    " · tint ${GlassTokens.tintAlpha(dark)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            PresetRow(backdrop, dark, "Thin", GlassTokens.Thin, RoundedCornerShape(32.dp))
            PresetRow(backdrop, dark, "Button", GlassTokens.Button, RoundedCornerShape(24.dp))
            PresetRow(backdrop, dark, "Panel", GlassTokens.Panel, RoundedCornerShape(24.dp))
            PresetRow(
                backdrop,
                dark,
                "TabBar tinted",
                GlassTokens.TabBar,
                RoundedCornerShape(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            PresetRow(backdrop, dark, "Dialog", GlassTokens.Dialog, RoundedCornerShape(28.dp))
        }
    }
}

/** Saturated bands, hard stripes and text: all three make a displaced backdrop obvious. */
@Composable
private fun GlassChart(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF0A0A0F))
            val bands = listOf(
                Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
                Color(0xFF00ACC1), Color(0xFF3949AB), Color(0xFF8E24AA), Color(0xFFD81B60),
            )
            val bandWidth = size.width / bands.size
            bands.forEachIndexed { index, color ->
                drawRect(
                    color = color,
                    topLeft = Offset(index * bandWidth, 0f),
                    size = Size(bandWidth, size.height * 0.52f),
                )
            }
            val stripe = 10.dp.toPx()
            val gap = 26.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = Color.White,
                    start = Offset(x, size.height * 0.52f),
                    end = Offset(x + size.height * 0.48f, size.height),
                    strokeWidth = stripe,
                )
                x += gap
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            repeat(6) { line ->
                Text(
                    text = "▸ ▸ ▸  ABCDEFG 0123456789  $line",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    backdrop: Backdrop,
    dark: Boolean,
    label: String,
    surface: GlassSurface,
    shape: Shape,
    tint: Color = Color.Unspecified,
) {
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        surface = surface,
        tint = tint,
    ) {
        Text(
            text = "$label  blur ${surface.blurRadius(dark).value.toInt()}" +
                "  lens ${surface.refractionHeight.value.toInt()}/${surface.refractionAmount.value.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun GlassShowcaseLightPreview() {
    MonogramTheme(darkTheme = false) { GlassShowcase() }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GlassShowcaseDarkPreview() {
    MonogramTheme(darkTheme = true) { GlassShowcase() }
}
