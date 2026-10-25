package app.kontakt.client

import app.kontakt.client.auth.AuthFailureKind
import app.kontakt.client.auth.AuthFlow
import app.kontakt.client.auth.AuthState
import app.kontakt.client.auth.SessionSink
import app.kontakt.client.auth.describeAuthFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFlowTest {

    private class FakeSink(var failSave: Boolean = false) : SessionSink {
        var saved: String? = null
        var clears = 0
        override fun save(token: String) {
            if (failSave) throw IllegalStateException("keystore unavailable")
            saved = token
        }
        override fun clear() {
            saved = null
            clears++
        }
    }

    private fun message(flow: AuthFlow) = (flow.state.value as AuthState.SignedOut).message

    @Test
    fun startsSignedOutWithoutToken() {
        val flow = AuthFlow(FakeSink(), initialToken = null)

        assertTrue(flow.state.value is AuthState.SignedOut)
        assertNull(flow.token())
        assertFalse(flow.busy.value)
    }

    @Test
    fun startsSignedInWhenTokenWasStored() {
        val flow = AuthFlow(FakeSink(), initialToken = "stored")

        assertEquals(AuthState.SignedIn, flow.state.value)
        assertEquals("stored", flow.token())
    }

    @Test
    fun secondAttemptIsRefusedWhileFirstIsRunning() {
        val flow = AuthFlow(FakeSink(), null)

        assertTrue(flow.begin())
        assertTrue(flow.busy.value)
        assertFalse(flow.begin())
    }

    @Test
    fun successSavesTokenAndClearsBusy() {
        val sink = FakeSink()
        val flow = AuthFlow(sink, null)
        flow.begin()

        flow.succeeded("tok")

        assertEquals("tok", sink.saved)
        assertEquals("tok", flow.token())
        assertEquals(AuthState.SignedIn, flow.state.value)
        assertFalse(flow.busy.value)
    }

    @Test
    fun saveFailureDoesNotSignInAndDoesNotKeepToken() {
        val flow = AuthFlow(FakeSink(failSave = true), null)
        flow.begin()

        flow.succeeded("tok")

        assertNull(flow.token())
        assertEquals(AuthFlow.SAVE_FAILED_MESSAGE, message(flow))
        assertFalse(flow.busy.value)
    }

    @Test
    fun blankTokenIsRejectedAsSaveFailure() {
        val sink = FakeSink()
        val flow = AuthFlow(sink, null)
        flow.begin()

        flow.succeeded("  ")

        assertNull(sink.saved)
        assertNull(flow.token())
        assertFalse(flow.busy.value)
    }

    @Test
    fun cancelReturnsToSignedOutAndAllowsRetry() {
        val flow = AuthFlow(FakeSink(), null)
        flow.begin()

        flow.failed(describeAuthFailure(AuthFailureKind.CANCELED, null))

        assertFalse(flow.busy.value)
        assertTrue(message(flow)!!.startsWith("Вход не завершён"))
        assertTrue(flow.begin())
    }

    @Test
    fun errorReturnsToSignedOutAndAllowsRetry() {
        val flow = AuthFlow(FakeSink(), null)
        flow.begin()

        flow.failed(describeAuthFailure(AuthFailureKind.OAUTH, "invalid_request"))

        assertFalse(flow.busy.value)
        assertTrue("invalid_request" in message(flow)!!)
        assertTrue(flow.begin())
    }

    @Test
    fun newAttemptClearsStaleErrorMessage() {
        val flow = AuthFlow(FakeSink(), null)
        flow.reject("старая ошибка")

        flow.begin()

        assertNull(message(flow))
    }

    @Test
    fun rejectedAttemptDoesNotStayBusy() {
        val flow = AuthFlow(FakeSink(), null)
        flow.begin()

        flow.reject("не настроено")

        assertFalse(flow.busy.value)
        assertEquals("не настроено", message(flow))
    }

    @Test
    fun lostResultIsReleasedByAbandon() {
        val flow = AuthFlow(FakeSink(), null)
        flow.begin()

        flow.abandoned()

        assertFalse(flow.busy.value)
        assertEquals(AuthFlow.ABANDONED_MESSAGE, message(flow))
    }

    @Test
    fun abandonDoesNothingWhenNoAttemptIsRunning() {
        val flow = AuthFlow(FakeSink(), "stored")

        flow.abandoned()

        assertEquals(AuthState.SignedIn, flow.state.value)
    }

    @Test
    fun lateSuccessAfterAbandonStillSignsIn() {
        val flow = AuthFlow(FakeSink(), null)
        flow.begin()
        flow.abandoned()

        flow.succeeded("late")

        assertEquals(AuthState.SignedIn, flow.state.value)
        assertEquals("late", flow.token())
    }

    @Test
    fun expiredSessionClearsTokenAndExplainsWhy() {
        val sink = FakeSink()
        val flow = AuthFlow(sink, "tok")

        flow.expired()

        assertNull(flow.token())
        assertEquals(1, sink.clears)
        assertEquals("Сессия истекла. Войдите снова.", message(flow))
    }

    @Test
    fun expiredWhileSignedOutKeepsTheCurrentMessage() {
        val sink = FakeSink()
        val flow = AuthFlow(sink, null)
        flow.reject("своё сообщение")

        flow.expired()

        assertEquals(0, sink.clears)
        assertEquals("своё сообщение", message(flow))
    }

    @Test
    fun signOutWipesSessionEvenIfClearThrows() {
        val flow = AuthFlow(
            object : SessionSink {
                override fun save(token: String) = Unit
                override fun clear() = throw IllegalStateException("prefs unavailable")
            },
            "tok",
        )

        flow.signOut()

        assertNull(flow.token())
        assertTrue(flow.state.value is AuthState.SignedOut)
        assertFalse(flow.busy.value)
    }

    @Test
    fun tokenValueNeverAppearsInState() {
        val flow = AuthFlow(FakeSink(failSave = true), null)
        flow.begin()

        flow.succeeded("SUPER-SECRET-TOKEN")

        assertFalse("SUPER-SECRET-TOKEN" in flow.state.value.toString())
    }
}
