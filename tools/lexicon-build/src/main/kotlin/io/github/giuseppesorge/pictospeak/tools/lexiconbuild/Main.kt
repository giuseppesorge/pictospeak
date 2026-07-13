package io.github.giuseppesorge.pictospeak.tools.lexiconbuild

import io.github.giuseppesorge.pictospeak.nlg.lang.it.AdjectiveEntry
import io.github.giuseppesorge.pictospeak.nlg.lang.it.ItalianLexicon
import io.github.giuseppesorge.pictospeak.nlg.lang.it.LexiconEntry
import io.github.giuseppesorge.pictospeak.nlg.lang.it.NounEntry
import io.github.giuseppesorge.pictospeak.nlg.lang.it.VerbEntry
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Builds the bundled morphological lexicon from Morph-it! and the curated vocabulary:
 *   assets-src/morph-it/ + tools/core-vocabulary.csv + tools/lexicon-build/aux_it.csv
 *     -> app/src/main/assets/lexicon/lexicon_{lang}.json (+ LICENSE, PROVENANCE.md)
 *     -> app/src/test/resources/lexicon/morphit_reference_{lang}.tsv (property-test oracle)
 *
 * Catalog lemmas with no usable Morph-it! entry are recorded in the lexicon's
 * `unsupported` list: the engine degrades to citation form for them, never guesses.
 */
fun main(args: Array<String>) {
    val repoRoot = findRepoRoot(Paths.get("").toAbsolutePath())
    val lang = args.getOrNull(0) ?: "it"
    require(lang == "it") { "only 'it' is implemented; new languages add their own sources" }

    val morphItFile = repoRoot.resolve("assets-src/morph-it/current_version/morph-it_048.txt")
    if (!morphItFile.exists()) fail("Morph-it! not found — see PROVENANCE.md for the download procedure")

    val vocabulary = parseVocabulary(repoRoot.resolve("tools/core-vocabulary.csv"), lang)
    val verbTable = parseVerbTable(repoRoot.resolve("tools/lexicon-build/verbs_it.csv"))
    val nonAttributive = parseNonAttributive(repoRoot.resolve("tools/lexicon-build/non_attributive_it.csv"))

    val wantedLemmas = buildWantedLemmas(vocabulary)
    val rows = readMorphIt(morphItFile, wantedLemmas)
    println("morph-it: ${rows.size} rows for ${wantedLemmas.size} candidate lemmas")

    val overrides = parseOverrides(repoRoot.resolve("tools/lexicon-build/overrides_it.csv"))
    val builder = LexiconBuilder(rows.groupBy { it.lemma }, verbTable, nonAttributive, overrides)
    val entries = mutableListOf<LexiconEntry>()
    val unsupported = mutableListOf<String>()
    val problems = mutableListOf<String>()

    // SOCIAL/MISC/PROPER_NAME items need no morphology — they are neither entries nor
    // "unsupported"; the engine passes them through by design.
    val morphologyPos = setOf("VERB", "NOUN", "DESCRIPTOR")
    for (item in vocabulary.filter { it.pos in morphologyPos }) {
        when (val result = builder.build(item)) {
            is BuildResult.Entry -> entries += result.entry
            is BuildResult.Unsupported -> unsupported += item.lemma
            is BuildResult.Problem -> problems += result.message
        }
    }
    if (problems.isNotEmpty()) {
        problems.forEach { System.err.println("LEXICON: $it") }
        fail("${problems.size} problem(s) — fix verbs_it.csv or the vocabulary")
    }

    val assetsDir = repoRoot.resolve("app/src/main/assets/lexicon")
    Files.createDirectories(assetsDir)
    val json = Json { prettyPrint = true }
    val lexicon = ItalianLexicon(language = lang, entries = entries, unsupported = unsupported.sorted())
    Files.writeString(assetsDir.resolve("lexicon_$lang.json"), json.encodeToString(lexicon))
    writeReferenceTable(repoRoot, lang, rows, entries.map { it.lemma }.toSet())
    writeLicense(assetsDir)
    writeProvenance(assetsDir, repoRoot)

    val verbs = entries.count { it is VerbEntry }
    val nouns = entries.count { it is NounEntry }
    val adjectives = entries.count { it is AdjectiveEntry }
    println("lexicon_$lang.json: ${entries.size} entries ($verbs verbs, $nouns nouns, $adjectives adjectives)")
    println("unsupported (citation-form fallback): ${unsupported.size} -> ${unsupported.sorted().joinToString(", ")}")
}

internal fun findRepoRoot(start: Path): Path {
    var dir: Path? = start
    while (dir != null) {
        if (dir.resolve("settings.gradle.kts").exists()) return dir
        dir = dir.parent
    }
    return fail("run from inside the repository")
}

internal fun fail(message: String): Nothing {
    System.err.println("ERROR: $message")
    exitProcess(1)
}
