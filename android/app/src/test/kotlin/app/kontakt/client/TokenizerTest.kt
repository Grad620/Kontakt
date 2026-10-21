package app.kontakt.client

import app.kontakt.client.rec.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenizerTest {
    @Test
    fun stemsAreFirstSixLettersAndFormsOfAWordMerge() {
        val stems = Tokenizer.tokens("путешествие путешествия путешествию").map { it.stem }.toSet()
        assertEquals(setOf("путеше"), stems)
    }

    @Test
    fun stopWordsDigitsAndShortWordsAreDropped() {
        val tokens = Tokenizer.tokens("Это 2024 год, и мы в горах").map { it.surface }
        assertEquals(listOf("год", "горах"), tokens)
    }

    @Test
    fun yoIsNormalizedToYe() {
        val tokens = Tokenizer.tokens("Ёлка")
        assertEquals("елка", tokens.single().surface)
    }

    @Test
    fun frequenciesAreSmoothed() {
        val tf = Tokenizer.termFrequencies(Tokenizer.tokens("горы горы горы озеро"))
        assertTrue(tf.getValue("горы") > tf.getValue("озеро"))
        assertEquals(1.0, tf.getValue("озеро"), 1e-9)
    }

    @Test
    fun tokenCountIsBounded() {
        val text = (1..1000).joinToString(" ") { "слово$it" }
        assertTrue(Tokenizer.tokens(text).size <= 250)
    }
}
