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
        assertEquals(HapticsMode.On, AppearanceSettings.parseHapticsMode("on"))
    }

    @Test
    fun unknownNameFollowsTheSystem() {
        assertEquals(HapticsMode.System, AppearanceSettings.parseHapticsMode("buzz"))
    }
}
