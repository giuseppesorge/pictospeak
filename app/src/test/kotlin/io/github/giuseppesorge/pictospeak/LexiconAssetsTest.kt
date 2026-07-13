package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.AdjectiveEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Lexicon
import io.github.giuseppesorge.pictospeak.nlg.api.NounEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Person
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.api.VerbEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Validates the bundled lexicon against the bundled catalog (definition of done for the
 * lexicon pipeline): full coverage accounting, structural completeness of every entry, and
 * a property check of verb morphology against the Morph-it!-derived reference table.
 */
class LexiconAssetsTest {
    private val assets: Path = Paths.get("src/main/assets")
    private val json = Json { ignoreUnknownKeys = true }
    private val language = "it"

    private fun lexicon(): Lexicon =
        json.decodeFromString(Files.readString(assets.resolve("lexicon/lexicon_$language.json")))

    private fun catalog(): List<PictogramToken> =
        json.decodeFromString(Files.readString(assets.resolve("arasaac/catalog_$language.json")))

    @Test
    fun `every morphology-bearing catalog lemma is either covered or explicitly unsupported`() {
        val lexicon = lexicon()
        val covered = lexicon.entries.map { it.lemma.lowercase() }.toSet()
        val unsupported = lexicon.unsupported.map { it.lowercase() }.toSet()
        val needsMorphology = setOf(Pos.VERB, Pos.NOUN, Pos.DESCRIPTOR)

        val unaccounted =
            catalog()
                .filter { it.pos in needsMorphology }
                .map { it.lemma.lowercase() }
                .filterNot { it in covered || it in unsupported }
        assertTrue("lemmas neither in lexicon nor declared unsupported: $unaccounted", unaccounted.isEmpty())

        val both = covered.intersect(unsupported)
        assertTrue("lemmas both covered and unsupported: $both", both.isEmpty())
    }

    @Test
    fun `entries are structurally complete`() {
        val problems =
            buildList {
                for (entry in lexicon().entries) {
                    when (entry) {
                        is VerbEntry -> {
                            if (entry.presentIndicative.keys != Person.entries.toSet()) {
                                add("${entry.lemma}: incomplete present indicative")
                            }
                            if (entry.pastParticiple.isBlank()) add("${entry.lemma}: blank participle")
                        }
                        is NounEntry -> if (entry.singular.isBlank()) add("${entry.lemma}: blank singular")
                        is AdjectiveEntry ->
                            if (
                                listOf(
                                    entry.masculineSingular,
                                    entry.feminineSingular,
                                    entry.masculinePlural,
                                    entry.femininePlural,
                                ).any { it.isBlank() }
                            ) {
                                add("${entry.lemma}: blank adjective form")
                            }
                    }
                }
            }
        assertTrue(problems.joinToString(), problems.isEmpty())
    }

    // Property check against the Morph-it!-derived reference table: every verb form we ship
    // must exist in the source data (verbs have no whole-entry overrides by design).
    @Test
    fun `verb forms exist in the reference inflection table`() {
        val reference =
            javaClass.classLoader!!
                .getResourceAsStream("lexicon/morphit_reference_$language.tsv")!!
                .bufferedReader()
                .readLines()
                .map { it.split('\t') }
                .filter { it.size == 3 }
                .groupBy({ it[1] }, { it[0] })

        val problems =
            buildList {
                for (entry in lexicon().entries.filterIsInstance<VerbEntry>()) {
                    val base = if (entry.reflexive) entry.lemma.removeSuffix("rsi") + "re" else entry.lemma
                    val known = reference[base].orEmpty().toSet()
                    (entry.presentIndicative.values + entry.pastParticiple)
                        .filterNot { it in known }
                        .forEach { add("${entry.lemma}: form '$it' not found in Morph-it! reference") }
                }
            }
        assertTrue(problems.joinToString(), problems.isEmpty())
    }

    @Test
    fun `license fencing files ship with the lexicon`() {
        assertTrue(Files.exists(assets.resolve("lexicon/LICENSE")))
        assertTrue(Files.exists(assets.resolve("lexicon/PROVENANCE.md")))
    }
}
