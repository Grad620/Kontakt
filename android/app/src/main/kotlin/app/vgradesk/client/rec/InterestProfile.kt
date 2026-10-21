package app.vgradesk.client.rec

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Serializable
data class AuthorStat(var affinity: Double = 0.0, var impressions: Int = 0)

/**
 * Профиль интересов пользователя: веса слов (основ), отношение к авторам/сообществам.
 * Хранится только на устройстве. Старые интересы затухают (период полураспада задаёт RankingConfig).
 */
@Serializable
class InterestProfile(
    val terms: MutableMap<String, Double> = HashMap(),
    /** Как слово выглядело в тексте (для поисковых запросов), по основе. */
    val surfaces: MutableMap<String, String> = HashMap(),
    val authors: MutableMap<Long, AuthorStat> = HashMap(),
    var updatedAt: Long = 0L,
    var signals: Int = 0,
) {
    fun decay(nowMs: Long, halfLifeMs: Long) {
        if (updatedAt == 0L) {
            updatedAt = nowMs
            return
        }
        val elapsed = nowMs - updatedAt
        if (elapsed <= 0L) return
        val factor = 0.5.pow(elapsed.toDouble() / halfLifeMs.toDouble())
        for (entry in terms.entries) entry.setValue(entry.value * factor)
        val dead = terms.filterValues { abs(it) < MIN_WEIGHT }.keys.toSet()
        terms.keys.removeAll(dead)
        surfaces.keys.retainAll(terms.keys)
        for (stat in authors.values) stat.affinity *= factor
        updatedAt = nowMs
    }

    /** Обучение на одном сигнале: value > 0 — интерес, value < 0 — нежелание видеть такое. */
    fun learn(
        tf: Map<String, Double>,
        surfaceOf: Map<String, String>,
        authorId: Long,
        value: Double,
        rate: Double,
        maxTerms: Int,
    ) {
        val norm = sqrt(tf.values.sumOf { it * it }).takeIf { it > 0.0 } ?: 1.0
        for ((stem, frequency) in tf) {
            val delta = rate * value * (frequency / norm)
            terms[stem] = ((terms[stem] ?: 0.0) + delta).coerceIn(-MAX_WEIGHT, MAX_WEIGHT)
            if (value > 0.0) surfaceOf[stem]?.let { surfaces[stem] = it }
        }
        val stat = authors.getOrPut(authorId) { AuthorStat() }
        stat.affinity = (stat.affinity + value * AUTHOR_SHARE).coerceIn(-MAX_AFFINITY, MAX_AFFINITY)
        signals++
        trimTerms(maxTerms)
        trimAuthors()
    }

    fun bumpImpression(authorId: Long) {
        authors.getOrPut(authorId) { AuthorStat() }.impressions++
        trimAuthors()
    }

    private fun trimTerms(max: Int) {
        if (terms.size <= max) return
        val keep = terms.entries.sortedByDescending { abs(it.value) }.take(max).map { it.key }.toSet()
        terms.keys.retainAll(keep)
        surfaces.keys.retainAll(terms.keys)
    }

    private fun trimAuthors() {
        if (authors.size <= MAX_AUTHORS) return
        val keep = authors.entries
            .sortedByDescending { abs(it.value.affinity) + it.value.impressions * 0.01 }
            .take(MAX_AUTHORS)
            .map { it.key }
            .toSet()
        authors.keys.retainAll(keep)
    }

    companion object {
        private const val MIN_WEIGHT = 0.02
        private const val MAX_WEIGHT = 5.0
        private const val MAX_AFFINITY = 6.0
        private const val AUTHOR_SHARE = 0.5
        private const val MAX_AUTHORS = 600
    }
}
