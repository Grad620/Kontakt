package app.vgradesk.client.data

import app.vgradesk.client.net.VkAttachment
import app.vgradesk.client.net.VkGroup
import app.vgradesk.client.net.VkPost
import app.vgradesk.client.net.VkUser
import app.vgradesk.client.net.VkWallPost
import app.vgradesk.client.rec.ContentFilter
import app.vgradesk.client.rec.FilterCategory
import app.vgradesk.client.rec.FilterConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Сколько публикаций скрыто фильтром (только счётчики, без содержимого). */
data class FilterCounts(val ads: Int = 0, val goods: Int = 0, val scam: Int = 0) {
    val total: Int get() = ads + goods + scam
    operator fun plus(other: FilterCounts) = FilterCounts(ads + other.ads, goods + other.goods, scam + other.scam)
}

/** Счётчики скрытого с момента запуска приложения. На диск не пишутся. */
class FilterStats {
    private val _counts = MutableStateFlow(FilterCounts())
    val counts: StateFlow<FilterCounts> = _counts.asStateFlow()

    fun add(counts: FilterCounts) {
        if (counts.total > 0) _counts.update { it + counts }
    }

    fun reset() {
        _counts.value = FilterCounts()
    }
}

/** Единый вид записи независимо от того, откуда она пришла (лента, стена, поиск). */
data class RawPost(
    val key: String,
    val authorId: Long,
    val date: Long,
    val text: String,
    val likes: Int,
    val comments: Int,
    val reposts: Int,
    val attachments: List<VkAttachment>,
    val markedAsAds: Boolean,
)

data class MappedBatch(val posts: List<FeedPost>, val dropped: FilterCounts)

object FeedMapping {
    private const val MAX_PHOTO_WIDTH = 800
    private const val MAX_PHOTOS = 4

    fun raw(post: VkPost): RawPost {
        val original = post.copyHistory.firstOrNull()
        return RawPost(
            key = "${post.sourceId}_${post.postId}",
            authorId = post.sourceId,
            date = post.date,
            text = post.text.ifBlank { original?.text.orEmpty() },
            likes = post.likes?.count ?: 0,
            comments = post.comments?.count ?: 0,
            reposts = post.reposts?.count ?: 0,
            attachments = post.attachments + original?.attachments.orEmpty(),
            markedAsAds = post.markedAsAds != 0,
        )
    }

    /** Ответы на записи (`reply`) не показываем. */
    fun raw(post: VkWallPost): RawPost? {
        if (post.postType == "reply") return null
        val original = post.copyHistory.firstOrNull()
        val author = when {
            post.ownerId < 0 -> post.ownerId
            post.fromId != 0L -> post.fromId
            else -> post.ownerId
        }
        return RawPost(
            key = "${post.ownerId}_${post.id}",
            authorId = author,
            date = post.date,
            text = post.text.ifBlank { original?.text.orEmpty() },
            likes = post.likes?.count ?: 0,
            comments = post.comments?.count ?: 0,
            reposts = post.reposts?.count ?: 0,
            attachments = post.attachments + original?.attachments.orEmpty(),
            markedAsAds = post.markedAsAds != 0,
        )
    }

    fun map(
        raws: List<RawPost>,
        users: List<VkUser>,
        groups: List<VkGroup>,
        source: PostSource,
        topic: String?,
        config: FilterConfig,
    ): MappedBatch {
        val userById = users.associateBy { it.id }
        val groupById = groups.associateBy { it.id }
        var ads = 0
        var goods = 0
        var scam = 0
        val posts = ArrayList<FeedPost>(raws.size)
        for (raw in raws) {
            val photos = photoUrls(raw.attachments)
            if (raw.text.isBlank() && photos.isEmpty()) continue
            val verdict = ContentFilter.evaluate(raw.text, raw.markedAsAds, config)
            if (verdict.drop) {
                when (verdict.category) {
                    FilterCategory.Ad -> ads++
                    FilterCategory.Goods -> goods++
                    else -> scam++
                }
                continue
            }
            val (name, avatar) = if (raw.authorId >= 0) {
                val user = userById[raw.authorId]
                (user?.fullName?.ifBlank { null } ?: "Пользователь") to user?.photo100
            } else {
                val group = groupById[-raw.authorId]
                (group?.name?.ifBlank { null } ?: "Сообщество") to group?.photo100
            }
            posts += FeedPost(
                key = raw.key,
                authorId = raw.authorId,
                authorName = name,
                authorAvatar = avatar,
                date = raw.date,
                text = raw.text,
                photoUrls = photos,
                likes = raw.likes,
                comments = raw.comments,
                reposts = raw.reposts,
                spam = verdict.spam,
                source = source,
                topic = topic,
            )
        }
        return MappedBatch(posts, FilterCounts(ads, goods, scam))
    }

    /** Берём уменьшенное превью (до 800 px по ширине), а не оригинал. */
    fun photoUrls(attachments: List<VkAttachment>): List<String> =
        attachments
            .filter { it.type == "photo" }
            .mapNotNull { attachment ->
                val sizes = attachment.photo?.sizes.orEmpty().filter { it.url.isNotBlank() }
                (sizes.filter { it.width in 1..MAX_PHOTO_WIDTH }.maxByOrNull { it.width } ?: sizes.minByOrNull { it.width })?.url
            }
            .take(MAX_PHOTOS)
}
