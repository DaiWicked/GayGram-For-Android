package org.monogram.core.ui.haptics

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.monogram.core.ui.HapticsMode

private const val ThrottleMs = 70L
private const val EdgeCooldownMs = 400L

/**
 * Installs the app's touch feedback for everything below it.
 *
 * [HapticsMode.System] deliberately reads no system setting: `performHapticFeedback` already
 * honours the system haptic-feedback switch and stays silent when the user turned it off.
 */
@Composable
fun ProvideHaptics(mode: HapticsMode = HapticsMode.System, content: @Composable () -> Unit) {
    val feedback = LocalHapticFeedback.current
    val haptics = remember(feedback, mode) { ThrottledHaptics(feedback, mode) }
    CompositionLocalProvider(LocalHaptics provides haptics, content = content)
}

private class ThrottledHaptics(
    private val feedback: HapticFeedback,
    private val mode: HapticsMode,
) : Haptics {
    private var lastTapAt = 0L
    private var lastEdgeAt = 0L

    override fun perform(intent: HapticIntent) {
        if (mode == HapticsMode.Off) return
        val now = SystemClock.uptimeMillis()
        if (intent == HapticIntent.Edge) {
            if (now - lastEdgeAt < EdgeCooldownMs) return
            lastEdgeAt = now
        } else {
            if (now - lastTapAt < ThrottleMs) return
            lastTapAt = now
        }
        feedback.performHapticFeedback(intent.feedbackType())
    }
}

private fun HapticIntent.feedbackType(): HapticFeedbackType = when (this) {
    HapticIntent.Tick, HapticIntent.HandleMove -> HapticFeedbackType.SegmentTick
    HapticIntent.Edge, HapticIntent.ScrollEnd -> HapticFeedbackType.GestureEnd
    HapticIntent.ToggleOn -> HapticFeedbackType.ToggleOn
    HapticIntent.ToggleOff -> HapticFeedbackType.ToggleOff
    HapticIntent.Press -> HapticFeedbackType.ContextClick
    HapticIntent.LongPress -> HapticFeedbackType.LongPress
    HapticIntent.Threshold -> HapticFeedbackType.GestureThresholdActivate
    HapticIntent.Confirm -> HapticFeedbackType.Confirm
    HapticIntent.Reject -> HapticFeedbackType.Reject
}
