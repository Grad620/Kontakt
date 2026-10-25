package app.kontakt.client

import app.kontakt.client.auth.AuthFailureKind
import app.kontakt.client.auth.describeAuthFailure
import app.kontakt.client.auth.sanitizeDiagnostic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFailureTest {

    @Test
    fun cancelIsNeutralAndNotAnError() {
        val info = describeAuthFailure(AuthFailureKind.CANCELED, "User returns to auth activity without auth")

        assertTrue(info.cancelled)
        assertTrue(info.message.startsWith("Вход не завершён"))
        // Для отмены техническое описание не показываем.
        assertFalse("User returns" in info.message)
    }

    @Test
    fun everyNonCancelKindIsNotCancelled() {
        AuthFailureKind.entries.filter { it != AuthFailureKind.CANCELED }.forEach { kind ->
            assertFalse(kind.name, describeAuthFailure(kind, "x").cancelled)
        }
    }

    @Test
    fun apiCallFailureKeepsSafeDescription() {
        val info = describeAuthFailure(AuthFailureKind.API_CALL, "invalid_client")

        assertEquals("VK не принял запрос на вход (обмен кода на токен): invalid_client", info.message)
    }

    @Test
    fun emptyDescriptionDoesNotLeaveDanglingColon() {
        assertEquals("VK вернул ошибку авторизации", describeAuthFailure(AuthFailureKind.OAUTH, null).message)
        assertEquals("VK вернул ошибку авторизации", describeAuthFailure(AuthFailureKind.OAUTH, "  ").message)
    }

    @Test
    fun noBrowserMessageNamesTheRealProblem() {
        assertTrue("браузер" in describeAuthFailure(AuthFailureKind.NO_BROWSER, null).message)
    }

    @Test
    fun urlParametersWithSecretsAreHidden() {
        val raw = "redirect failed vk54842124://vk.ru/blank.html?code=SECRETCODE123&state=abc&device_id=DEV999&x=1"
        val clean = sanitizeDiagnostic(raw)

        assertFalse(clean, "SECRETCODE123" in clean)
        assertFalse(clean, "DEV999" in clean)
        assertFalse(clean, "state=abc" in clean)
        assertTrue(clean, "code=<скрыто>" in clean)
    }

    @Test
    fun jsonSecretsAreHidden() {
        val clean = sanitizeDiagnostic("""{"access_token":"abcdef","error":"invalid_request","refresh_token":"zzz"}""")

        assertFalse(clean, "abcdef" in clean)
        assertFalse(clean, "zzz" in clean)
        assertTrue(clean, "invalid_request" in clean)
    }

    @Test
    fun longTokenLikeValuesAreHidden() {
        val token = "vk1.a.AbCdEfGhIjKlMnOpQrStUvWxYz0123456789"
        val clean = sanitizeDiagnostic("bad response for $token end")

        assertFalse(clean, token in clean)
        assertTrue(clean, "end" in clean)
    }

    @Test
    fun ordinaryShortTextIsKept() {
        assertEquals("Security Error", sanitizeDiagnostic("Security Error"))
    }

    @Test
    fun whitespaceIsCollapsedAndLengthIsLimited() {
        assertEquals("a b c", sanitizeDiagnostic("a \n  b\t c"))
        val long = sanitizeDiagnostic("word ".repeat(100), maxLength = 20)
        assertEquals(21, long.length)
        assertTrue(long.endsWith("…"))
    }

    @Test
    fun nullAndBlankBecomeEmpty() {
        assertEquals("", sanitizeDiagnostic(null))
        assertEquals("", sanitizeDiagnostic("   "))
    }
}
