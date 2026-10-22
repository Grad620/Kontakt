package app.kontakt.client

import app.kontakt.client.core.Page
import app.kontakt.client.core.Paginator
import app.kontakt.client.core.UiError
import app.kontakt.client.net.VkError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginatorTest {

    @Test
    fun refreshThenLoadMoreAppendsAndStopsAtEnd() = runTest {
        val pages = mapOf<Int?, Page<Int, Int>>(
            null to Page(listOf(1, 2), 2),
            2 to Page(listOf(3), null),
        )
        val pager = Paginator<Int, Int>(this, { it }, "denied") { cursor -> pages.getValue(cursor) }

        pager.refresh()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), pager.state.value.items)
        assertFalse(pager.state.value.endReached)

        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), pager.state.value.items)
        assertTrue(pager.state.value.endReached)
    }

    @Test
    fun duplicateItemsAreNotAdded() = runTest {
        val pages = mapOf<Int?, Page<Int, Int>>(
            null to Page(listOf(1, 2), 2),
            2 to Page(listOf(2, 3), null),
        )
        val pager = Paginator<Int, Int>(this, { it }, "denied") { cursor -> pages.getValue(cursor) }
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), pager.state.value.items)
    }

    @Test
    fun accessDeniedBecomesFullScreenErrorWithCustomText() = runTest {
        val pager = Paginator<Int, Int>(this, { it }, "нет доступа") { throw VkError.AccessDenied(15, "x") }
        pager.refresh()
        advanceUntilIdle()
        val error = pager.state.value.error
        assertNotNull(error)
        assertEquals(UiError.Kind.AccessDenied, error?.kind)
        assertEquals("нет доступа", error?.message)
    }

    @Test
    fun failureAfterDataKeepsItemsAndShowsInlineError() = runTest {
        var calls = 0
        val pager = Paginator<Int, Int>(this, { it }, "denied") { _ ->
            calls++
            if (calls == 1) Page(listOf(1), 1) else throw VkError.Server(10, "boom")
        }
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1), pager.state.value.items)
        assertNull(pager.state.value.error)
        assertNotNull(pager.state.value.inlineError)
    }
}
