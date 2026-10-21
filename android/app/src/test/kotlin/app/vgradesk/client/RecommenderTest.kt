package app.vgradesk.client

import app.vgradesk.client.data.PostSource
import app.vgradesk.client.rec.Recommender
import app.vgradesk.client.rec.Signal
import app.vgradesk.client.rec.SignalKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderTest {

    private fun newRecommender(storage: MemoryStorage = MemoryStorage()): Recommender =
        Recommender(storage, clock = { NOW_MS }).also { it.load() }

    @Test
    fun coldStartUsesBroadTopicsAndDoesNotPretendToKnowInterests() {
        val rec = newRecommender()
        val queries = rec.searchQueries(3)
        assertEquals(3, queries.size)
        assertEquals(3, queries.toSet().size)
        assertTrue(queries.all { it in Recommender.COLD_TOPICS })
        assertEquals(0, rec.summary.value.signals)
        assertTrue(rec.summary.value.topInterests.isEmpty())
    }

    @Test
    fun searchQueriesAdaptToWhatTheUserReallyReads() {
        val rec = newRecommender()
        listOf(
            post("1", 1, "поход выходные палатка"),
            post("2", 2, "поход рюкзак маршрут"),
            post("3", 3, "поход костёр озеро"),
        ).forEach { rec.onSignal(Signal(SignalKind.DWELL, it, 12.0)) }

        val queries = rec.searchQueries(3)
        assertEquals("поход", queries.first())
        assertEquals(3, queries.size)
        assertTrue("одна тема всегда «чужая» для разнообразия", queries.drop(1).all { it in Recommender.COLD_TOPICS })
        assertTrue(rec.summary.value.topInterests.contains("поход"))
    }

    @Test
    fun interestedSignalRaisesSimilarPostsAboveUnrelated() {
        val rec = newRecommender()
        rec.onSignal(Signal(SignalKind.INTERESTED, post("seed", 1, "поход палатка озеро рюкзак")))
        val similar = post("a", 2, "поход палатка костёр")
        val unrelated = post("b", 3, "налоги бухгалтерия отчётность")
        val ranked = rec.rank(listOf(unrelated, similar), 2)
        assertEquals(listOf("a", "b"), ranked.map { it.key })
    }

    @Test
    fun notInterestedTwiceStopsShowingThatAuthor() {
        val rec = newRecommender()
        val p = post("x", 7, "реклама курсов")
        rec.onSignal(Signal(SignalKind.NOT_INTERESTED, p))
        assertEquals(1, rec.rank(listOf(post("y", 7, "другой текст")), 5).size)
        rec.onSignal(Signal(SignalKind.NOT_INTERESTED, p))
        assertTrue(rec.rank(listOf(post("y", 7, "другой текст")), 5).isEmpty())
    }

    @Test
    fun hiddenSourceIsExcludedUntilUnhiddenAndSurvivesReset() {
        val rec = newRecommender()
        val p = post("a", 5, "текст")
        rec.hideSource(5, "Автор 5")
        assertTrue(rec.rank(listOf(p), 5).isEmpty())
        assertTrue(rec.isHidden(5))
        assertEquals(listOf(5L), rec.summary.value.hiddenSources.map { it.id })

        rec.onSignal(Signal(SignalKind.INTERESTED, post("b", 6, "поход")))
        rec.reset()
        assertEquals(0, rec.summary.value.signals)
        assertTrue(rec.isHidden(5))

        rec.unhideSource(5)
        assertEquals(1, rec.rank(listOf(p), 5).size)
    }

    @Test
    fun wipeStorageRemovesEverythingIncludingHiddenSources() {
        val storage = MemoryStorage()
        val rec = newRecommender(storage)
        rec.hideSource(5, "Автор")
        rec.onSignal(Signal(SignalKind.INTERESTED, post("b", 6, "поход")))
        rec.flush()
        rec.wipeStorage()
        assertTrue(storage.deleted)
        assertFalse(rec.isHidden(5))
        assertEquals(0, rec.summary.value.signals)
    }

    @Test
    fun learningSurvivesRestart() {
        val storage = MemoryStorage()
        val first = newRecommender(storage)
        first.onSignal(Signal(SignalKind.INTERESTED, post("k1", 1, "поход палатка озеро")))
        first.hideSource(9, "Скрытый")
        first.flush()
        assertEquals(1, storage.writes)
        first.flush()
        assertEquals("без изменений ничего не пишем", 1, storage.writes)

        val second = newRecommender(storage)
        assertEquals(1, second.summary.value.signals)
        assertEquals(setOf("k1"), second.interested.value)
        assertTrue(second.isSeen("k1"))
        assertTrue(second.isHidden(9))
        assertTrue(second.summary.value.topInterests.contains("поход"))
    }

    @Test
    fun onChangedIsCalledOnSignals() {
        val rec = newRecommender()
        var calls = 0
        rec.onChanged = { calls++ }
        rec.onSignal(Signal(SignalKind.QUICK_SKIP, post("a", 1, "текст")))
        rec.markSeen(post("b", 2, "текст"))
        assertEquals(2, calls)
    }

    @Test
    fun interestedSetFollowsToggle() {
        val rec = newRecommender()
        val p = post("a", 1, "поход")
        rec.onSignal(Signal(SignalKind.INTERESTED, p))
        assertEquals(setOf("a"), rec.interested.value)
        rec.onSignal(Signal(SignalKind.INTEREST_REMOVED, p))
        assertTrue(rec.interested.value.isEmpty())
    }

    @Test
    fun alreadySeenPostsSinkBelowFreshOnes() {
        val rec = newRecommender()
        val seen = post("seen", 1, "одинаковый текст публикации")
        val fresh = post("fresh", 2, "одинаковый текст публикации")
        rec.markSeen(seen)
        assertEquals(listOf("fresh", "seen"), rec.rank(listOf(seen, fresh), 2).map { it.key })
    }

    @Test
    fun oneAuthorDoesNotFloodTheTopOfTheFeed() {
        val rec = newRecommender()
        val flood = (1..6).map { post("f$it", 1, "тема$it слово$it уникальное$it", likes = 500) }
        val others = listOf(post("o1", 2, "совсем другое про кино"), post("o2", 3, "рецепт борща и хлеба"))
        val ranked = rec.rank(flood + others, 4)
        assertEquals(4, ranked.size)
        assertTrue(ranked.count { it.authorId == 1L } <= 2)
    }

    @Test
    fun seenListIsBounded() {
        val rec = newRecommender()
        for (i in 0 until 900) rec.markSeen(post("k$i", 1L + i % 5, "текст"))
        assertFalse(rec.isSeen("k0"))
        assertTrue(rec.isSeen("k899"))
    }

    @Test
    fun explanationUsesRealReasonsOnly() {
        val rec = newRecommender()
        val cold = post("a", 1, "поход палатка").copy(source = PostSource.Search, topic = "кино")
        val coldText = rec.explain(cold, NOW_MS / 1000).joinToString(" ")
        assertTrue(coldText.contains("кино"))
        assertTrue(coldText.contains("пока не знает"))
        assertFalse(coldText.contains("Совпадает с вашими интересами"))

        listOf(
            post("1", 1, "поход выходные палатка"),
            post("2", 2, "поход рюкзак маршрут"),
            post("3", 3, "поход костёр озеро"),
        ).forEach { rec.onSignal(Signal(SignalKind.DWELL, it, 12.0)) }
        val learned = rec.explain(post("n", 9, "новый поход в горы"), NOW_MS / 1000).joinToString(" ")
        assertTrue(learned.contains("Совпадает с вашими интересами: поход"))
        assertFalse(learned.contains("пока не знает"))
    }

    @Test
    fun withoutHiddenOnlyRemovesHiddenAuthors() {
        val rec = newRecommender()
        rec.hideSource(2, "Автор 2")
        val result = rec.withoutHidden(listOf(post("a", 1, "x"), post("b", 2, "y")))
        assertEquals(listOf("a"), result.map { it.key })
    }
}
