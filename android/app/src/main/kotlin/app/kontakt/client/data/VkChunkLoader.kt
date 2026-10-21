package app.kontakt.client.data

import app.kontakt.client.net.FeedResponse
import app.kontakt.client.net.VkApi
import app.kontakt.client.net.VkResponseParser
import app.kontakt.client.net.WallResponse
import app.kontakt.client.rec.FilterConfig

/**
 * Настоящая загрузка кандидатов из VK API:
 *  - Подписки: `newsfeed.get` (нужны права wall и friends — их VK выдаёт только по запросу);
 *  - Поиск по темам: `newsfeed.search` (токен пользователя или сервисный);
 *  - Стены сообществ: `wall.get`.
 * Доступность последних двух методов для токена без расширенных прав не проверена; если VK откажет,
 * источник отключится, а причина будет показана пользователю.
 */
class VkChunkLoader(
    private val api: VkApi,
    private val filterConfig: () -> FilterConfig,
) : ChunkLoader {

    override suspend fun load(spec: SourceSpec, cursor: String?): Chunk = when (spec.kind) {
        PostSource.Subscriptions -> loadFeed(cursor, FOR_YOU_FEED_COUNT)
        PostSource.Search -> loadSearch(spec, cursor)
        PostSource.Community -> loadWall(spec, cursor)
    }

    suspend fun loadFeed(cursor: String?, count: Int): Chunk {
        val params = mutableMapOf(
            "filters" to "post",
            "count" to count.toString(),
            "fields" to "photo_100",
            "max_photos" to "4",
        )
        if (cursor != null) params["start_from"] = cursor
        val data = VkResponseParser.decode<FeedResponse>(api.call("newsfeed.get", params))
        val raws = data.items.filter { it.type == "post" }.map { FeedMapping.raw(it) }
        val batch = FeedMapping.map(raws, data.profiles, data.groups, PostSource.Subscriptions, null, filterConfig())
        return Chunk(batch.posts, batch.dropped, data.nextFrom?.takeIf { it.isNotBlank() })
    }

    private suspend fun loadSearch(spec: SourceSpec, cursor: String?): Chunk {
        val params = mutableMapOf(
            "q" to spec.topic.orEmpty(),
            "extended" to "1",
            "count" to SEARCH_COUNT.toString(),
            "fields" to "photo_100",
        )
        if (cursor != null) params["start_from"] = cursor
        val data = VkResponseParser.decode<WallResponse>(api.call("newsfeed.search", params))
        val raws = data.items.mapNotNull { FeedMapping.raw(it) }
        val batch = FeedMapping.map(raws, data.profiles, data.groups, PostSource.Search, spec.topic, filterConfig())
        return Chunk(batch.posts, batch.dropped, data.nextFrom?.takeIf { it.isNotBlank() })
    }

    private suspend fun loadWall(spec: SourceSpec, cursor: String?): Chunk {
        val offset = cursor?.toIntOrNull() ?: 0
        val params = mapOf(
            "owner_id" to (-spec.communityId).toString(),
            "count" to WALL_COUNT.toString(),
            "offset" to offset.toString(),
            "filter" to "owner",
            "extended" to "1",
            "fields" to "photo_100",
        )
        val data = VkResponseParser.decode<WallResponse>(api.call("wall.get", params))
        val raws = data.items.mapNotNull { FeedMapping.raw(it) }
        val batch = FeedMapping.map(raws, data.profiles, data.groups, PostSource.Community, null, filterConfig())
        val nextOffset = offset + data.items.size
        val next = if (data.items.isEmpty() || nextOffset >= data.count) null else nextOffset.toString()
        return Chunk(batch.posts, batch.dropped, next)
    }

    private companion object {
        const val FOR_YOU_FEED_COUNT = 40
        const val SEARCH_COUNT = 30
        const val WALL_COUNT = 15
    }
}
