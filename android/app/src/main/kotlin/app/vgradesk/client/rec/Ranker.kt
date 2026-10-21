package app.vgradesk.client.rec

import app.vgradesk.client.data.FeedPost
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

data class RankingConfig(
    val halfLifeDays: Double = 14.0,
    val maxTerms: Int = 400,
    val learningRate: Double = 0.35,
    val freshnessHalfLifeHours: Double = 36.0,
    val wRelevance: Double = 1.6,
    val wAuthor: Double = 1.0,
    val wFreshness: Double = 0.9,
    val wPopularity: Double = 0.5,
    val wExploration: Double = 0.35,
    val spamPenalty: Double = 2.0,
    val seenPenalty: Double = 3.0,
    /** MMR: 1.0 — только релевантность, меньше — сильнее штраф за похожесть на уже выбранное. */
    val mmrLambda: Double = 0.7,
    val similarityScale: Double = 3.0,
    /** Не больше authorCap постов одного автора среди последних authorWindow выбранных. */
    val authorCap: Int = 2,
    val authorWindow: Int = 10,
    /** Автор с таким или меньшим «отношением» больше не показывается. */
    val blockedAffinity: Double = -3.5,
    val maxSeen: Int = 800,
) {
    val halfLifeMs: Long get() = (halfLifeDays * 24 * 3600 * 1000).toLong()
}

data class Scored(val post: FeedPost, val tf: Map<String, Double>, val score: Double)

/**
 * Собственное ранжирование: релевантность тексту по профилю интересов + отношение к автору +
 * свежесть + популярность + исследование (новые авторы) − спам − уже виденное.
 * Затем выбор с разнообразием (MMR и ограничение на одного автора подряд).
 */
class Ranker(val config: RankingConfig = RankingConfig()) {

    fun analyze(post: FeedPost): Map<String, Double> =
        Tokenizer.termFrequencies(Tokenizer.tokens(post.text))

    fun score(post: FeedPost, tf: Map<String, Double>, profile: InterestProfile, nowMs: Long, seen: Boolean): Scored {
        val dot = tf.entries.sumOf { (profile.terms[it.key] ?: 0.0) * it.value }
        val norm = sqrt(tf.values.sumOf { it * it })
        val relevance = if (norm > 0.0) tanh(dot / (norm * 2.0)) else 0.0

        val stat = profile.authors[post.authorId]
        val author = tanh((stat?.affinity ?: 0.0) / 3.0)

        val ageHours = max(0.0, (nowMs / 1000.0 - post.date) / 3600.0)
        val freshness = 0.5.pow(ageHours / config.freshnessHalfLifeHours)

        val engagement = post.likes + 2.0 * post.comments + 3.0 * post.reposts
        val popularity = (ln(1.0 + engagement) / ln(1.0 + 2000.0)).coerceIn(0.0, 1.0)

        val exploration = 1.0 / sqrt(1.0 + (stat?.impressions ?: 0))

        var total = config.wRelevance * relevance +
            config.wAuthor * author +
            config.wFreshness * freshness +
            config.wPopularity * popularity +
            config.wExploration * exploration -
            config.spamPenalty * post.spam
        if (seen) total -= config.seenPenalty
        return Scored(post, tf, total)
    }

    fun select(scored: List<Scored>, limit: Int): List<Scored> {
        val remaining = scored.sortedByDescending { it.score }.toMutableList()
        val picked = ArrayList<Scored>()
        while (picked.size < limit && remaining.isNotEmpty()) {
            val recent = picked.takeLast(config.authorWindow)
            var best: Scored? = null
            var bestValue = Double.NEGATIVE_INFINITY
            var bestCapped = true
            for (candidate in remaining) {
                val capped = recent.count { it.post.authorId == candidate.post.authorId } >= config.authorCap
                val similarity = picked.maxOfOrNull { cosine(it.tf, candidate.tf) } ?: 0.0
                val value = config.mmrLambda * candidate.score -
                    (1.0 - config.mmrLambda) * config.similarityScale * similarity
                val better = best == null ||
                    (bestCapped && !capped) ||
                    (capped == bestCapped && value > bestValue)
                if (better) {
                    best = candidate
                    bestValue = value
                    bestCapped = capped
                }
            }
            val chosen = best ?: break
            picked += chosen
            remaining.remove(chosen)
        }
        return picked
    }

    companion object {
        fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
            if (a.isEmpty() || b.isEmpty()) return 0.0
            val small = if (a.size <= b.size) a else b
            val big = if (a.size <= b.size) b else a
            var dot = 0.0
            for ((key, value) in small) {
                val other = big[key]
                if (other != null) dot += value * other
            }
            val na = sqrt(a.values.sumOf { it * it })
            val nb = sqrt(b.values.sumOf { it * it })
            return if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
        }
    }
}
