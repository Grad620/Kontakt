package app.kontakt.client.data

import app.kontakt.client.core.Page
import app.kontakt.client.net.MessageHistoryResponse
import app.kontakt.client.net.VkApi
import app.kontakt.client.net.VkResponseParser

/** Репозиторий истории: сетевой DTO отделён от модели, которую использует UI (см. [toMessagePage]). */
class MessagesRepository(private val api: VkApi) {
    suspend fun history(peerId: Long, offset: Int): Page<Int, MessageItem> {
        val response = api.call(
            "messages.getHistory",
            mapOf(
                "peer_id" to peerId.toString(),
                "offset" to offset.toString(),
                "count" to PAGE_SIZE.toString(),
                // 0 — от новых к старым (так открывается чат с последнего сообщения); 1 — от самых старых.
                "rev" to "0",
            ),
        )
        return VkResponseParser.decode<MessageHistoryResponse>(response).toMessagePage(offset)
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
