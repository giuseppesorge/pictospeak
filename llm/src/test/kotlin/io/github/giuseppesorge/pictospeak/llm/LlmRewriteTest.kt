package io.github.giuseppesorge.pictospeak.llm

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmRewriteTest {
    private fun token(label: String) = PictogramToken(id = label, lemma = label, pos = Pos.MISC, label = label)

    private fun baseline(text: String) = SentenceCandidate(text = text, source = CandidateSource.TEMPLATE, trace = "t")

    @Test
    fun `prompt contains the words and the draft`() {
        val prompt =
            LlmRewrite.buildPrompt(
                listOf(token("io"), token("mangiare"), token("pizza")),
                baseline("io mangio la pizza"),
            )
        assertTrue(prompt.contains("io mangiare pizza"))
        assertTrue(prompt.contains("io mangio la pizza"))
        // Language-neutral: the prompt must not hard-code a language name.
        assertTrue(!prompt.contains("Italian") && !prompt.contains("English"))
    }

    @Test
    fun `clean takes the first non-empty line and strips a label prefix and quotes`() {
        val cleaned =
            LlmRewrite.cleanResponse("Sentence: \"Io voglio mangiare la pizza.\"\nextra junk", baseline("draft"))
        assertEquals("Io voglio mangiare la pizza.", cleaned)
    }

    @Test
    fun `clean collapses whitespace`() {
        assertEquals("io voglio pizza", LlmRewrite.cleanResponse("io   voglio\tpizza", baseline("draft")))
    }

    @Test
    fun `blank response yields null`() {
        assertNull(LlmRewrite.cleanResponse("   \n  \n", baseline("draft")))
    }

    @Test
    fun `a rambling over-long response is rejected`() {
        val long = "word ".repeat(80)
        assertNull(LlmRewrite.cleanResponse(long, baseline("draft")))
    }

    @Test
    fun `an echo of the draft is not offered as a duplicate`() {
        assertNull(LlmRewrite.cleanResponse("io voglio la pizza", baseline("io voglio la pizza")))
        // ...even with different casing and trailing punctuation.
        assertNull(LlmRewrite.cleanResponse("Io voglio la pizza.", baseline("io voglio la pizza")))
    }

    @Test
    fun `candidate is always labeled LLM`() {
        assertEquals(CandidateSource.LLM, LlmRewrite.candidate("io voglio la pizza").source)
    }
}
