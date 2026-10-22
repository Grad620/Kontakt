package app.kontakt.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kontakt.client.AppContainer
import app.kontakt.client.core.PagedState
import app.kontakt.client.core.Paginator
import app.kontakt.client.data.FeedPost
import app.kontakt.client.data.FeedRepository
import app.kontakt.client.data.ForYouRepository
import app.kontakt.client.data.PostSource
import app.kontakt.client.rec.Recommender
import app.kontakt.client.rec.Signal
import app.kontakt.client.rec.SignalClassifier
import app.kontakt.client.rec.SignalKind
import app.kontakt.client.rec.DwellTracker
import app.kontakt.client.ui.common.formatPostDate
import app.kontakt.client.ui.common.vmFactory
import app.kontakt.client.ui.components.Avatar
import app.kontakt.client.ui.components.InfoBanner
import app.kontakt.client.ui.components.PagedColumn
import app.kontakt.client.ui.components.ScreenHeader
import app.kontakt.client.ui.components.SegmentedTabs
import app.kontakt.client.ui.components.SkeletonKind
import app.kontakt.client.ui.theme.Kontakt
import app.kontakt.client.ui.theme.KontaktIcons
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val FOR_YOU_DENIED =
    "Не удалось получить публикации через доступные методы VK API. Возможности зависят от типа токена и разрешений приложения. " +
        "Kontakt не может расширить их самостоятельно."

private const val FEED_DENIED =
    "VK отклонил запрос ленты. Доступность newsfeed.get зависит от типа токена и разрешений приложения; " +
        "Kontakt не может расширить их самостоятельно."

enum class FeedMode(val title: String) {
    ForYou("Для вас"),
    Subscriptions("Подписки"),
}

/** Данные для окна «Почему я это вижу?». */
data class Explanation(val author: String, val lines: List<String>)

class FeedViewModel(
    forYouRepo: ForYouRepository,
    feedRepo: FeedRepository,
    private val recommender: Recommender,
    online: StateFlow<Boolean>,
) : ViewModel() {

    private val forYouPager = Paginator<Int, FeedPost>(
        scope = viewModelScope,
        keyOf = { it.key },
        deniedMessage = FOR_YOU_DENIED,
        fetch = { cursor -> forYouRepo.page(cursor) },
    )
    private val subsPager = Paginator<String, FeedPost>(
        scope = viewModelScope,
        keyOf = { it.key },
        deniedMessage = FEED_DENIED,
        fetch = { cursor -> feedRepo.page(cursor) },
    )

    val forYouState: StateFlow<PagedState<FeedPost>> = forYouPager.state
    val subsState: StateFlow<PagedState<FeedPost>> = subsPager.state
    val sourceStatuses = forYouRepo.statuses
    val interested: StateFlow<Set<String>> = recommender.interested

    private val _mode = MutableStateFlow(FeedMode.ForYou)
    val mode: StateFlow<FeedMode> = _mode.asStateFlow()

    private val _explanation = MutableStateFlow<Explanation?>(null)
    val explanation: StateFlow<Explanation?> = _explanation.asStateFlow()

    private var subsStarted = false

    /** Показанные публикации по ключу: нужны, чтобы по времени просмотра понять, какая публикация была на экране. */
    private val shown = LinkedHashMap<String, FeedPost>()
    private val tracker = DwellTracker(onDwell = { key, millis -> onDwell(key, millis) })

    init {
        forYouPager.refresh()
        viewModelScope.launch { forYouPager.state.collect { rememberShown(it.items) } }
        viewModelScope.launch { subsPager.state.collect { rememberShown(it.items) } }
        // Сеть вернулась — повторяем загрузку, если она остановилась из-за отсутствия соединения.
        viewModelScope.launch {
            online.collect { isOnline ->
                if (isOnline) {
                    forYouPager.retryIfOffline()
                    subsPager.retryIfOffline()
                }
            }
        }
    }

    private fun rememberShown(items: List<FeedPost>) {
        for (item in items) shown[item.key] = item
        while (shown.size > MAX_SHOWN) {
            val eldest = shown.keys.iterator()
            eldest.next()
            eldest.remove()
        }
    }

    fun select(mode: FeedMode) {
        _mode.value = mode
        if (mode == FeedMode.Subscriptions && !subsStarted) {
            subsStarted = true
            subsPager.refresh()
        }
    }

    fun refresh() = if (_mode.value == FeedMode.ForYou) forYouPager.refresh() else subsPager.refresh()
    fun loadMore() = if (_mode.value == FeedMode.ForYou) forYouPager.loadMore() else subsPager.loadMore()
    fun retry() = if (_mode.value == FeedMode.ForYou) forYouPager.retry() else subsPager.retry()

    // --- время просмотра ---

    fun onVisible(keys: Set<Any>) {
        tracker.onVisible(keys.mapNotNullTo(HashSet()) { it as? String })
    }

    fun resume() = tracker.resume()
    fun pause() = tracker.pause()

    private fun onDwell(key: String, millis: Long) {
        val post = shown[key] ?: return
        val kind = SignalClassifier.fromDwell(millis)
        if (kind != null) {
            recommender.onSignal(Signal(kind, post, millis / 1000.0))
        } else {
            recommender.markSeen(post)
        }
    }

    // --- явные действия пользователя ---

    fun toggleInterested(post: FeedPost) {
        val kind = if (post.key in recommender.interested.value) SignalKind.INTEREST_REMOVED else SignalKind.INTERESTED
        recommender.onSignal(Signal(kind, post))
    }

    fun notInterested(post: FeedPost) {
        shown.remove(post.key)
        recommender.onSignal(Signal(SignalKind.NOT_INTERESTED, post))
        forYouPager.removeWhere { it.key == post.key }
        subsPager.removeWhere { it.key == post.key }
    }

    fun hideSource(post: FeedPost) {
        shown.values.removeAll { it.authorId == post.authorId }
        recommender.hideSource(post.authorId, post.authorName)
        forYouPager.removeWhere { it.authorId == post.authorId }
        subsPager.removeWhere { it.authorId == post.authorId }
    }

    fun explain(post: FeedPost) {
        _explanation.value = Explanation(post.authorName, recommender.explain(post))
    }

    fun dismissExplanation() {
        _explanation.value = null
    }

    override fun onCleared() {
        tracker.pause()
    }

    private companion object {
        const val MAX_SHOWN = 400
    }
}

@Composable
fun FeedScreen(container: AppContainer) {
    val vm: FeedViewModel = viewModel(
        factory = vmFactory {
            FeedViewModel(container.forYouRepo, container.feedRepo, container.recommender, container.network.online)
        },
    )
    val mode by vm.mode.collectAsStateWithLifecycle()
    val forYou by vm.forYouState.collectAsStateWithLifecycle()
    val subs by vm.subsState.collectAsStateWithLifecycle()
    val statuses by vm.sourceStatuses.collectAsStateWithLifecycle()
    val interested by vm.interested.collectAsStateWithLifecycle()
    val explanation by vm.explanation.collectAsStateWithLifecycle()

    // Время просмотра считается, только пока экран ленты открыт и приложение на виду.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.resume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.pause() }
    DisposableEffect(vm) { onDispose { vm.pause() } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Лента")
        SegmentedTabs(
            titles = FeedMode.entries.map { it.title },
            selectedIndex = mode.ordinal,
            onSelect = { vm.select(FeedMode.entries[it]) },
        )

        if (mode == FeedMode.ForYou) {
            val unavailable = statuses.filterValues { it == ForYouRepository.SourceState.Denied }.keys
            if (unavailable.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                InfoBanner(
                    "VK не дал доступ к источникам: " + unavailable.joinToString { it.title.lowercase() } +
                        ". Лента собрана из остальных.",
                )
            }
        }

        key(mode) {
            val state = if (mode == FeedMode.ForYou) forYou else subs
            PagedColumn(
                state = state,
                itemKey = { it.key },
                emptyTitle = if (mode == FeedMode.ForYou) "Пока нечего показать" else "В ленте пока пусто",
                emptyText = if (mode == FeedMode.ForYou) {
                    "Новое появится после обновления. Всё свежее уже просмотрено или скрыто фильтром."
                } else {
                    "Новые публикации друзей и сообществ появятся здесь."
                },
                onRefresh = vm::refresh,
                onLoadMore = vm::loadMore,
                onRetry = vm::retry,
                onVisibleKeys = vm::onVisible,
                skeleton = SkeletonKind.Posts,
                emptyIcon = KontaktIcons.Feed,
                modifier = Modifier.weight(1f),
            ) { post ->
                PostCard(
                    post = post,
                    interested = post.key in interested,
                    onInterested = { vm.toggleInterested(post) },
                    onNotInterested = { vm.notInterested(post) },
                    onHideSource = { vm.hideSource(post) },
                    onWhy = { vm.explain(post) },
                )
            }
        }
    }

    explanation?.let { info ->
        AlertDialog(
            onDismissRequest = vm::dismissExplanation,
            title = { Text("Почему я это вижу?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Источник: ${info.author}", style = MaterialTheme.typography.labelLarge)
                    info.lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = vm::dismissExplanation) { Text("Понятно") } },
        )
    }
}

@Composable
private fun PostCard(
    post: FeedPost,
    interested: Boolean,
    onInterested: () -> Unit,
    onNotInterested: () -> Unit,
    onHideSource: () -> Unit,
    onWhy: () -> Unit,
) {
    val style = Kontakt.style
    var menuOpen by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = style.screenPadding, vertical = style.postGap / 2),
        shape = style.cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = if (style.cardBordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(bottom = 2.dp)) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(post.authorAvatar, post.authorName, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val topic = if (post.source == PostSource.Search && post.topic != null) " · тема «${post.topic}»" else ""
                    Text(
                        formatPostDate(post.date) + topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (post.text.isNotBlank()) {
                Text(
                    post.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                )
            }

            when (post.photoUrls.size) {
                0 -> Unit
                1 -> AsyncImage(
                    model = post.photoUrls[0],
                    contentDescription = "Фотография",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                )
                else -> LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(post.photoUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Фотография",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(196.dp)
                                .width(236.dp)
                                .clip(MaterialTheme.shapes.medium),
                        )
                    }
                }
            }

            // Счётчики VK только показываются (кнопок «нравится» и «комментировать» здесь нет).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountLabel(KontaktIcons.Like, post.likes, "Нравится")
                Spacer(Modifier.width(16.dp))
                CountLabel(KontaktIcons.Comment, post.comments, "Комментарии")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onInterested) {
                    Icon(
                        imageVector = if (interested) KontaktIcons.Star else KontaktIcons.StarOutline,
                        contentDescription = null,
                        tint = if (interested) Kontakt.colors.accentText else muted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Интересно",
                        color = if (interested) Kontakt.colors.accentText else muted,
                    )
                }
                Column {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(KontaktIcons.More, contentDescription = "Ещё", tint = muted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Почему я это вижу?") },
                            onClick = {
                                menuOpen = false
                                onWhy()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Не интересно") },
                            onClick = {
                                menuOpen = false
                                onNotInterested()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Не показывать «${post.authorName}»", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                menuOpen = false
                                onHideSource()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Значок и число: счётчик лайков или комментариев VK. Для чтения с экрана — «Нравится: 5». */
@Composable
private fun CountLabel(icon: ImageVector, count: Int, description: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$description: $count" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
