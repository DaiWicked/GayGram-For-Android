package org.monogram.core.common.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationLedgerTest {
    @Test
    fun countAndDuplicateIdentitySurviveSerializationBeyondVisibleHistory() {
        var state = NotificationLedger()
        repeat(20) { state = state.append(it + 1, null) }
        val restored = NotificationLedger.decode(state.encode())
        assertEquals(20, restored.pending.size)
        assertEquals(restored, restored.append(2, null))
        assertEquals(20, restored.append(2, null).pending.size)
    }

    @Test
    fun readAndDeleteMarkersPreventRedeliveryAcrossRestart() {
        val state = NotificationLedger().append(1, null).append(3, null).append(5, null)
            .retain(setOf(3), 1)
        val restored = NotificationLedger.decode(state.encode())
        assertFalse(restored.accepts(1))
        assertFalse(restored.accepts(3))
        assertTrue(restored.accepts(2))
        assertEquals(setOf(5), restored.pending.keys)
        assertEquals(setOf(5), restored.append(3, null).pending.keys)
    }

    @Test
    fun dismissalAndUnboundedReadDoNotBlockFutureMessages() {
        val state = NotificationLedger().append(3, null).append(5, null)
        for (cleared in listOf(state.dismiss(), state.retain(emptySet(), Int.MAX_VALUE))) {
            val restored = NotificationLedger.decode(cleared.encode())
            assertFalse(restored.accepts(5))
            assertTrue(restored.accepts(6))
            assertEquals(setOf(6), restored.append(6, null).pending.keys)
        }
    }

    @Test
    fun topicReadKeepsOtherTopicsAndSurvivesRestart() {
        val state = NotificationLedger().append(1, 10).append(2, 20).append(3, 10)
            .retain(emptySet(), 2, 10)
        val restored = NotificationLedger.decode(state.encode())
        assertEquals(setOf(2, 3), restored.pending.keys)
        assertTrue(restored.accepts(2))
        assertFalse(restored.accepts(1))
    }

    @Test
    fun malformedStorageIsSafeAndStateIsBounded() {
        assertEquals(NotificationLedger(), NotificationLedger.decode("invalid"))
        val encoded = "0|" + (1..5000).joinToString(",") { "$it:0" } + "|"
        assertEquals(
            NotificationLedger.MAX_PENDING,
            NotificationLedger.decode(encoded).pending.size
        )
    }
}
