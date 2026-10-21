package app.vgradesk.client.rec

import app.vgradesk.client.data.FeedPost
import app.vgradesk.client.data.PostSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class HiddenSource(val id: Long, val name: String)

/** Краткая сводка для экрана настроек: что приложение «узнало» о вас. Хранится только на устройстве. */
data class RecommenderSummary(
    val signals: Int = 0,
    val topInterests: List<String> = emptyList(),
    val knownSources: Int = 0,
    val hiddenSources: List<HiddenSource> = emptyList(),
)

/**
 * Собственная рекомендательная система Kontakt. Работает целиком на устройстве, ничего не отправляет в VK.
 * Учится на реальных сигналах: время просмотра, быстрый пропуск, «Интересно», «Не интересно», скрытие источника.
 * Все методы потокобезопасны (одна блокировка).
 */
class Recommender(
    private val storage: RecommenderStorage,
    private val ranker: Ranker = Ranker(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private var profile = InterestProfile()
    private val seen = LinkedHashSet<String>()
    private val liked = LinkedHashSet<String>()
    private val hidden = LinkedHashMap<Long, String>()
    private var dirty = false

    /** Вызывается после каждого изменения; подписчик решает, когда сохранять (например, с задержкой). */
    @Volatile
    var onChanged: (() -> Unit)? = null

    private val _summary = MutableStateFlow(RecommenderSummary())
    val summary: StateFlow<RecommenderSummary> = _summary.asStateFlow()

    private val _interested = MutableStateFlow<Set<String>>(emptySet())
    val interested: StateFlow<Set<String>> = _interested.asStateFlow()

    fun load() {
        val state = storage.load()
        synchronized(lock) {
            profile = state.profile
            seen.clear()
            seen.addAll(state.seen)
            liked.clear()
            liked.addAll(state.liked)
            hidden.clear()
            hidden.putAll(state.hidden)
            profile.decay(clock(), ranker.config.halfLifeMs)
            publish(likedChanged = true)
        }
    }

    fun isSeen(key: String): Boolean = synchronized(lock) { key in seen }

    fun isHidden(authorId: Long): Boolean = synchronized(lock) { authorId in hidden }

    /** Лучшие [limit] публикаций из [posts]: скрытые источники и «отвергнутые» авторы отбрасываются. */
    fun rank(posts: List<FeedPost>, limit: Int): List<FeedPost> = synchronized(lock) {
        val now = clock()
        profile.decay(now, ranker.config.halfLifeMs)
        val scored = posts
            .filter { allowed(it) }
            .map { ranker.score(it, ranker.analyze(it), profile, now, it.key in seen) }
        ranker.select(scored, limit).map { it.post }
    }

    /** Отфильтровать без ранжирования (режим «Подписки»): только скрытые источники. */
    fun withoutHidden(posts: List<FeedPost>): List<FeedPost> = synchronized(lock) {
        posts.filter { it.authorId !in hidden }
    }

    /** Можно ли показывать эту публикацию (источник не скрыт и не «отвергнут» повторными «Не интересно»). */
    fun isAllowed(post: FeedPost): Boolean = synchronized(lock) { allowed(post) }

    private fun allowed(post: FeedPost): Boolean {
        if (post.authorId in hidden) return false
        val affinity = profile.authors[post.authorId]?.affinity ?: 0.0
        return affinity > ranker.config.blockedAffinity
    }

    fun onSignal(signal: Signal) {
        synchronized(lock) {
            val now = clock()
            profile.decay(now, ranker.config.halfLifeMs)
            val post = signal.post
            var likedChanged = false
            when (signal.kind) {
                SignalKind.DWELL, SignalKind.QUICK_SKIP -> {
                    markSeenLocked(post.key)
                    profile.bumpImpression(post.authorId)
                }
                SignalKind.INTERESTED -> {
                    markSeenLocked(post.key)
                    likedChanged = liked.add(post.key)
                }
                SignalKind.INTEREST_REMOVED -> likedChanged = liked.remove(post.key)
                SignalKind.NOT_INTERESTED -> {
                    markSeenLocked(post.key)
                    likedChanged = liked.remove(post.key)
                }
            }
            val tokens = Tokenizer.tokens(post.text)
            val surfaces = HashMap<String, String>()
            for (token in tokens) surfaces[token.stem] = token.surface
            profile.learn(
                tf = Tokenizer.termFrequencies(tokens),
                surfaceOf = surfaces,
                authorId = post.authorId,
                value = SignalClassifier.valueOf(signal),
                rate = ranker.config.learningRate,
                maxTerms = ranker.config.maxTerms,
            )
            dirty = true
            publish(likedChanged)
        }
        onChanged?.invoke()
    }

    /** Публикацию видели, но сигнала нет (1,2–3 с на экране): запоминаем только «уже показано». */
    fun markSeen(post: FeedPost) {
        synchronized(lock) {
            markSeenLocked(post.key)
            profile.bumpImpression(post.authorId)
            dirty = true
        }
        onChanged?.invoke()
    }

    private fun markSeenLocked(key: String) {
        seen.remove(key)
        seen.add(key)
        while (seen.size > ranker.config.maxSeen) {
            val eldest = seen.iterator()
            eldest.next()
            eldest.remove()
        }
    }

    /** «Не показывать этот источник»: явное решение пользователя, снимается в настройках. */
    fun hideSource(authorId: Long, name: String) {
        synchronized(lock) {
            hidden[authorId] = name
            dirty = true
            publish(likedChanged = false)
        }
        onChanged?.invoke()
    }

    fun unhideSource(authorId: Long) {
        synchronized(lock) {
            if (hidden.remove(authorId) != null) {
                dirty = true
                publish(likedChanged = false)
            }
        }
        onChanged?.invoke()
    }

    /** Сброс обучения. Список скрытых источников — ваш явный выбор, он сохраняется. */
    fun reset() {
        synchronized(lock) {
            profile = InterestProfile()
            seen.clear()
            liked.clear()
            dirty = true
            publish(likedChanged = true)
        }
        onChanged?.invoke()
    }

    /**
     * Поисковые слова для поиска «за пределами подписок». До появления интересов — широкие общие темы
     * (они меняются каждый час), потом — слова из публикаций, которые вам были интересны, плюс одна
     * «случайная» тема, чтобы лента не замыкалась.
     */
    fun searchQueries(count: Int): List<String> = synchronized(lock) {
        val learned = profile.terms.entries
            .filter { it.value >= MIN_QUERY_WEIGHT && profile.surfaces.containsKey(it.key) }
            .sortedByDescending { it.value }
            .mapNotNull { profile.surfaces[it.key] }
        val learnedSlots = minOf(learned.size, if (count >= 2) count - 1 else count)
        val result = ArrayList<String>()
        result.addAll(learned.take(learnedSlots))
        val hour = clock() / 3_600_000L
        var i = 0
        while (result.size < count && i < COLD_TOPICS.size) {
            val topic = COLD_TOPICS[((hour + i * 5L) % COLD_TOPICS.size).toInt()]
            if (topic !in result) result.add(topic)
            i++
        }
        result
    }

    /** Сообщества, к которым у вас накопился интерес (для чтения их стен). */
    fun favoriteCommunities(count: Int): List<Long> = synchronized(lock) {
        profile.authors.entries
            .filter { it.key < 0 && it.key !in hidden && it.value.affinity >= FAVORITE_AFFINITY }
            .sortedByDescending { it.value.affinity }
            .take(count)
            .map { -it.key }
    }

    /** Ответ на вопрос «Почему я это вижу?» — только по реальным причинам, которые есть в данных. */
    fun explain(post: FeedPost, nowSeconds: Long = clock() / 1000): List<String> = synchronized(lock) {
        val lines = ArrayList<String>()
        when (post.source) {
            PostSource.Subscriptions ->
                lines += "Эта публикация есть в вашей ленте ВКонтакте (источник: «${post.authorName}»)."
            PostSource.Search ->
                lines += "Найдена поиском VK по теме «${post.topic ?: "из ваших интересов"}»: так Kontakt ищет новое за пределами подписок."
            PostSource.Community ->
                lines += "Из сообщества «${post.authorName}», публикации которого вам обычно интересны."
        }
        val tf = ranker.analyze(post)
        val matched = tf.keys
            .filter { (profile.terms[it] ?: 0.0) > MATCH_WEIGHT }
            .sortedByDescending { profile.terms[it] ?: 0.0 }
            .take(3)
            .map { profile.surfaces[it] ?: it }
        if (matched.isNotEmpty()) lines += "Совпадает с вашими интересами: ${matched.joinToString(", ")}."
        val stat = profile.authors[post.authorId]
        if (stat != null && stat.affinity >= 0.8) {
            lines += "Вы часто задерживаетесь на публикациях этого источника или отмечаете их как интересные."
        } else if (stat == null || stat.impressions == 0) {
            lines += "Этот источник для вас новый: Kontakt иногда показывает незнакомое, чтобы лента не зацикливалась."
        }
        val ageHours = (nowSeconds - post.date) / 3600.0
        if (post.date > 0 && ageHours in 0.0..12.0) lines += "Свежая публикация."
        if (post.likes + 2 * post.comments + 3 * post.reposts >= 100) {
            lines += "Публикация вызвала много реакций (нравится: ${post.likes}, комментарии: ${post.comments})."
        }
        if (profile.signals == 0) {
            lines += "Kontakt пока не знает ваших интересов и подбирает общие темы. Он учится по времени просмотра и по кнопкам «Интересно» и «Не интересно»."
        }
        lines
    }

    /** Сохранить изменения, если они есть. Вызывать не на главном потоке. */
    fun flush() {
        val text = synchronized(lock) {
            if (!dirty) return
            dirty = false
            storage.encode(StoredState(profile, seen.toList(), liked.toList(), LinkedHashMap(hidden)))
        }
        try {
            storage.write(text)
        } catch (e: Exception) {
            synchronized(lock) { dirty = true }
        }
    }

    /** Удалить сохранённое обучение с устройства (при выходе из аккаунта). */
    fun wipeStorage() {
        synchronized(lock) {
            profile = InterestProfile()
            seen.clear()
            liked.clear()
            hidden.clear()
            dirty = false
            publish(likedChanged = true)
        }
        storage.delete()
    }

    private fun publish(likedChanged: Boolean) {
        val top = profile.terms.entries
            .filter { it.value >= SUMMARY_WEIGHT }
            .sortedByDescending { it.value }
            .take(SUMMARY_TERMS)
            .map { profile.surfaces[it.key] ?: it.key }
        _summary.value = RecommenderSummary(
            signals = profile.signals,
            topInterests = top,
            knownSources = profile.authors.values.count { it.impressions > 0 || abs(it.affinity) > 0.3 },
            hiddenSources = hidden.map { HiddenSource(it.key, it.value) },
        )
        if (likedChanged) _interested.value = liked.toSet()
    }

    companion object {
        private const val MIN_QUERY_WEIGHT = 0.4
        private const val FAVORITE_AFFINITY = 0.8
        private const val MATCH_WEIGHT = 0.3
        private const val SUMMARY_WEIGHT = 0.25
        private const val SUMMARY_TERMS = 6

        /** Широкие темы для «холодного старта». Фиксированы в коде, пользователь их не выбирает. */
        val COLD_TOPICS = listOf(
            "новости", "наука", "путешествия", "кино", "музыка", "технологии", "кулинария", "спорт",
            "книги", "игры", "природа", "искусство", "история", "здоровье", "автомобили", "фотография",
        )
    }
}
