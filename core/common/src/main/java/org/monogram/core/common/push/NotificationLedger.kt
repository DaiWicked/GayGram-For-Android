package org.monogram.core.common.push

/** Only message identities and cursors are persisted; notification bodies remain owned by Android. */
data class NotificationLedger(
    val pending: Map<Int, Int?> = emptyMap(),
    val clearedThrough: Int = 0,
    val deleted: Set<Int> = emptySet(),
) {
    fun accepts(messageId: Int): Boolean =
        messageId <= 0 || (messageId > clearedThrough && messageId !in deleted)

    fun append(messageId: Int, topicId: Int?): NotificationLedger =
        if (messageId <= 0 || !accepts(messageId)) this
        else copy(
            pending = (pending + (messageId to topicId)).entries.toList().takeLast(MAX_PENDING)
                .associate { it.toPair() })

    fun retain(dropIds: Set<Int>, upTo: Int?, topicId: Int? = null): NotificationLedger {
        val cursor = if (upTo == Int.MAX_VALUE) pending.keys.maxOrNull() ?: clearedThrough else upTo
        val removed = if (topicId == null) dropIds else
            pending.filter { (id, topic) -> topic == topicId && (id in dropIds || (cursor != null && id <= cursor)) }.keys
        val cleared =
            if (topicId == null && cursor != null) maxOf(clearedThrough, cursor) else clearedThrough
        return copy(
            pending = pending.filter { (id, topic) ->
                (topicId != null && topic != topicId) || (id !in removed && (cursor == null || id > cursor))
            },
            clearedThrough = cleared,
            deleted = (deleted + removed).filter { it > cleared }.takeLast(MAX_PENDING).toSet(),
        )
    }

    fun dismiss(): NotificationLedger = copy(
        pending = emptyMap(), clearedThrough = maxOf(clearedThrough, pending.keys.maxOrNull() ?: 0),
    )

    fun encode(): String = "$clearedThrough|" +
            pending.entries.joinToString(",") { (id, topic) -> "$id:${topic ?: 0}" } + "|" + deleted.joinToString(
        ","
    )

    companion object {
        const val MAX_PENDING = 4096

        fun decode(value: String?): NotificationLedger {
            if (value == null) return NotificationLedger()
            val sections = value.split('|')
            val cursor = sections.firstOrNull()?.toIntOrNull() ?: return NotificationLedger()
            val deleted = sections.getOrNull(2).orEmpty().split(',').mapNotNull { it.toIntOrNull() }
                .filter { it > cursor }.takeLast(MAX_PENDING).toSet()
            val pending = sections.getOrNull(1).orEmpty().split(',').mapNotNull { entry ->
                val id = entry.substringBefore(':').toIntOrNull()
                    ?.takeIf { it > cursor && it !in deleted }
                    ?: return@mapNotNull null
                val topic = entry.substringAfter(':').toIntOrNull()?.takeIf { it > 0 }
                id to topic
            }.takeLast(MAX_PENDING).toMap()
            return NotificationLedger(pending, cursor, deleted)
        }
    }
}
