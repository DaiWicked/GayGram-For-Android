package org.monogram.core.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of a floating top bar above the content beneath it, or zero when the bar is not floating.
 * Scroll containers add it to their top content padding so the first row can clear the bar while
 * still scrolling underneath its glass.
 */
val LocalTopChromeInset = compositionLocalOf<Dp> { 0.dp }
