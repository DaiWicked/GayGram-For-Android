package org.monogram.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HapticsModeTest {
    @Test
    fun missingPreferenceFollowsTheSystem() {
        assertEquals(HapticsMode.System, AppearanceSettings.parseHapticsMode(null))
    }

    @Test
    fun storedNameRestoresTheMode() {
        assertEquals(HapticsMode.Off, AppearanceSettings.parseHapticsMode("Off"))
        assertEquals(HapticsMode.Off, AppearanceSettings.parseHapticsMode("off"))
    }

    @Test
    fun theRetiredForcedModeReadsAsFollowingTheSystem() {
        assertEquals(HapticsMode.System, AppearanceSettings.parseHapticsMode("On"))
    }

    @Test
    fun unknownNameFollowsTheSystem() {
        assertEquals(HapticsMode.System, AppearanceSettings.parseHapticsMode("buzz"))
    }
}
