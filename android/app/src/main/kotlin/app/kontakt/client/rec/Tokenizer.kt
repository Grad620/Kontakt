package app.kontakt.client.rec

import kotlin.math.ln

/** stem — «основа» слова (первые 6 букв, грубо объединяет формы слова), surface — слово как написано. */
data class Token(val stem: String, val surface: String)

object Tokenizer {
    private const val MAX_TOKENS = 250
    private const val STEM_LENGTH = 6
    private val SPLIT = Regex("[^\\p{L}\\p{Nd}]+")

    private val STOP = setOf(
        "это", "как", "что", "так", "все", "для", "или", "его", "она", "они", "уже", "еще", "был", "была", "было",
        "были", "быть", "будет", "когда", "если", "только", "даже", "очень", "можно", "надо", "нет", "вот", "там",
        "тут", "где", "кто", "чем", "при", "над", "под", "про", "без", "про", "между", "после", "перед", "через",
        "этот", "эти", "эту", "этой", "этом", "того", "тоже", "свой", "свою", "наш", "ваш", "мой", "они", "нас",
        "вас", "мне", "нам", "вам", "ему", "ней", "них", "ним", "себя", "себе", "тебя", "меня", "один", "два",
        "три", "более", "всех", "всего", "также", "ещё", "ведь", "чтобы", "потому", "поэтому", "сегодня", "завтра",
        "http", "https", "www", "com", "club", "public", "the", "and", "for", "with", "that", "this",
    )

    fun tokens(text: String): List<Token> =
        text.lowercase().replace('ё', 'е')
            .split(SPLIT)
            .asSequence()
            .filter { it.length >= 3 && it !in STOP && !it.all { c -> c.isDigit() } }
            .take(MAX_TOKENS)
            .map { Token(it.take(STEM_LENGTH), it) }
            .toList()

    /** Частоты по основам, сглаженные: 1 + ln(количество). */
    fun termFrequencies(tokens: List<Token>): Map<String, Double> {
        val counts = HashMap<String, Int>()
        for (token in tokens) counts[token.stem] = (counts[token.stem] ?: 0) + 1
        return counts.mapValues { 1.0 + ln(it.value.toDouble()) }
    }
}
