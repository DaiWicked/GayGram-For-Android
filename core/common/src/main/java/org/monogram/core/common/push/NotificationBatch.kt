package org.monogram.core.common.push

/**
 * One entry of a collapsed chat notification. Android keeps a short message history inside the
 * single per-chat notification so a burst of messages is shown as one conversation instead of
 * replacing the previous message.
 */
data class NotificationMessage(
    val messageId: Int,
    val text: String,
    val timestamp: Long,
    val outgoing: Boolean = false,
    val senderName: String? = null,
    val senderKey: String? = null,
    val topicId: Int? = null,
    val pictureUri: String? = null,
    val pictureMimeType: String = "image/jpeg",
    val edited: Boolean = false,
)

/**
 * Batching rules for chat notifications. Kept free of Android types so the collapse behaviour is
 * unit tested without a device.
 */
object NotificationBatch {
    /** Upper bound of the history kept in one chat notification. */
    const val MAX_MESSAGES: Int = 8

    /**
     * Appends [incoming] to [existing] and keeps the newest [max] entries. An entry is a repeat when
     * it carries the same Telegram message id, or, for payloads without an id, the same text and
     * timestamp; repeats replace the earlier copy instead of stacking a second bubble.
     */
    fun append(
        existing: List<NotificationMessage>,
        incoming: NotificationMessage,
        max: Int = MAX_MESSAGES,
    ): List<NotificationMessage> {
        if (max <= 0) return emptyList()
        val index = existing.indexOfFirst { it.isRepeatOf(incoming) }
        if (index >= 0) {
            return existing.mapIndexed { i, message ->
                if (i == index && (!message.edited || incoming.edited)) incoming.copy(
                    timestamp = message.timestamp,
                    pictureUri = incoming.pictureUri ?: message.pictureUri,
                    pictureMimeType = if (incoming.pictureUri == null) message.pictureMimeType else incoming.pictureMimeType,
                )
                else message
            }.takeLast(max)
        }
        return (existing + incoming).takeLast(max)
    }

    fun attachPicture(
        existing: List<NotificationMessage>,
        messageId: Int,
        uri: String,
        mimeType: String = "image/jpeg",
    ): List<NotificationMessage> = existing.map {
        if (it.messageId == messageId) it.copy(pictureUri = uri, pictureMimeType = mimeType) else it
    }

    /**
     * Message count for a notification that hides message bodies: every push adds one, a quiet
     * repaint keeps the count that is already on screen.
     */
    fun countHidden(previousCount: Int, isNewPush: Boolean): Int =
        (previousCount.coerceAtLeast(0) + if (isNewPush) 1 else 0).coerceAtLeast(1)

    /**
     * Drops deleted ids, and every message at or below [upTo] when a read cursor is set.
     * Messages without an id stay unless the cursor marks the whole chat.
     */
    fun retain(
        existing: List<NotificationMessage>,
        dropIds: Set<Int> = emptySet(),
        upTo: Int? = null,
    ): List<NotificationMessage> = existing.filterNot { message ->
        message.messageId in dropIds ||
            (upTo != null && upTo > 0 && message.messageId > 0 && message.messageId <= upTo) ||
            (upTo != null && upTo >= Int.MAX_VALUE && message.messageId <= 0)
    }
}

fun historyReadUpTo(messageId: Int, maxId: Int): Int {
    val cursor = maxOf(messageId, maxId)
    return if (cursor > 0) cursor else Int.MAX_VALUE
}

private fun NotificationMessage.isRepeatOf(other: NotificationMessage): Boolean =
    if (messageId > 0 && other.messageId > 0) {
        messageId == other.messageId
    } else {
        messageId == other.messageId && text == other.text && timestamp == other.timestamp
    }
