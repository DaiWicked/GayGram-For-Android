package org.monogram.core.ui.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // The tint absorbs the backdrop, so a bright backdrop needs the heavier one; getting this backwards
    // makes light surfaces wash out and dark surfaces look milky.
    @Test fun tintAbsorbsMoreOfADarkBackdrop() {
        assertTrue(GlassTokens.tintAlpha(dark = true) > GlassTokens.tintAlpha(dark = false))
    }

    @Test fun fallbackStandsInForGlassWithoutBeingGlass() {
        assertTrue(GlassTokens.FallbackAlpha > GlassTokens.tintAlpha(dark = true))
        assertTrue(GlassTokens.FallbackAlpha < 1f)
    }
}
