package org.monogram.core.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height the floating tab bar occupies at the bottom of the screen, or zero when it is hidden.
 * Scroll containers add it to their bottom content padding so the last row can clear the bar
 * while still scrolling underneath its glass.
 */
val LocalBottomBarInset = compositionLocalOf<Dp> { 0.dp }
