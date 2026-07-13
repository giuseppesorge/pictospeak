package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.Lexicon
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Engine + the REAL bundled lexicon and catalog: proves the shipped data actually drives
 * grammatical output end-to-end (the fixture-based golden corpus cannot see data drift).
 */
class BundledEngineIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val assets = Paths.get("src/main/assets")

    private val lexicon: Lexicon =
        json.decodeFromString(Files.readString(assets.resolve("lexicon/lexicon_it.json")))
    private val catalog: Map<String, PictogramToken> =
        json
            .decodeFromString<List<PictogramToken>>(Files.readString(assets.resolve("arasaac/catalog_it.json")))
            .associateBy { it.lemma.lowercase() }
    private val engine = TemplateSentenceEngine(language = "it", lexicon = lexicon)

    private fun tokens(vararg lemmas: String): List<PictogramToken> =
        lemmas.map { requireNotNull(catalog[it]) { "lemma '$it' not in bundled catalog" } }

    @Test
    fun `the flagship sentence works with shipped data`() {
        val texts = engine.propose(tokens("io", "volere", "mangiare", "pizza")).map { it.text }
        assertEquals("io voglio mangiare la pizza", texts.first())
    }

    @Test
    fun `shipped data covers the daily basics`() {
        val cases =
            mapOf(
                listOf("volere", "acqua") to "voglio l'acqua",
                listOf("io", "essere", "stanco") to "io sono stanco",
                listOf("lei", "essere", "felice") to "lei è felice",
                listOf("non", "volere", "mangiare") to "non voglio mangiare",
                listOf("mamma", "andare") to "la mamma va",
                listOf("io", "volere", "giocare") to "io voglio giocare",
            )
        cases.forEach { (lemmas, expected) ->
            val texts = engine.propose(tokens(*lemmas.toTypedArray())).map { it.text }
            assertEquals("input $lemmas gave $texts", expected, texts.first())
        }
    }

    @Test
    fun `passato prossimo with shipped auxiliaries`() {
        val andare = engine.propose(tokens("lei", "andare")).map { it.text }
        assertTrue("missing 'lei è andata' in $andare", "lei è andata" in andare)
        val mangiare = engine.propose(tokens("io", "mangiare", "pizza")).map { it.text }
        assertTrue("missing 'io ho mangiato la pizza' in $mangiare", "io ho mangiato la pizza" in mangiare)
    }

    @Test
    fun `adversarial-review regressions hold against shipped data`() {
        // uovo is masculine (data-bug fix): article and agreement must be masculine
        val uovo = engine.propose(tokens("uovo", "essere", "rotto")).map { it.text }
        assertEquals("l'uovo è rotto", uovo.first())
        assertTrue(
            "must never say un'uovo/una",
            engine.propose(tokens("io", "volere", "uovo")).all {
                "un'uovo" !in
                    it.text
            },
        )
        // città must not carry an ASCII apostrophe
        assertTrue(
            "città apostrophe leaked",
            engine.propose(tokens("io", "volere", "città")).none { it.text.contains("citta'") },
        )
        // intransitive verb + place noun degrades to concat, never "vado la casa"
        val andare = engine.propose(tokens("io", "andare", "casa"))
        assertEquals(CandidateSource.FALLBACK_CONCAT, andare.single().source)
        // 'dopo' (invariable adverb, now unsupported) must never inflect to "dopa"
        assertTrue(
            "dopo inflected",
            engine.propose(tokens("io", "volere", "pizza", "dopo")).none { it.text.contains("dopa") },
        )
    }

    @Test
    fun `every proposal for shipped vocabulary ends with concat and never throws`() {
        // A quick sweep: pair every core verb with a common object; must never throw.
        val verbs = listOf("volere", "mangiare", "bere", "guardare", "fare", "avere")
        val objects = listOf("pizza", "acqua", "palla", "libro", "musica")
        verbs.forEach { v ->
            objects.forEach { o ->
                val candidates = engine.propose(tokens(v, o))
                assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.last().source)
                assertTrue(candidates.first().text.isNotBlank())
            }
        }
    }
}
