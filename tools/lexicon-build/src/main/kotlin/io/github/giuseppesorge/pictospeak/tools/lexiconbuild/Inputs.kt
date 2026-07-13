package io.github.giuseppesorge.pictospeak.tools.lexiconbuild

import io.github.giuseppesorge.pictospeak.nlg.api.AdjectiveEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Auxiliary
import io.github.giuseppesorge.pictospeak.nlg.api.LexiconEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Person
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal const val MORPHIT_COLUMNS = 3

data class MorphRow(
    val form: String,
    val lemma: String,
    val features: String,
)

data class VocabularyItem(
    val lemma: String,
    val pos: String,
)

data class Overrides(
    val entries: Map<String, LexiconEntry>,
    val verbForms: Map<String, Map<Person, String>>,
)

/** Reflexive lemmas are looked up under their base verb (sedersi -> sedere). */
internal fun baseLemma(lemma: String): String = if (lemma.endsWith("rsi")) lemma.removeSuffix("rsi") + "re" else lemma

@Suppress("MagicNumber") // CSV column indices
internal fun parseVocabulary(
    path: Path,
    lang: String,
): List<VocabularyItem> =
    Files
        .readAllLines(path)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .map { it.split(",", limit = 8) }
        .filter { it[4].trim() == lang }
        .map { VocabularyItem(lemma = it[1].trim(), pos = it[3].trim()) }
        .distinctBy { it.lemma.lowercase() }

internal fun parseAuxTable(path: Path): Map<String, Pair<Auxiliary, Boolean>> =
    Files
        .readAllLines(path)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .associate { line ->
            val f = line.split(",").map { it.trim() }
            f[0] to (Auxiliary.valueOf(f[1]) to f[2].toBooleanStrict())
        }

/** Hand-curated patches for Morph-it! data gaps (tools/lexicon-build/overrides_it.csv). */
@Suppress("MagicNumber") // CSV column indices
internal fun parseOverrides(path: Path): Overrides {
    val entries = mutableMapOf<String, LexiconEntry>()
    val verbForms = mutableMapOf<String, MutableMap<Person, String>>()
    Files
        .readAllLines(path)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .forEach { line ->
            val f = line.split(",").map { it.trim() }
            when (f[1]) {
                "adjective" ->
                    entries[f[0]] =
                        AdjectiveEntry(
                            lemma = f[0],
                            masculineSingular = f[2],
                            feminineSingular = f[3],
                            masculinePlural = f[4],
                            femininePlural = f[5],
                        )
                "verb-form" ->
                    verbForms.getOrPut(f[0]) { mutableMapOf() }[Person.valueOf(f[2])] = f[3]
                else -> error("override type '${f[1]}' not implemented")
            }
        }
    return Overrides(entries, verbForms)
}

internal fun buildWantedLemmas(vocabulary: List<VocabularyItem>): Set<String> =
    vocabulary.flatMap { listOf(it.lemma.lowercase(), baseLemma(it.lemma.lowercase())) }.toSet()

internal fun readMorphIt(
    file: Path,
    wanted: Set<String>,
): List<MorphRow> =
    Files.newBufferedReader(file, StandardCharsets.ISO_8859_1).useLines { lines ->
        lines
            .mapNotNull { line ->
                val f = line.split('\t')
                if (f.size == MORPHIT_COLUMNS && f[1].lowercase() in wanted) {
                    MorphRow(f[0], f[1].lowercase(), f[2])
                } else {
                    null
                }
            }.toList()
    }
