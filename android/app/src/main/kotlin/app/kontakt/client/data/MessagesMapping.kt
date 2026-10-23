package app.kontakt.client.data

import app.kontakt.client.core.Page
import app.kontakt.client.net.MessageHistoryResponse

data class MessageItem(
    val id: Long,
    val fromId: Long,
    val text: String,
    val date: Long,
    val outgoing: Boolean,
)

/**
 * Превращает страницу `messages.getHistory` в страницу для UI.
 *
 * Запрос идёт с `rev=0`, поэтому VK отдаёт сообщения от новых к старым, и `offset` считается от самого
 * свежего сообщения. Порядок сохраняется как есть: экран переписки показывает список с `reverseLayout`,
 * поэтому первое (самое новое) сообщение оказывается внизу, а подгрузка добавляет более старые сверху.
 *
 * Следующий сдвиг не выдаётся, если страница пустая (защита от бесконечной подгрузки при неверном `count`)
 * или уже получены все [MessageHistoryResponse.count] сообщений.
 */
fun MessageHistoryResponse.toMessagePage(offset: Int): Page<Int, MessageItem> {
    val mapped = items.map { message ->
        MessageItem(
            id = message.id,
            fromId = message.fromId,
            text = message.text,
            date = message.date,
            outgoing = message.out == 1,
        )
    }
    val nextOffset = offset + mapped.size
    val next = nextOffset.takeIf { mapped.isNotEmpty() && it < count }
    return Page(mapped, next)
}
