package app.kontakt.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kontakt.client.AppContainer
import app.kontakt.client.core.PagedState
import app.kontakt.client.core.Paginator
import app.kontakt.client.data.ChatItem
import app.kontakt.client.data.ChatsRepository
import app.kontakt.client.ui.common.formatChatTime
import app.kontakt.client.ui.common.vmFactory
import app.kontakt.client.ui.components.Avatar
import app.kontakt.client.ui.components.PagedColumn
import app.kontakt.client.ui.components.ScreenHeader
import kotlinx.coroutines.flow.StateFlow

private const val CHATS_DENIED =
    "VK отклонил запрос личных сообщений (код 1051): текущий тип токена не подходит для этого метода. " +
        "Это ограничение авторизации VK, не VPN. Повтор запроса не поможет; доступ должен быть разрешён самим VK."

class ChatsViewModel(repo: ChatsRepository) : ViewModel() {
    private val pager = Paginator<Int, ChatItem>(
        scope = viewModelScope,
        keyOf = { it.peerId },
        deniedMessage = CHATS_DENIED,
        fetch = { cursor -> repo.page(cursor ?: 0) },
    )
    val state: StateFlow<PagedState<ChatItem>> = pager.state

    init { pager.refresh() }
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()
}

@Composable
fun ChatsScreen(container: AppContainer) {
    val vm: ChatsViewModel = viewModel(factory = vmFactory { ChatsViewModel(container.chatsRepo) })
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = state.copy(
        items = state.items.filter {
            query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) ||
                it.lastText.contains(query.trim(), ignoreCase = true)
        },
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader("Сообщения")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Поиск по чатам") },
            singleLine = true,
            shape = CircleShape,
        )
        Spacer(Modifier.height(4.dp))
        PagedColumn(
            state = filtered,
            itemKey = { it.peerId },
            emptyTitle = if (query.isBlank()) "Сообщений пока нет" else "Ничего не найдено",
            emptyText = if (query.isBlank()) {
                "Диалоги появятся здесь, когда VK разрешит Kontakt доступ к сообщениям."
            } else {
                "Попробуй изменить запрос."
            },
            onRefresh = vm::refresh,
            onLoadMore = vm::loadMore,
            onRetry = vm::retry,
            modifier = Modifier.weight(1f),
        ) { chat -> ChatRow(chat) }
    }
}

@Composable
private fun ChatRow(chat: ChatItem) {
    val unread = chat.unread > 0
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(chat.avatar, chat.title, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatChatTime(chat.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (chat.lastIsOutgoing) "Вы: " else "") + chat.lastText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (chat.unread > 99) "99+" else chat.unread.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}
