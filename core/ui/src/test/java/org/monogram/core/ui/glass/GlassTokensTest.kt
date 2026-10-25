package org.monogram.core.ui.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassTokensTest {
    @Test fun dialogBlurNarrowsWhenDark() {
        assertEquals(16.dp, GlassTokens.Dialog.blurRadius(dark = false))
        assertEquals(8.dp, GlassTokens.Dialog.blurRadius(dark = true))
    }

    @Test fun otherSurfacesBlurTheSameInBothThemes() {
        val surfaces = listOf(GlassTokens.Thin, GlassTokens.Button, GlassTokens.Panel, GlassTokens.TabBar)
        surfaces.forEach { surface ->
            assertEquals(surface.blurRadius, surface.blurRadius(dark = true))
        }
    }
}
