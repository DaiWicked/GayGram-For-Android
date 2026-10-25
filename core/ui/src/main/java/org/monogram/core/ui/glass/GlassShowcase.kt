package org.monogram.core.ui.glass

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.monogram.core.ui.components.MonogramWallpaper
import org.monogram.core.ui.theme.MonogramTheme

/** Every [GlassTokens] preset over the app wallpaper, so a preset can be judged before it ships. */
@Composable
fun GlassShowcase(modifier: Modifier = Modifier) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxSize()) {
        MonogramWallpaper(Modifier.layerBackdrop(backdrop).fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PresetRow(backdrop, "Thin", GlassTokens.Thin, RoundedCornerShape(32.dp), Color.Unspecified)
            PresetRow(backdrop, "Button", GlassTokens.Button, RoundedCornerShape(24.dp), Color.Unspecified)
            PresetRow(backdrop, "Panel", GlassTokens.Panel, RoundedCornerShape(24.dp), Color.Unspecified)
            PresetRow(
                backdrop,
                "TabBar tinted",
                GlassTokens.TabBar,
                RoundedCornerShape(24.dp),
                MaterialTheme.colorScheme.primary,
            )
            PresetRow(backdrop, "Dialog", GlassTokens.Dialog, RoundedCornerShape(28.dp), Color.Unspecified)
        }
    }
}

@Composable
private fun PresetRow(
    backdrop: Backdrop,
    label: String,
    surface: GlassSurface,
    shape: Shape,
    tint: Color,
) {
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        surface = surface,
        tint = tint,
    ) {
        Text(
            text = "$label  blur ${surface.blurRadius}  lens ${surface.refractionHeight}/${surface.refractionAmount}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 20.dp, vertical = 22.dp),
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
