package org.monogram.feature.dialog.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test
import org.monogram.core.models.TextEntity
import org.monogram.feature.dialog.R

class MessageReplyQuoteTest {
    @Test
    fun quoteEntitiesRenderAsSpans() {
        val text = quoteAnnotatedString(
            quote = "quoted",
            entities = listOf(TextEntity("bold", 0, 6)),
            linkColor = Color.Red,
        )
        assertEquals("quoted", text.text)
        assertEquals(SpanStyle(fontWeight = FontWeight.Bold), text.spanStyles.single().item)
    }

    @Test
    fun quoteWithoutEntitiesKeepsPlainText() {
        val text = quoteAnnotatedString("quoted", emptyList(), Color.Red)
        assertEquals("quoted", text.text)
        assertEquals(0, text.spanStyles.size)
    }

    @Test
    fun mediaKindsThatHadNoLabelMapToTheirOwnLabel() {
        assertEquals(R.string.dialog_media_audio, replyMediaLabelRes("audio"))
        assertEquals(R.string.dialog_media_voice, replyMediaLabelRes("voice"))
        assertEquals(R.string.dialog_media_video_note, replyMediaLabelRes("video_note"))
    }

    @Test
    fun kindsThatAlreadyHadALabelKeepIt() {
        assertEquals(R.string.dialog_reply_photo, replyMediaLabelRes("photo"))
        assertEquals(R.string.dialog_reply_video, replyMediaLabelRes("video"))
        assertEquals(R.string.dialog_reply_sticker, replyMediaLabelRes("sticker"))
        assertEquals(R.string.dialog_reply_sticker, replyMediaLabelRes("sticker_animated"))
        assertEquals(R.string.dialog_reply_gif, replyMediaLabelRes("gif"))
        assertEquals(R.string.dialog_reply_document, replyMediaLabelRes("document"))
    }

    @Test
    fun unknownAndMissingKindsFallBackToTheGenericLabel() {
        assertEquals(R.string.dialog_replying, replyMediaLabelRes(null))
        assertEquals(R.string.dialog_replying, replyMediaLabelRes(""))
        assertEquals(R.string.dialog_replying, replyMediaLabelRes("contact"))
    }
}