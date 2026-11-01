package org.monogram.core.ui.haptics

import androidx.compose.runtime.compositionLocalOf

/** Touch feedback, reduced to the intents the app actually uses. */
fun interface Haptics {
    fun perform(intent: HapticIntent)
}

private object NoopHaptics : Haptics {
    override fun perform(intent: HapticIntent) = Unit
}

/** Silent by default, so a preview or a test that never installs [ProvideHaptics] cannot buzz. */
val LocalHaptics = compositionLocalOf<Haptics> { NoopHaptics }
