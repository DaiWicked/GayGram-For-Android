package org.monogram.core.common.push

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationBatchTest {
    private fun message(id: Int, text: String, at: Long = 0L) = NotificationMessage(id, text, at)

    @Test
    fun appendsMessagesInArrivalOrder() {
        val batch = NotificationBatch.append(emptyList(), message(1, "one"))
        val grown = NotificationBatch.append(batch, message(2, "two"))
        assertEquals(listOf("one", "two"), grown.map { it.text })
    }

    @Test
    fun repeatedMessageIdReplacesInsteadOfStacking() {
        val batch = listOf(message(1, "one"), message(2, "two"))
        val retried = NotificationBatch.append(batch, message(2, "two", at = 500L))
        assertEquals(2, retried.size)
        assertEquals(listOf(1, 2), retried.map { it.messageId })
        assertEquals(0L, retried.last().timestamp)
    }

    @Test
    fun staleDuplicateCannotOverwriteAnAuthoritativeEdit() {
        val edited = message(1, "edited", 10L).copy(edited = true, pictureUri = "content://photo/1")
        val batch = listOf(edited, message(2, "newer", 20L))
        assertEquals(batch, NotificationBatch.append(batch, message(1, "original", 30L)))
        val updated = NotificationBatch.append(batch, edited.copy(text = "edited again"))
        assertEquals("edited again", updated.first().text)
        assertEquals(updated, NotificationBatch.append(updated, message(1, "original", 40L)))
    }

    @Test
    fun editingOlderMessagePreservesOrderAndAttachment() {
        val batch = listOf(
            message(1, "old", 10L).copy(
                pictureUri = "content://photo/1",
                pictureMimeType = "image/webp"
            ), message(2, "new", 20L)
        )
        val edited = NotificationBatch.append(batch, message(1, "edited", 99L))
        assertEquals(listOf(1, 2), edited.map { it.messageId })
        assertEquals("edited", edited.first().text)
        assertEquals(10L, edited.first().timestamp)
        assertEquals("content://photo/1", edited.first().pictureUri)
        assertEquals("image/webp", edited.first().pictureMimeType)
    }

    @Test
    fun latePictureUpdatesOnlyItsMessageAndDoesNotResurrectDeletedMessages() {
        val batch = listOf(message(1, "photo"), message(2, "text"))
        val refreshed = NotificationBatch.attachPicture(batch, 1, "content://photo/1")
        assertEquals("content://photo/1", refreshed.first().pictureUri)
        assertEquals(null, refreshed.last().pictureUri)
        val deleted = NotificationBatch.retain(refreshed, dropIds = setOf(1))
        assertEquals(deleted, NotificationBatch.attachPicture(deleted, 1, "content://photo/1"))
        assertEquals(refreshed, NotificationBatch.append(refreshed, message(2, "text")))
    }

    @Test
    fun keepsOnlyNewestMessagesWithinTheLimit() {
        var batch = emptyList<NotificationMessage>()
        repeat(12) { index -> batch = NotificationBatch.append(batch, message(index + 1, "msg $index"), max = 3) }
        assertEquals(listOf(10, 11, 12), batch.map { it.messageId })
    }

    @Test
    fun payloadsWithoutMessageIdDedupeOnTextAndTimestamp() {
        val batch = listOf(message(0, "joined", at = 10L))
        assertEquals(1, NotificationBatch.append(batch, message(0, "joined", at = 10L)).size)
        assertEquals(2, NotificationBatch.append(batch, message(0, "joined", at = 11L)).size)
    }

    @Test
    fun nonPositiveLimitDropsEverything() {
        assertEquals(emptyList<NotificationMessage>(), NotificationBatch.append(emptyList(), message(1, "one"), max = 0))
    }

    @Test
    fun appendKeepsOutgoingRepliesDistinctFromIncoming() {
        val reply = NotificationMessage(2, "и хорошо", 20L, outgoing = true)
        val batch = NotificationBatch.append(listOf(message(1, "Понял"), reply), message(3, "ok"))
        assertEquals(listOf(false, true, false), batch.map { it.outgoing })
    }

    @Test
    fun readAndDeleteRemoveOnlyAffectedMessages() {
        val batch = listOf(message(2, "a"), message(5, "b"), message(9, "c"))
        assertEquals(listOf(9), NotificationBatch.retain(batch, dropIds = setOf(2, 5)).map { it.messageId })
        assertEquals(listOf(9), NotificationBatch.retain(batch, upTo = historyReadUpTo(4, 5)).map { it.messageId })
        assertEquals(9, historyReadUpTo(messageId = 4, maxId = 9))
        assertEquals(emptyList<Int>(), NotificationBatch.retain(batch, upTo = historyReadUpTo(0, 0)).map { it.messageId })
    }

    @Test
    fun hiddenCountGrowsWithPushesAndKeepsQuietRepaints() {
        assertEquals(1, NotificationBatch.countHidden(previousCount = 0, isNewPush = true))
        assertEquals(4, NotificationBatch.countHidden(previousCount = 3, isNewPush = true))
        assertEquals(3, NotificationBatch.countHidden(previousCount = 3, isNewPush = false))
        // A repaint of a notification that never counted before still shows one message.
        assertEquals(1, NotificationBatch.countHidden(previousCount = 0, isNewPush = false))
    }
}
