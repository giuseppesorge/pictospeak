package io.github.giuseppesorge.pictospeak.nlg.lang.it

import org.junit.Assert.assertEquals
import org.junit.Test

class ItalianMorphologyTest {
    @Test
    fun `definite articles follow the phonological rules`() {
        val cases =
            mapOf(
                // masculine default
                ("gelato" to Gender.MASCULINE) to "il gelato",
                // s+consonant, z, gn, ps, y -> lo
                ("zaino" to Gender.MASCULINE) to "lo zaino",
                ("gnomo" to Gender.MASCULINE) to "lo gnomo",
                ("psicologo" to Gender.MASCULINE) to "lo psicologo",
                ("specchio" to Gender.MASCULINE) to "lo specchio",
                ("yogurt" to Gender.MASCULINE) to "lo yogurt",
                // vowel -> elision
                ("amico" to Gender.MASCULINE) to "l'amico",
                ("acqua" to Gender.FEMININE) to "l'acqua",
                // feminine default
                ("pizza" to Gender.FEMININE) to "la pizza",
            )
        cases.forEach { (input, expected) ->
            assertEquals(expected, ItalianMorphology.withDefiniteArticle(input.first, input.second, plural = false))
        }
    }

    @Test
    fun `plural definite articles`() {
        assertEquals("i gelati", ItalianMorphology.withDefiniteArticle("gelati", Gender.MASCULINE, plural = true))
        assertEquals("gli zaini", ItalianMorphology.withDefiniteArticle("zaini", Gender.MASCULINE, plural = true))
        assertEquals("gli amici", ItalianMorphology.withDefiniteArticle("amici", Gender.MASCULINE, plural = true))
        assertEquals("le pizze", ItalianMorphology.withDefiniteArticle("pizze", Gender.FEMININE, plural = true))
        assertEquals("le acque", ItalianMorphology.withDefiniteArticle("acque", Gender.FEMININE, plural = true))
    }

    @Test
    fun `indefinite articles`() {
        assertEquals("un gelato", ItalianMorphology.withIndefiniteArticle("gelato", Gender.MASCULINE))
        assertEquals("uno zaino", ItalianMorphology.withIndefiniteArticle("zaino", Gender.MASCULINE))
        assertEquals("uno specchio", ItalianMorphology.withIndefiniteArticle("specchio", Gender.MASCULINE))
        assertEquals("un amico", ItalianMorphology.withIndefiniteArticle("amico", Gender.MASCULINE))
        assertEquals("una pizza", ItalianMorphology.withIndefiniteArticle("pizza", Gender.FEMININE))
        assertEquals("un'amica", ItalianMorphology.withIndefiniteArticle("amica", Gender.FEMININE))
        assertEquals("un'acqua", ItalianMorphology.withIndefiniteArticle("acqua", Gender.FEMININE))
    }

    @Test
    fun `participle agreement inflects only regular -o participles`() {
        assertEquals("andato", ItalianMorphology.agreeParticiple("andato", feminine = false, plural = false))
        assertEquals("andata", ItalianMorphology.agreeParticiple("andato", feminine = true, plural = false))
        assertEquals("andati", ItalianMorphology.agreeParticiple("andato", feminine = false, plural = true))
        assertEquals("andate", ItalianMorphology.agreeParticiple("andato", feminine = true, plural = true))
        // -o participles inflect; anything else must be left untouched (never guess)
        assertEquals("sorpreso", ItalianMorphology.agreeParticiple("sorpreso", feminine = false, plural = false))
        assertEquals("sorpresa", ItalianMorphology.agreeParticiple("sorpreso", feminine = true, plural = false))
    }
}
