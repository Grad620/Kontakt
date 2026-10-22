package app.kontakt.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kontakt.client.core.PagedState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max
import kotlin.math.min

/**
 * Общий список с состояниями: загрузка, ошибка, пусто, обновление свайпом, подгрузка при прокрутке.
 * Позиция прокрутки сохраняется при переключении вкладок (rememberLazyListState + сохранение стека навигации).
 * Внизу оставляется место под плавающую панель навигации.
 *
 * [onVisibleKeys] (необязательно) сообщает, какие элементы сейчас заметно видны на экране (не меньше половины
 * высоты). По этим данным лента измеряет время просмотра.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PagedColumn(
    state: PagedState<T>,
    itemKey: (T) -> Any,
    emptyTitle: String,
    emptyText: String,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onVisibleKeys: ((Set<Any>) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val error = state.error
    when {
        state.loading -> LoadingBox(modifier)
        error != null -> ErrorState(error, onRetry, modifier)
        state.items.isEmpty() -> StateMessage(emptyTitle, emptyText, modifier, "Обновить", onRefresh)
        else -> {
            val listState = rememberLazyListState()
            val nearEnd by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
                }
            }
            LaunchedEffect(nearEnd, state.items.size) {
                if (nearEnd) onLoadMore()
            }

            val currentOnVisible by rememberUpdatedState(onVisibleKeys)
            if (onVisibleKeys != null) {
                LaunchedEffect(listState) {
                    snapshotFlow {
                        val info = listState.layoutInfo
                        val start = info.viewportStartOffset
                        val end = info.viewportEndOffset
                        val viewport = end - start
                        info.visibleItemsInfo
                            .filter { item ->
                                val visible = min(item.offset + item.size, end) - max(item.offset, start)
                                visible > 0 && visible * 2 >= min(item.size, viewport)
                            }
                            .map { it.key }
                            .filter { it != FOOTER_KEY }
                            .toSet()
                    }
                        .distinctUntilChanged()
                        .collect { keys -> currentOnVisible?.invoke(keys) }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = modifier,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = LocalBottomBarPadding.current),
                ) {
                    items(state.items, key = itemKey) { item -> itemContent(item) }
                    item(key = FOOTER_KEY) {
                        val inline = state.inlineError
                        when {
                            state.loadingMore -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                            inline != null -> Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    inline.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                TextButton(onClick = onRetry) { Text("Повторить") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val FOOTER_KEY = "footer"
