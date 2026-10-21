package app.vgradesk.client

import app.vgradesk.client.data.Chunk
import app.vgradesk.client.data.ChunkLoader
import app.vgradesk.client.data.FeedPost
import app.vgradesk.client.data.FilterCounts
import app.vgradesk.client.data.FilterStats
import app.vgradesk.client.data.ForYouRepository
import app.vgradesk.client.data.PostSource
import app.vgradesk.client.data.SourceSpec
import app.vgradesk.client.net.VkError
import app.vgradesk.client.rec.Recommender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ForYouRepositoryTest {

    private class FakeLoader(val handler: (SourceSpec, String?) -> Chunk) : ChunkLoader {
        val calls = ArrayList<SourceSpec>()
        override suspend fun load(spec: SourceSpec, cursor: String?): Chunk {
            calls += spec
            return handler(spec, cursor)
        }
    }

    private fun chunk(spec: SourceSpec, prefix: String, count: Int, dropped: FilterCounts = FilterCounts(), author: (Int) -> Long) =
        Chunk(
            posts = (1..count).map { post("$prefix-$it", author(it), "запись $prefix номер $it").copy(source = spec.kind) },
            dropped = dropped,
            next = null,
        )

    private fun repo(loader: ChunkLoader, rec: Recommender = Recommender(MemoryStorage(), clock = { NOW_MS }).also { it.load() },
                     stats: FilterStats = FilterStats(), clock: () -> Long = { NOW_MS }) =
        ForYouRepository(loader, rec, stats, clock)

    @Test
    fun collectsFromAllSourcesPagesWithoutDuplicates() = runBlocking {
        val loader = FakeLoader { spec, _ ->
            when (spec.kind) {
                PostSource.Subscriptions -> chunk(spec, "s", 25) { 1L + it % 5 }
                PostSource.Search -> chunk(spec, "q-${spec.topic}", 8) { 100L + it }
                PostSource.Community -> chunk(spec, "c-${spec.communityId}", 5) { -spec.communityId }
            }
        }
        val repo = repo(loader)
        val keys = ArrayList<String>()
        var round: Int? = null
        var pages = 0
        while (true) {
            val page = repo.page(round)
            keys += page.items.map { it.key }
            pages++
            round = page.next ?: break
            assertTrue("защита от бесконечного цикла", pages < 10)
        }
        assertEquals(25 + 3 * 8, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(1, loader.calls.count { it.kind == PostSource.Subscriptions })
        assertEquals(3, loader.calls.count { it.kind == PostSource.Search })
        assertEquals(ForYouRepository.SourceState.Ok, repo.statuses.value[PostSource.Subscriptions])
    }

    @Test
    fun postsAlreadySeenOrFromHiddenSourcesAreNotOffered() = runBlocking {
        val rec = Recommender(MemoryStorage(), clock = { NOW_MS }).also { it.load() }
        rec.hideSource(2, "Скрытый")
        rec.markSeen(post("s-1", 1, "x"))
        val loader = FakeLoader { spec, _ -> chunk(spec, "s", 6) { it.toLong() } }
        val keys = repo(loader, rec).page(null).items.map { it.key }.toSet()
        assertFalse("s-1" in keys)
        assertFalse("s-2" in keys)
        assertTrue("s-3" in keys)
    }

    @Test
    fun deniedMethodIsDisabledAndOthersKeepWorking() = runBlocking {
        var now = NOW_MS
        val loader = FakeLoader { spec, _ ->
            if (spec.kind == PostSource.Subscriptions) throw VkError.AccessDenied(15, "access denied")
            chunk(spec, "q-${spec.topic}", 5) { 100L + it }
        }
        val repo = repo(loader, clock = { now })
        val first = repo.page(null)
        assertTrue(first.items.isNotEmpty())
        assertEquals(ForYouRepository.SourceState.Denied, repo.statuses.value[PostSource.Subscriptions])
        assertEquals(ForYouRepository.SourceState.Ok, repo.statuses.value[PostSource.Search])
        assertEquals(1, loader.calls.count { it.kind == PostSource.Subscriptions })

        now += 60_000
        repo.page(null)
        assertEquals("запрещённый метод не дёргаем снова", 1, loader.calls.count { it.kind == PostSource.Subscriptions })

        now += 11 * 60_000
        repo.page(null)
        assertEquals("через 10 минут пробуем ещё раз", 2, loader.calls.count { it.kind == PostSource.Subscriptions })
    }

    @Test
    fun everySourceDeniedSurfacesAccessDeniedInsteadOfEmptyFeed() = runBlocking {
        val loader = FakeLoader { _, _ -> throw VkError.AccessDenied(15, "denied") }
        val repo = repo(loader)
        try {
            repo.page(null)
            fail("должна быть ошибка доступа")
        } catch (e: VkError.AccessDenied) {
            assertEquals(15, e.code)
        }
        // повторное открытие экрана тоже честно показывает причину, а не «пусто»
        try {
            repo.page(null)
            fail("должна быть ошибка доступа")
        } catch (e: VkError.AccessDenied) {
            assertTrue(true)
        }
    }

    @Test
    fun networkFailureWithNothingToShowIsReportedWithDataItIsHidden() = runBlocking {
        val failing = FakeLoader { _, _ -> throw VkError.Network(IOException("offline")) }
        try {
            repo(failing).page(null)
            fail("должна быть сетевая ошибка")
        } catch (e: VkError.Network) {
            assertTrue(true)
        }

        val partial = FakeLoader { spec, _ ->
            if (spec.kind == PostSource.Subscriptions) chunk(spec, "s", 5) { it.toLong() }
            else throw VkError.Network(IOException("offline"))
        }
        val repo = repo(partial)
        assertEquals(5, repo.page(null).items.size)
        assertEquals(ForYouRepository.SourceState.Failed, repo.statuses.value[PostSource.Search])
    }

    @Test
    fun sessionExpiredAlwaysPropagates() = runBlocking {
        val loader = FakeLoader { _, _ -> throw VkError.SessionExpired() }
        try {
            repo(loader).page(null)
            fail("должна быть ошибка сессии")
        } catch (e: VkError.SessionExpired) {
            assertTrue(true)
        }
    }

    @Test
    fun filterCountsAreAccumulated() = runBlocking {
        val stats = FilterStats()
        val loader = FakeLoader { spec, _ -> chunk(spec, "s", 3, FilterCounts(ads = 2, goods = 1)) { it.toLong() } }
        repo(loader, stats = stats).page(null)
        assertTrue(stats.counts.value.ads >= 2)
        assertTrue(stats.counts.value.goods >= 1)
    }

    @Test
    fun emptySourcesGiveEmptyPageNotError() = runBlocking {
        val loader = FakeLoader { _, _ -> Chunk(emptyList(), FilterCounts(), null) }
        val page = repo(loader).page(null)
        assertTrue(page.items.isEmpty())
        assertEquals(null, page.next)
    }
}
