package app.kontakt.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kontakt.client.AppContainer
import app.kontakt.client.core.PagedState
import app.kontakt.client.core.Paginator
import app.kontakt.client.data.MessageItem
import app.kontakt.client.data.MessagesRepository
import app.kontakt.client.ui.common.formatChatTime
import app.kontakt.client.ui.common.vmFactory
import app.kontakt.client.ui.components.PagedColumn
import app.kontakt.client.ui.components.SubScreenHeader
import app.kontakt.client.ui.components.SkeletonKind
import kotlinx.coroutines.flow.StateFlow

private const val HISTORY_DENIED =
    "VK не разрешил получить историю сообщений для текущего токена. Откройте Настройки → Приложение и диагностика → " +
        "Проверить доступ к VK API. Если VK возвращает код 1051, это ограничение типа авторизации."

class MessagesHistoryViewModel(repo: MessagesRepository, peerId: Long) : ViewModel() {
    private val pager = Paginator<Int, MessageItem>(
        scope = viewModelScope,
        keyOf = { it.id },
        deniedMessage = HISTORY_DENIED,
        fetch = { cursor -> repo.history(peerId, cursor ?: 0) },
    )

    val state: StateFlow<PagedState<MessageItem>> = pager.state

    init { pager.refresh() }
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retry() = pager.retry()
}

@Composable
fun MessagesHistoryScreen(container: AppContainer, peerId: Long, onBack: () -> Unit) {
    val vm: MessagesHistoryViewModel = viewModel(
        key = "message-history-$peerId",
        factory = vmFactory { MessagesHistoryViewModel(container.messagesRepo, peerId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SubScreenHeader("Диалог", onBack = onBack)
        PagedColumn(
            state = state,
            itemKey = { it.id },
            emptyTitle = "Сообщений нет",
            emptyText = "Если список не загружается, проверьте доступ к messages.getHistory в диагностике VK API.",
            onRefresh = vm::refresh,
            onLoadMore = vm::loadMore,
            onRetry = vm::retry,
            skeleton = SkeletonKind.Chats,
            modifier = Modifier.weight(1f),
        ) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
private fun MessageBubble(message: MessageItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (message.outgoing) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    message.text.ifBlank { "Вложение или системное сообщение" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.outgoing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatChatTime(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.outgoing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
