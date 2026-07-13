package io.github.giuseppesorge.pictospeak.nlg.engine

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.lang.it.TestLexicon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the golden corpus — the executable spec of grammar v1. Every future field bug
 * becomes a new line in the corpus (docs/grammar-v1.md).
 */
class GoldenCorpusTest {
    private val engine = TemplateSentenceEngine(language = "it", lexicon = TestLexicon.lexicon)

    private data class Case(
        val line: Int,
        val tokens: List<PictogramToken>,
        val expected: String,
    )

    private fun corpus(): List<Case> =
        javaClass.classLoader!!
            .getResourceAsStream("golden/it/corpus.tsv")!!
            .bufferedReader()
            .readLines()
            .mapIndexedNotNull { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@mapIndexedNotNull null
                val (input, expected) = line.split('\t', limit = 2)
                val tokens =
                    input.split(' ').map { spec ->
                        val lemma = spec.substringBeforeLast(':').replace('_', ' ')
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
    fun `every proposal list ends with the concat fallback`() {
        corpus().forEach { case ->
            val last = engine.propose(case.tokens).last()
            assertEquals(
                "line ${case.line}",
                CandidateSource.FALLBACK_CONCAT,
                last.source,
            )
        }
    }

    @Test
    fun `corpus is not silently shrinking`() {
        assertTrue("corpus has ${corpus().size} cases", corpus().size >= 75)
    }
}
