package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.Board
import io.github.giuseppesorge.pictospeak.nlg.api.BoardCell
import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * The shipped English pack, end to end: the same pictograms as Italian, driven by the
 * English lexicon through the English realizer. Proves the LanguagePack contract works with
 * real data — the engine core is unchanged, only the pack differs.
 */
class EnglishBundledTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val assets: Path = Paths.get("src/main/assets")

    private val catalog: Map<String, PictogramToken> =
        json
            .decodeFromString<List<PictogramToken>>(Files.readString(assets.resolve("arasaac/catalog_en.json")))
            .associateBy { it.lemma.lowercase() }
    private val engine =
        TemplateSentenceEngine.forLanguage("en", Files.readString(assets.resolve("lexicon/lexicon_en.json")))

    private fun tokens(vararg lemmas: String): List<PictogramToken> =
        lemmas.map { requireNotNull(catalog[it]) { "lemma '$it' not in bundled English catalog" } }

    @Test
    fun `the flagship sentence works in English`() {
        val texts = engine.propose(tokens("i", "want", "eat", "pizza")).map { it.text }
        assertEquals("I want to eat the pizza", texts.first())
    }

    @Test
    fun `shipped English data covers the daily basics`() {
        val cases =
            mapOf(
                listOf("i", "eat", "pizza") to "I eat the pizza",
                listOf("he", "eat", "pizza") to "he eats the pizza",
                listOf("i", "want", "water") to "I want the water",
                listOf("i", "can", "go") to "I can go",
                listOf("i", "be", "happy") to "I am happy",
                listOf("she", "be", "tired") to "she is tired",
                listOf("i", "not", "eat") to "I do not eat",
            )
        cases.forEach { (lemmas, expected) ->
            val texts = engine.propose(tokens(*lemmas.toTypedArray())).map { it.text }
            assertEquals("input $lemmas gave $texts", expected, texts.first())
        }
    }

    @Test
    fun `English irregulars and past tense come from the lexicon`() {
        val go = engine.propose(tokens("he", "go")).map { it.text }
        assertTrue("missing 'he went' in $go", "he went" in go)
        val eat = engine.propose(tokens("i", "eat", "pizza")).map { it.text }
        assertTrue("missing 'I ate the pizza' in $eat", "I ate the pizza" in eat)
    }

    @Test
    fun `intransitive verb plus place noun degrades to concat`() {
        val candidates = engine.propose(tokens("i", "go", "house"))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }

    @Test
    fun `English catalog and boards are coherent`() {
        val catalogIds = catalog.values.map { it.id }.toSet()
        assertTrue("English catalog is suspiciously small (${catalogIds.size})", catalogIds.size >= 200)
        catalogIds.forEach { id ->
            assertTrue("missing image for $id", assets.resolve("arasaac/$id.png").exists())
        }
        val boards =
            assets
                .resolve("boards/default_en")
                .listDirectoryEntries("*.json")
                .map { json.decodeFromString<Board>(Files.readString(it)) }
        assertTrue("English home board missing", boards.any { it.id == "home" })
        val boardIds = boards.map { it.id }.toSet()
        boards.forEach { board ->
            board.cells.forEach { cell ->
                when (cell) {
                    is BoardCell.Pictogram ->
                        assertTrue(
                            "${board.id}: ${cell.pictogramId}",
                            cell.pictogramId in catalogIds,
                        )
                    is BoardCell.Link -> assertTrue("${board.id}: ${cell.boardId}", cell.boardId in boardIds)
                }
            }
        }
    }
}
