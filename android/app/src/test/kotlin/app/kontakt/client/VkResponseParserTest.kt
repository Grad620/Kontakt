package app.kontakt.client

import app.kontakt.client.net.ConversationsResponse
import app.kontakt.client.net.FeedResponse
import app.kontakt.client.net.MessageHistoryResponse
import app.kontakt.client.net.VkError
import app.kontakt.client.net.VkResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VkResponseParserTest {

    @Test
    fun errorCode5MeansSessionExpired() {
        assertThrows(VkError.SessionExpired::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":5,"error_msg":"User authorization failed"}}""")
        }
    }

    @Test
    fun errorCode15MeansAccessDenied() {
        val error = assertThrows(VkError.AccessDenied::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":15,"error_msg":"Access denied"}}""")
        }
        assertEquals(15, error.code)
    }

    @Test
    fun errorCode1051MeansUnsupportedProfileType() {
        assertThrows(VkError.ProfileTypeUnavailable::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":1051,"error_msg":"Method is not available for this profile type"}}""")
        }
    }

    @Test
    fun profileTypeErrorCanCarryTheMethodName() {
        val error = VkError.ProfileTypeUnavailable("Method is not available for this profile type", "messages.getConversations")
        assertEquals("messages.getConversations", error.method)
    }

    @Test
    fun rateLimitCodesAreMappedWithoutAutomaticRetry() {
        assertThrows(VkError.RateLimited::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":6,"error_msg":"Too many requests"}}""")
        }
        assertThrows(VkError.RateLimited::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":9,"error_msg":"Flood control"}}""")
        }
    }

    @Test
    fun unknownApiErrorPreservesCodeAndMessage() {
        val error = assertThrows(VkError.Server::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":999,"error_msg":"Unexpected error"}}""")
        }
        assertEquals(999, error.code)
        assertEquals("Unexpected error", error.vkMessage)
    }

    @Test
    fun missingResponseAndErrorIsMalformed() {
        assertThrows(VkError.Malformed::class.java) {
            VkResponseParser.parse("""{"items":[]}""")
        }
    }

    @Test
    fun captchaIsReportedNotBypassed() {
        assertThrows(VkError.Captcha::class.java) {
            VkResponseParser.parse("""{"error":{"error_code":14,"error_msg":"Captcha needed"}}""")
        }
    }

    @Test
    fun garbageIsMalformed() {
        assertThrows(VkError.Malformed::class.java) { VkResponseParser.parse("<html>502</html>") }
    }

    @Test
    fun conversationsDecode() {
        val json = """
            {"response":{"count":1,"items":[{"conversation":{"peer":{"id":7,"type":"user"},"unread_count":2},
            "last_message":{"id":5,"date":10,"from_id":7,"text":"привет","out":0}}],
            "profiles":[{"id":7,"first_name":"Анна","last_name":"Иванова"}],"groups":[]}}
        """.trimIndent()
        val data = VkResponseParser.decode<ConversationsResponse>(VkResponseParser.parse(json))
        assertEquals(1, data.items.size)
        assertEquals(2, data.items[0].conversation.unreadCount)
        assertEquals("Анна Иванова", data.profiles[0].fullName)
    }

    @Test
    fun feedDecodeKeepsNextFromAndSkipsUnknownFields() {
        val json = """
            {"response":{"items":[{"type":"post","source_id":-1,"date":1,"post_id":9,"text":"t","unknown":42,
            "likes":{"count":3},"comments":{"count":1},"marked_as_ads":0}],"profiles":[],"groups":[],"next_from":"abc"}}
        """.trimIndent()
        val data = VkResponseParser.decode<FeedResponse>(VkResponseParser.parse(json))
        assertEquals("abc", data.nextFrom)
        assertEquals(3, data.items[0].likes?.count)
    }

    @Test
    fun messageHistoryDecodeKeepsOrderOutgoingFlagAndSkipsUnknownFields() {
        val json = """
            {"response":{"count":2,"items":[
            {"id":12,"date":200,"from_id":1,"text":"второе","out":1,"peer_id":7,"random_id":0,"attachments":[]},
            {"id":11,"date":100,"from_id":7,"text":"","out":0,"peer_id":7,"fwd_messages":[]}]}}
        """.trimIndent()
        val data = VkResponseParser.decode<MessageHistoryResponse>(VkResponseParser.parse(json))
        assertEquals(2, data.count)
        assertEquals(listOf(12L, 11L), data.items.map { it.id })
        assertEquals(1, data.items[0].out)
        assertEquals("", data.items[1].text)
    }
}
