package app.vgradesk.client

import app.vgradesk.client.data.FeedPost
import app.vgradesk.client.rec.AuthorStat
import app.vgradesk.client.rec.InterestProfile
import app.vgradesk.client.rec.RecommenderStorage
import app.vgradesk.client.rec.StoredState

const val NOW_MS = 1_760_000_000_000L

fun post(
    key: String,
    author: Long,
    text: String,
    ageHours: Int = 1,
    likes: Int = 0,
    comments: Int = 0,
    spam: Double = 0.0,
): FeedPost = FeedPost(
    key = key,
    authorId = author,
    authorName = "Автор $author",
    authorAvatar = null,
    date = NOW_MS / 1000 - ageHours * 3600L,
    text = text,
    photoUrls = emptyList(),
    likes = likes,
    comments = comments,
    spam = spam,
)

/** Хранилище в памяти: «запись» сохраняет глубокую копию, как это сделал бы файл. */
class MemoryStorage : RecommenderStorage {
    private val snapshots = HashMap<String, StoredState>()
    private var counter = 0
    var current: StoredState = StoredState()
    var writes = 0
    var deleted = false

    override fun load(): StoredState = copy(current)

    override fun encode(state: StoredState): String {
        val key = "snap-${counter++}"
        snapshots[key] = copy(state)
        return key
    }

    override fun write(text: String) {
        current = snapshots.getValue(text)
        writes++
    }

    override fun delete() {
        current = StoredState()
        deleted = true
    }

    private fun copy(state: StoredState): StoredState {
        val p = state.profile
        val authors = HashMap<Long, AuthorStat>()
        for ((id, stat) in p.authors) authors[id] = AuthorStat(stat.affinity, stat.impressions)
        val profile = InterestProfile(HashMap(p.terms), HashMap(p.surfaces), authors, p.updatedAt, p.signals)
        return StoredState(profile, state.seen.toList(), state.liked.toList(), LinkedHashMap(state.hidden))
    }
}
