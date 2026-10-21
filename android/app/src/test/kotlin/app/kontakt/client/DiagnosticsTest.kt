package app.kontakt.client

import app.kontakt.client.core.DiagnosticsLog
import app.kontakt.client.net.VkError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiagnosticsTest {
    @Test
    fun logKeepsNewestFirstAndIsBounded() {
        var t = 0L
        val log = DiagnosticsLog(capacity = 3, clock = { ++t })
        log.record("a", VkError.RateLimited())
        log.record("b", VkError.AccessDenied(15, "секретный текст"))
        log.record("c", VkError.Server(10, "x"))
        log.record("d", VkError.Captcha())
        val events = log.events.value
        assertEquals(listOf("d", "c", "b"), events.map { it.method })
        assertEquals(15, events[2].code)
    }

    @Test
    fun profileTypeUnavailableIsRecordedAs1051() {
        val log = DiagnosticsLog()
        log.record("users.get", VkError.ProfileTypeUnavailable("Method is not available for this profile type"))
        assertEquals("users.get", log.events.value.single().method)
        assertEquals(1051, log.events.value.single().code)
        assertEquals("метод недоступен для типа профиля", log.events.value.single().kind)
    }

    @Test
    fun entriesNeverContainVkMessageText() {
        val log = DiagnosticsLog()
        log.record("messages.getConversations", VkError.AccessDenied(15, "токен abc123 текст письма"))
        val text = log.events.value.joinToString { "${it.method} ${it.kind} ${it.code}" }
        assertFalse(text.contains("abc123"))
        assertFalse(text.contains("письма"))
    }
}
