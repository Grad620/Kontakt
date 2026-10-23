package app.kontakt.client.data

import app.kontakt.client.core.Page
import app.kontakt.client.net.MessageHistoryResponse
import app.kontakt.client.net.VkApi
import app.kontakt.client.net.VkResponseParser

data class MessageItem(
    val id: Long,
    val fromId: Long,
    val text: String,
    val date: Long,
    val outgoing: Boolean,
)

/** Репозиторий истории: сетевой DTO отделён от модели, которую использует UI. */
class MessagesRepository(private val api: VkApi) {
    suspend fun history(peerId: Long, offset: Int): Page<Int, MessageItem> {
        val response = api.call(
            "messages.getHistory",
            mapOf(
                "peer_id" to peerId.toString(),
                "offset" to offset.toString(),
                "count" to PAGE_SIZE.toString(),
                "rev" to "1",
            ),
        )
        val data = VkResponseParser.decode<MessageHistoryResponse>(response)
        val items = data.items.map { message ->
            MessageItem(
                id = message.id,
                fromId = message.fromId,
                text = message.text,
                date = message.date,
                outgoing = message.out == 1,
            )
        }
        val nextOffset = offset + items.size
        val next = nextOffset.takeIf { items.isNotEmpty() && it < data.count }
        return Page(items, next)
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
