package app.kontakt.client.data

/** Откуда пришла публикация. Нужно для объяснения «Почему я это вижу?» и для диагностики источников. */
enum class PostSource(val title: String, val method: String) {
    Subscriptions("Подписки", "newsfeed.get"),
    Search("Поиск по темам", "newsfeed.search"),
    Community("Стены сообществ", "wall.get"),
}

data class FeedPost(
    val key: String,
    /** Идентификатор автора: пользователь (> 0) или сообщество (< 0). */
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val date: Long,
    val text: String,
    val photoUrls: List<String>,
    val likes: Int,
    val comments: Int,
    val reposts: Int = 0,
    /** Оценка «мусорности» от 0 до 1 (эвристика фильтра). Влияет только на ранжирование. */
    val spam: Double = 0.0,
    val source: PostSource = PostSource.Subscriptions,
    /** Для источника «Поиск по темам»: по какому слову нашли. */
    val topic: String? = null,
)
