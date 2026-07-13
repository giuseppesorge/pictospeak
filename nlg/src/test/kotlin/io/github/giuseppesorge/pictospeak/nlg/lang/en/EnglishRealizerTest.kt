package io.github.giuseppesorge.pictospeak.nlg.lang.en

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Alternatives and safety properties the golden corpus (defaults only) cannot see. */
class EnglishRealizerTest {
    private val engine = TemplateSentenceEngine(EnglishRealizer(EnglishTestLexicon.lexicon))

    private fun t(
        lemma: String,
        pos: Pos,
    ) = PictogramToken(id = lemma, lemma = lemma, pos = pos, label = lemma)

    @Test
    fun `indefinite article is offered and chooses a-vs-an by sound`() {
        val pizza = engine.propose(listOf(t("I", Pos.MISC), t("want", Pos.VERB), t("pizza", Pos.NOUN))).map { it.text }
        assertEquals("I want the pizza", pizza.first())
        assertTrue("missing 'a pizza' in $pizza", "I want a pizza" in pizza)

        val apple = engine.propose(listOf(t("I", Pos.MISC), t("eat", Pos.VERB), t("apple", Pos.NOUN))).map { it.text }
        assertTrue("missing 'an apple' in $apple", "I eat an apple" in apple)

        // the article agrees with the adjective's sound: "an old egg", "a big egg"
        val oldEgg =
            engine
                .propose(listOf(t("I", Pos.MISC), t("want", Pos.VERB), t("old", Pos.DESCRIPTOR), t("egg", Pos.NOUN)))
                .map { it.text }
        assertTrue("missing 'an old egg' in $oldEgg", "I want an old egg" in oldEgg)
        val bigEgg =
            engine
                .propose(listOf(t("I", Pos.MISC), t("want", Pos.VERB), t("big", Pos.DESCRIPTOR), t("egg", Pos.NOUN)))
                .map { it.text }
        assertTrue("missing 'a big egg' in $bigEgg", "I want a big egg" in bigEgg)
    }

    @Test
    fun `simple past is offered with irregular forms from the lexicon`() {
        val eat = engine.propose(listOf(t("I", Pos.MISC), t("eat", Pos.VERB), t("pizza", Pos.NOUN))).map { it.text }
        assertTrue("missing 'I ate the pizza' in $eat", "I ate the pizza" in eat)
        val go = engine.propose(listOf(t("he", Pos.MISC), t("go", Pos.VERB))).map { it.text }
        assertTrue("missing 'he went' in $go", "he went" in go)
        val was =
            engine
                .propose(
                    listOf(t("I", Pos.MISC), t("be", Pos.VERB), t("happy", Pos.DESCRIPTOR)),
                ).map { it.text }
        assertTrue("missing 'I was happy' in $was", "I was happy" in was)
    }

    @Test
    fun `want-past uses the past of want, not do-support`() {
        val texts =
            engine.propose(listOf(t("she", Pos.MISC), t("want", Pos.VERB), t("eat", Pos.VERB))).map { it.text }
        assertTrue("missing 'she wanted to eat' in $texts", "she wanted to eat" in texts)
    }

    @Test
    fun `intransitive verb plus noun degrades to concat, never a guessed object`() {
        val candidates = engine.propose(listOf(t("I", Pos.MISC), t("go", Pos.VERB), t("house", Pos.NOUN)))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }

    @Test
    fun `no candidate ever drops a word the user selected`() {
        val candidates = engine.propose(listOf(t("I", Pos.MISC), t("eat", Pos.VERB), t("big", Pos.DESCRIPTOR)))
        assertEquals(1, candidates.size)
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
        assertEquals("I eat big", candidates.single().text)
    }

    @Test
    fun `unknown material and empty lexicon degrade to concat`() {
        assertEquals(
            CandidateSource.FALLBACK_CONCAT,
            engine.propose(listOf(t("xyzzy", Pos.MISC), t("eat", Pos.VERB))).single().source,
        )
        val bare = TemplateSentenceEngine(EnglishRealizer(EnglishLexicon.parse(null)))
        assertEquals(
            CandidateSource.FALLBACK_CONCAT,
            bare.propose(listOf(t("I", Pos.MISC), t("eat", Pos.VERB))).single().source,
        )
    }
}
