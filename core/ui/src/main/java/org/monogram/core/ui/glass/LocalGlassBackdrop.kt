package org.monogram.core.ui.glass

import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.Backdrop

/**
 * The backdrop a screen exposes to its own chrome.
 *
 * Screens that already record a backdrop provide it here so chrome several layers deep (the chat
 * composer sits under three) can draw glass without carrying a parameter through every signature.
 * Null means the screen offers no glass, and callers must draw an opaque surface instead.
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }
