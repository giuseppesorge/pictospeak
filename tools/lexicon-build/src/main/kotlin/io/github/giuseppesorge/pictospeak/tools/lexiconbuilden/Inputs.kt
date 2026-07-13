package io.github.giuseppesorge.pictospeak.tools.lexiconbuilden

import java.nio.file.Files
import java.nio.file.Path

data class EnVocabRow(
    val arasaacId: String,
    val lemma: String,
    val pos: String,
)

data class IrregularVerb(
    val thirdSingular: String,
    val past: String,
    val pastParticiple: String,
)

@Suppress("MagicNumber") // CSV column indices
internal fun parseVocabulary(
    path: Path,
    lang: String,
): List<EnVocabRow> =
    dataLines(path)
        .map { it.split(",", limit = 8) }
        .filter { it[4].trim() == lang }
        .map { EnVocabRow(arasaacId = it[0].trim(), lemma = it[1].trim(), pos = it[3].trim()) }
        .distinctBy { it.lemma.lowercase() }

/** arasaacId -> transitive, inherited from the Italian verb on the same pictogram. */
@Suppress("MagicNumber")
internal fun transitivityFromItalian(
    vocabCsv: Path,
    verbsItCsv: Path,
): Map<String, Boolean> {
    val itTransitive =
        dataLines(verbsItCsv)
            .associate { line ->
                val f = line.split(",").map { it.trim() }
                f[0] to f[3].toBooleanStrict()
            }
    return dataLines(vocabCsv)
        .map { it.split(",", limit = 8) }
        .filter { it[4].trim() == "it" && it[3].trim() == "VERB" }
        .mapNotNull { row -> itTransitive[row[1].trim()]?.let { row[0].trim() to it } }
        .toMap()
}

internal fun parseIrregularVerbs(path: Path): Map<String, IrregularVerb> {
    if (!Files.exists(path)) return emptyMap()
    return dataLines(path).associate { line ->
        val f = line.split(",").map { it.trim() }
        f[0] to IrregularVerb(thirdSingular = f[1], past = f[2], pastParticiple = f[3])
    }
}

internal fun parseIrregularPlurals(path: Path): Map<String, String> {
    if (!Files.exists(path)) return emptyMap()
    return dataLines(path).associate { line ->
        val f = line.split(",").map { it.trim() }
        f[0] to f[1]
    }
}

internal fun parseArticles(path: Path): Map<String, String> {
    if (!Files.exists(path)) return emptyMap()
    return dataLines(path).associate { line ->
        val f = line.split(",").map { it.trim() }
        f[0] to f[1]
    }
}

internal fun parseSet(path: Path): Set<String> {
    if (!Files.exists(path)) return emptySet()
    return dataLines(path).map { it.substringBefore(",").trim().lowercase() }.toSet()
}

private fun dataLines(path: Path): List<String> =
    Files
        .readAllLines(path)
        .drop(1)
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
