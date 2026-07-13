package io.github.giuseppesorge.pictospeak.nlg.engine

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateSentenceEngineTest {
    private val engine = TemplateSentenceEngine(language = "it")

    private fun token(
        lemma: String,
        pos: Pos = Pos.NOUN,
    ) = PictogramToken(id = "id-$lemma", lemma = lemma, pos = pos, label = lemma)

    @Test
    fun `empty selection proposes nothing`() {
        assertTrue(engine.propose(emptyList()).isEmpty())
    }

    @Test
    fun `unmatched input degrades to labeled concat fallback, never throws`() {
        val candidates =
            engine.propose(
                listOf(token("io", Pos.MISC), token("volere", Pos.VERB), token("pizza")),
            )
        val fallback = candidates.single()
        assertEquals("io volere pizza", fallback.text)
        assertEquals(CandidateSource.FALLBACK_CONCAT, fallback.source)
    }

    @Test
    fun `single token proposes its label`() {
        val candidates = engine.propose(listOf(token("acqua")))
        assertEquals("acqua", candidates.single().text)
    }
}
