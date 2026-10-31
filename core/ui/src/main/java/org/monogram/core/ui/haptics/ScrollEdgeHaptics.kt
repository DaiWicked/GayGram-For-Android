package org.monogram.core.ui.haptics

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Ticks once when the finger drags a list past its bottom edge.
 *
 * Only a finger counts: a fling that coasts to the end stays silent, and a tap never ticks
 * because the finger has to travel past touch slop first.
 */
@Composable
fun Modifier.scrollEdgeHaptics(state: LazyListState): Modifier {
    val haptics = LocalHaptics.current
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(state, haptics) {
        snapshotFlow {
            val scrollable = state.canScrollForward || state.canScrollBackward
            dragging && scrollable && !state.canScrollForward
        }
            .distinctUntilChanged()
            .collect { atEdge -> if (atEdge) haptics.perform(HapticIntent.Edge) }
    }

    return this.pointerInput(Unit) {
        var travel = 0f
        var wasDown = false
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                if (change.pressed) {
                    if (!wasDown) travel = 0f
                    wasDown = true
                    travel += (change.position - change.previousPosition).getDistance()
                    dragging = travel > viewConfiguration.touchSlop
                } else {
                    wasDown = false
                    travel = 0f
                    dragging = false
                }
            }
        }
    }
}
