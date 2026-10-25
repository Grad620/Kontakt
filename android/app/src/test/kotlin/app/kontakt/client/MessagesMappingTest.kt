package app.kontakt.client

import app.kontakt.client.data.toMessagePage
import app.kontakt.client.net.MessageHistoryResponse
import app.kontakt.client.net.VkMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesMappingTest {

    private fun message(id: Long, out: Int = 0, text: String = "m$id", from: Long = 7) =
        VkMessage(id = id, date = 1_000 + id, fromId = from, text = text, out = out)

    @Test
    fun keepsVkOrderNewestFirstAndMapsFields() {
        val response = MessageHistoryResponse(
            count = 3,
            items = listOf(message(30, out = 1, from = 1), message(20, text = "привет"), message(10)),
        )
        val page = response.toMessagePage(offset = 0)

        assertEquals(listOf(30L, 20L, 10L), page.items.map { it.id })
        assertTrue(page.items[0].outgoing)
        assertFalse(page.items[1].outgoing)
        assertEquals("привет", page.items[1].text)
        assertEquals(1_020L, page.items[1].date)
        assertEquals(1L, page.items[0].fromId)
    }

    @Test
    fun nextOffsetAdvancesByReceivedItemsWhileMoreMessagesExist() {
        val response = MessageHistoryResponse(count = 100, items = (1L..40L).map { message(it) })

        assertEquals(40, response.toMessagePage(offset = 0).next)
        assertEquals(80, response.toMessagePage(offset = 40).next)
    }

    @Test
    fun lastPageHasNoNextOffset() {
        val response = MessageHistoryResponse(count = 85, items = (1L..5L).map { message(it) })

        assertNull(response.toMessagePage(offset = 80).next)
    }

    @Test
    fun exactlyFullHistoryHasNoNextOffset() {
        val response = MessageHistoryResponse(count = 40, items = (1L..40L).map { message(it) })

        assertNull(response.toMessagePage(offset = 0).next)
    }

    @Test
    fun emptyPageStopsPagingEvenWhenCountClaimsMore() {
        val response = MessageHistoryResponse(count = 500, items = emptyList())
        val page = response.toMessagePage(offset = 120)

        assertTrue(page.items.isEmpty())
        assertNull(page.next)
    }

    @Test
    fun blankTextIsKeptSoTheScreenCanLabelAttachments() {
        val page = MessageHistoryResponse(count = 1, items = listOf(message(1, text = ""))).toMessagePage(0)

        assertEquals("", page.items.single().text)
    }
}
