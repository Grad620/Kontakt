package app.kontakt.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import app.kontakt.client.ui.components.CountBadge
import app.kontakt.client.ui.components.PagedColumn
import app.kontakt.client.ui.components.ScreenHeader
import app.kontakt.client.ui.components.SearchField
import app.kontakt.client.ui.components.SkeletonKind
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.KontaktIcons
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

    // Список лежит на цвете карточки (как в мессенджерах), а не на сером фоне страницы.
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ScreenHeader("Сообщения")
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Поиск по чатам",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
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
            skeleton = SkeletonKind.Chats,
            emptyIcon = if (query.isBlank()) KontaktIcons.Chat else KontaktIcons.Search,
            modifier = Modifier.weight(1f),
        ) { chat -> ChatRow(chat) }
    }
}

@Composable
private fun ChatRow(chat: ChatItem) {
    val style = Kontakt.style
    val unread = chat.unread > 0
    val tint = if (style.unreadTint && unread) Kontakt.colors.accentSoft else Color.Transparent
    Column(Modifier.background(tint)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.chatRowHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(chat.avatar, chat.title, style.chatAvatar)
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
                        color = if (unread) Kontakt.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (chat.lastIsOutgoing) "Вы: " else "") + chat.lastText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                        color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread) {
                        Spacer(Modifier.width(8.dp))
                        CountBadge(chat.unread)
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 28.dp + style.chatAvatar),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
    }
}
