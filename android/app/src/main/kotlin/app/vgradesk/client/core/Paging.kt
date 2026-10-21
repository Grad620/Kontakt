package app.vgradesk.client.core

import app.vgradesk.client.net.VkError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiError(val kind: Kind, val message: String) {
    enum class Kind { Offline, AccessDenied, SessionExpired, Other }
}

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    /** Ошибка на весь экран (когда показывать нечего). */
    val error: UiError? = null,
    /** Ошибка поверх уже показанных данных (кнопка «Повторить» под списком). */
    val inlineError: UiError? = null,
)

data class Page<K, T>(val items: List<T>, val next: K?)

fun Throwable.toUiError(deniedMessage: String): UiError = when (this) {
    is VkError.Network -> UiError(
        UiError.Kind.Offline,
        "Нет соединения с интернетом или VK недоступен. Проверьте сеть и повторите.",
    )
    is VkError.SessionExpired -> UiError(UiError.Kind.SessionExpired, "Сессия истекла. Войдите снова.")
    is VkError.AccessDenied -> UiError(UiError.Kind.AccessDenied, deniedMessage)
    is VkError.RateLimited -> UiError(
        UiError.Kind.Other,
        "VK просит подождать: слишком много запросов. Повторите через минуту.",
    )
    is VkError.Captcha -> UiError(
        UiError.Kind.Other,
        "VK запросил проверку (капчу). Kontakt её не обходит: откройте VK, подтвердите вход и повторите.",
    )
    is VkError.Server -> UiError(UiError.Kind.Other, "Ошибка VK (код $code): $vkMessage")
    else -> UiError(UiError.Kind.Other, "Не удалось загрузить данные. Повторите позже.")
}

/**
 * Постраничная загрузка: один активный запрос за раз, дубли игнорируются,
 * обновление отменяет текущую подгрузку, ошибки не стирают уже показанные данные.
 * K — тип курсора (смещение или строка start_from), null = первая страница.
 */
class Paginator<K : Any, T>(
    private val scope: CoroutineScope,
    private val keyOf: (T) -> Any,
    private val deniedMessage: String,
    private val fetch: suspend (K?) -> Page<K, T>,
) {
    private val _state = MutableStateFlow(PagedState<T>(loading = true))
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var job: Job? = null
    private var cursor: K? = null
    private var lastWasReset = true

    fun refresh() = start(reset = true)

    fun loadMore() {
        val s = _state.value
        if (s.endReached || s.loading || s.refreshing || s.loadingMore) return
        if (s.error != null || s.inlineError != null) return
        start(reset = false)
    }

    fun retry() {
        val s = _state.value
        start(reset = s.items.isEmpty() || lastWasReset)
    }

    /** Когда сеть вернулась: если экран стоит на ошибке «нет соединения», повторяем загрузку сами. */
    fun retryIfOffline() {
        val s = _state.value
        val offline = s.error?.kind == UiError.Kind.Offline || s.inlineError?.kind == UiError.Kind.Offline
        if (offline && job?.isActive != true) retry()
    }

    /** Убрать элементы из показанного списка (например, «Не интересно»). Курсор не меняется. */
    fun removeWhere(predicate: (T) -> Boolean) {
        _state.update { it.copy(items = it.items.filterNot(predicate)) }
    }

    private fun start(reset: Boolean) {
        if (job?.isActive == true) {
            if (!reset) return
            job?.cancel()
        }
        lastWasReset = reset
        val hadItems = _state.value.items.isNotEmpty()
        _state.update {
            when {
                reset && !hadItems -> it.copy(
                    loading = true, refreshing = false, loadingMore = false, error = null, inlineError = null,
                )
                reset -> it.copy(loading = false, refreshing = true, loadingMore = false, inlineError = null)
                else -> it.copy(loadingMore = true, inlineError = null)
            }
        }
        val from = if (reset) null else cursor
        job = scope.launch {
            try {
                val page = fetch(from)
                cursor = page.next
                _state.update { s ->
                    val merged = if (reset) page.items else (s.items + page.items).distinctBy(keyOf)
                    PagedState(items = merged, endReached = page.next == null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val ui = e.toUiError(deniedMessage)
                _state.update { s ->
                    if (s.items.isEmpty()) {
                        s.copy(loading = false, refreshing = false, loadingMore = false, error = ui)
                    } else {
                        s.copy(loading = false, refreshing = false, loadingMore = false, inlineError = ui)
                    }
                }
            }
        }
    }
}
