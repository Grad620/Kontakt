package app.kontakt.client.data

import app.kontakt.client.core.Page
import app.kontakt.client.net.ConversationsResponse
import app.kontakt.client.net.VkApi
import app.kontakt.client.net.VkError
import app.kontakt.client.net.VkMessage
import app.kontakt.client.net.VkResponseParser
import app.kontakt.client.net.VkUser
import app.kontakt.client.rec.Recommender

data class Profile(val id: Long, val name: String, val avatar: String?, val status: String?)

data class ChatItem(
    val peerId: Long,
    val title: String,
    val avatar: String?,
    val lastText: String,
    val time: Long,
    val unread: Int,
    val lastIsOutgoing: Boolean,
)

/** users.get — доступно с базовым доступом VK ID. */
class ProfileRepository(private val api: VkApi) {
    suspend fun load(): Profile {
        val response = api.call("users.get", mapOf("fields" to "photo_200,screen_name,status"))
        val users = VkResponseParser.decode<List<VkUser>>(response)
        val user = users.firstOrNull() ?: throw VkError.Malformed(null)
        return Profile(
            id = user.id,
            name = user.fullName.ifBlank { "Без имени" },
            avatar = user.photo200 ?: user.photo100,
            status = user.status?.takeIf { it.isNotBlank() },
        )
    }
}

/** messages.getConversations — для пользовательского токена нужно право messages (выдаётся VK по запросу). */
class ChatsRepository(private val api: VkApi) {
    suspend fun page(offset: Int): Page<Int, ChatItem> {
        val response = api.call(
            "messages.getConversations",
            mapOf(
                "offset" to offset.toString(),
                "count" to PAGE_SIZE.toString(),
                "extended" to "1",
                "fields" to "photo_100",
            ),
        )
        val data = VkResponseParser.decode<ConversationsResponse>(response)
        val users = data.profiles.associateBy { it.id }
        val groups = data.groups.associateBy { it.id }

        val items = data.items.map { item ->
            val peer = item.conversation.peer
            val last = item.lastMessage
            val settings = item.conversation.chatSettings
            val (title, avatar) = when (peer.type) {
                "user" -> {
                    val user = users[peer.id]
                    (user?.fullName?.ifBlank { null } ?: "Пользователь") to user?.photo100
                }
                "group" -> {
                    val group = groups[-peer.id]
                    (group?.name?.ifBlank { null } ?: "Сообщество") to group?.photo100
                }
                "chat" -> (settings?.title?.ifBlank { null } ?: "Беседа") to settings?.photo?.photo100
                else -> "Диалог" to null
            }
            ChatItem(
                peerId = peer.id,
                title = title,
                avatar = avatar,
                lastText = previewOf(last),
                time = last?.date ?: 0L,
                unread = item.conversation.unreadCount,
                lastIsOutgoing = last?.out == 1,
            )
        }
        val next = offset + data.items.size
        return Page(items, if (data.items.isNotEmpty() && next < data.count) next else null)
    }

    private fun previewOf(message: VkMessage?): String {
        if (message == null) return ""
        if (message.text.isNotBlank()) return message.text.replace('\n', ' ')
        return when (message.attachments.firstOrNull()?.type) {
            null -> ""
            "photo" -> "Фотография"
            "audio_message" -> "Голосовое сообщение"
            "sticker" -> "Стикер"
            "video" -> "Видео"
            "doc" -> "Документ"
            else -> "Вложение"
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

/**
 * Режим «Подписки»: обычная лента VK (`newsfeed.get`, нужны права wall и friends — VK выдаёт их по запросу).
 * Реклама и мусор отбрасываются по настройкам фильтра; скрытые источники не показываются.
 */
class FeedRepository(
    private val loader: VkChunkLoader,
    private val recommender: Recommender,
    private val stats: FilterStats,
) {
    suspend fun page(startFrom: String?): Page<String, FeedPost> {
        var cursor = startFrom
        repeat(MAX_EMPTY_PAGES) {
            val chunk = loader.loadFeed(cursor, PAGE_SIZE)
            stats.add(chunk.dropped)
            val posts = recommender.withoutHidden(chunk.posts)
            val next = chunk.next
            if (posts.isNotEmpty() || next == null) return Page(posts, next)
            cursor = next
        }
        // Три страницы подряд оказались целиком отфильтрованы: отдаём курсор, чтобы список мог догрузиться.
        return Page(emptyList(), cursor)
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_EMPTY_PAGES = 3
    }
}
