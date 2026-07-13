package io.github.giuseppesorge.pictospeak.nlg.lang.en

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the English golden corpus — the executable spec of English grammar v1. */
class EnglishGoldenCorpusTest {
    private val engine = TemplateSentenceEngine(EnglishRealizer(EnglishTestLexicon.lexicon))

    private data class Case(
        val line: Int,
        val tokens: List<PictogramToken>,
        val expected: String,
    )

    private fun corpus(): List<Case> =
        javaClass.classLoader!!
            .getResourceAsStream("golden/en/corpus.tsv")!!
            .bufferedReader()
            .readLines()
            .mapIndexedNotNull { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@mapIndexedNotNull null
                val (input, expected) = line.split('\t', limit = 2)
                val tokens =
                    input.split(' ').map { spec ->
                        val lemma = spec.substringBeforeLast(':')
                        val pos = Pos.valueOf(spec.substringAfterLast(':'))
                        PictogramToken(id = "g-$lemma", lemma = lemma, pos = pos, label = lemma)
                    }
                Case(index + 1, tokens, expected.trim())
            }

    @Test
    fun `golden corpus - default candidate matches the spec`() {
        val failures =
            corpus().mapNotNull { case ->
                val candidates = engine.propose(case.tokens)
                val default = candidates.first()
                when {
                    case.expected == "CONCAT" ->
                        if (default.source != CandidateSource.FALLBACK_CONCAT || candidates.size != 1) {
                            "line ${case.line}: expected concat-only, got " +
                                candidates.joinToString { "'${it.text}' (${it.source})" }
                        } else {
                            null
                        }
                    default.text != case.expected || default.source != CandidateSource.TEMPLATE ->
                        "line ${case.line}: expected '${case.expected}', got '${default.text}' (${default.source})"
                    else -> null
                }
            }
        assertTrue(failures.joinToString("\n", prefix = "\n"), failures.isEmpty())
    }

    @Test
    fun `every proposal ends with the concat fallback`() {
        corpus().forEach { case ->
            assertEquals(
                "line ${case.line}",
                CandidateSource.FALLBACK_CONCAT,
                engine.propose(case.tokens).last().source,
            )
        }
    }

    @Test
    fun `corpus is not silently shrinking`() {
        assertTrue("corpus has ${corpus().size} cases", corpus().size >= 45)
    }
}
