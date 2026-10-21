package app.vgradesk.client.data

import app.vgradesk.client.core.Page
import app.vgradesk.client.net.VkError
import app.vgradesk.client.rec.Recommender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Один источник кандидатов: лента подписок, поиск по теме или стена сообщества. */
data class SourceSpec(val kind: PostSource, val topic: String? = null, val communityId: Long = 0L)

/** Порция публикаций от источника, уже прошедшая фильтр. [next] — курсор следующей порции или null. */
data class Chunk(val posts: List<FeedPost>, val dropped: FilterCounts, val next: String?)

fun interface ChunkLoader {
    suspend fun load(spec: SourceSpec, cursor: String?): Chunk
}

/**
 * Лента «Для вас»: собирает кандидатов из нескольких источников VK, ранжирует их собственным алгоритмом
 * ([Recommender]) и отдаёт страницами. Если VK не выдал приложению доступ к методу, источник отключается,
 * а пользователю показывается причина. Лимиты VK не обходятся: источники обращаются к API по очереди,
 * с паузами ([app.vgradesk.client.net.VkApi]).
 */
class ForYouRepository(
    private val loader: ChunkLoader,
    private val recommender: Recommender,
    private val stats: FilterStats,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val pageSize: Int = 20,
) {
    enum class SourceState { Unknown, Ok, Denied, Failed }

    private class Source(val spec: SourceSpec) {
        var cursor: String? = null
        var exhausted = false
    }

    private val mutex = Mutex()
    private val pool = LinkedHashMap<String, FeedPost>()
    private val sources = ArrayList<Source>()
    private val deniedAt = HashMap<PostSource, Long>()
    private val deniedErrors = HashMap<PostSource, VkError.AccessDenied>()

    private val _statuses = MutableStateFlow<Map<PostSource, SourceState>>(emptyMap())
    val statuses: StateFlow<Map<PostSource, SourceState>> = _statuses.asStateFlow()

    /** [round] == null — новая подборка (обновление), иначе — следующая страница. */
    suspend fun page(round: Int?): Page<Int, FeedPost> = mutex.withLock {
        if (round == null) startSession()

        var picked: List<FeedPost> = emptyList()
        var failure: VkError? = null
        for (attempt in 0 until MAX_ATTEMPTS) {
            failure = fillPool() ?: failure
            picked = withContext(Dispatchers.Default) { recommender.rank(pool.values.toList(), pageSize) }
            picked.forEach { pool.remove(it.key) }
            pool.values.removeAll { !recommender.isAllowed(it) }
            if (picked.isNotEmpty() || (pool.isEmpty() && activeSources().isEmpty())) break
        }

        if (picked.isEmpty()) {
            val allDenied = sources.isNotEmpty() && sources.all { isDenied(it.spec.kind) }
            val error = failure ?: if (allDenied) deniedErrors.values.firstOrNull() else null
            if (error != null) throw error
            return@withLock Page(emptyList(), null)
        }
        val more = pool.isNotEmpty() || activeSources().isNotEmpty()
        Page(picked, if (more) (round ?: 0) + 1 else null)
    }

    private fun startSession() {
        pool.clear()
        sources.clear()
        sources += Source(SourceSpec(PostSource.Subscriptions))
        recommender.searchQueries(SEARCH_QUERIES).forEach { sources += Source(SourceSpec(PostSource.Search, topic = it)) }
        recommender.favoriteCommunities(FAVORITE_COMMUNITIES).forEach {
            sources += Source(SourceSpec(PostSource.Community, communityId = it))
        }
    }

    private fun isDenied(kind: PostSource): Boolean {
        val at = deniedAt[kind] ?: return false
        if (clock() - at >= DENIED_RETRY_MS) {
            deniedAt.remove(kind)
            deniedErrors.remove(kind)
            return false
        }
        return true
    }

    private fun activeSources(): List<Source> = sources.filter { !it.exhausted && !isDenied(it.spec.kind) }

    private fun setStatus(kind: PostSource, state: SourceState) {
        _statuses.update { it + (kind to state) }
    }

    /** Возвращает первую ошибку, если она остановила загрузку; иначе null. */
    private suspend fun fillPool(): VkError? {
        var firstError: VkError? = null
        var rounds = 0
        while (pool.size < pageSize * 2 && rounds < MAX_FILL_ROUNDS) {
            val active = activeSources()
            if (active.isEmpty()) break
            for (source in active) {
                try {
                    val chunk = loader.load(source.spec, source.cursor)
                    stats.add(chunk.dropped)
                    accept(chunk.posts)
                    source.cursor = chunk.next
                    source.exhausted = chunk.next.isNullOrBlank()
                    setStatus(source.spec.kind, SourceState.Ok)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: VkError.SessionExpired) {
                    throw e
                } catch (e: VkError.AccessDenied) {
                    deniedAt[source.spec.kind] = clock()
                    deniedErrors[source.spec.kind] = e
                    setStatus(source.spec.kind, SourceState.Denied)
                    firstError = firstError ?: e
                } catch (e: VkError.Network) {
                    setStatus(source.spec.kind, SourceState.Failed)
                    return firstError ?: e
                } catch (e: VkError.RateLimited) {
                    setStatus(source.spec.kind, SourceState.Failed)
                    return firstError ?: e
                } catch (e: VkError.Captcha) {
                    setStatus(source.spec.kind, SourceState.Failed)
                    return firstError ?: e
                } catch (e: VkError) {
                    source.exhausted = true
                    setStatus(source.spec.kind, SourceState.Failed)
                    firstError = firstError ?: e
                }
            }
            rounds++
        }
        return firstError
    }

    private fun accept(posts: List<FeedPost>) {
        for (post in posts) {
            if (post.key in pool) continue
            if (recommender.isSeen(post.key) || recommender.isHidden(post.authorId)) continue
            pool[post.key] = post
        }
    }

    private companion object {
        const val SEARCH_QUERIES = 3
        const val FAVORITE_COMMUNITIES = 2
        const val MAX_FILL_ROUNDS = 2
        const val MAX_ATTEMPTS = 3
        const val DENIED_RETRY_MS = 10 * 60 * 1000L
    }
}
