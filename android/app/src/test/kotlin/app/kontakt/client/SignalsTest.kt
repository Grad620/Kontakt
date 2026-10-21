package app.kontakt.client

import app.kontakt.client.rec.DwellTracker
import app.kontakt.client.rec.Signal
import app.kontakt.client.rec.SignalClassifier
import app.kontakt.client.rec.SignalKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalsTest {
    @Test
    fun dwellThresholds() {
        assertEquals(SignalKind.QUICK_SKIP, SignalClassifier.fromDwell(300))
        assertNull(SignalClassifier.fromDwell(2_000))
        assertEquals(SignalKind.DWELL, SignalClassifier.fromDwell(3_000))
    }

    @Test
    fun dwellValueGrowsWithTimeAndIsCapped() {
        val p = post("a", 1, "текст")
        val short = SignalClassifier.valueOf(Signal(SignalKind.DWELL, p, 3.0))
        val long = SignalClassifier.valueOf(Signal(SignalKind.DWELL, p, 15.0))
        val longer = SignalClassifier.valueOf(Signal(SignalKind.DWELL, p, 120.0))
        assertTrue(short < long)
        assertEquals(long, longer, 1e-9)
    }

    @Test
    fun explicitSignalsHaveFixedValues() {
        val p = post("a", 1, "текст")
        assertEquals(3.0, SignalClassifier.valueOf(Signal(SignalKind.INTERESTED, p)), 1e-9)
        assertEquals(-4.0, SignalClassifier.valueOf(Signal(SignalKind.NOT_INTERESTED, p)), 1e-9)
    }

    private class Clock(var now: Long = 0)

    @Test
    fun trackerMeasuresOnlyWhileActive() {
        val clock = Clock()
        val events = ArrayList<Pair<String, Long>>()
        val tracker = DwellTracker({ clock.now }, { key, ms -> events += key to ms })

        tracker.resume()
        tracker.onVisible(setOf("a", "b"))
        clock.now = 5_000
        tracker.onVisible(setOf("b"))                 // a ушла с экрана
        assertEquals(listOf("a" to 5_000L), events)

        clock.now = 7_000
        tracker.pause()                               // приложение свернули
        assertEquals(listOf("a" to 5_000L, "b" to 7_000L), events)

        clock.now = 60_000                            // время в фоне не считается
        tracker.resume()
        clock.now = 61_000
        tracker.pause()
        assertEquals("b" to 1_000L, events.last())
    }

    @Test
    fun visibleKeysBeforeResumeStartCountingOnResume() {
        val clock = Clock()
        val events = ArrayList<Pair<String, Long>>()
        val tracker = DwellTracker({ clock.now }, { key, ms -> events += key to ms })
        tracker.onVisible(setOf("x"))
        clock.now = 10_000
        tracker.resume()
        clock.now = 12_500
        tracker.pause()
        assertEquals(listOf("x" to 2_500L), events)
    }
}
