package io.github.giuseppesorge.pictospeak.tools.lexiconbuilden

import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnAdjective
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnEntry
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnNoun
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnVerb
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnglishLexicon
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Builds the English LanguagePack lexicon (lexicon_en.json) from curated data and rules —
 * English has no Morph-it! equivalent, so regular forms are COMPUTED (EnglishMorphologyRules)
 * and only irregulars are curated:
 *   tools/core-vocabulary.csv (lang=en) + tools/lexicon-build-en/{irregular,non_attributive,articles}_en.csv
 *   + verb transitivity inherited from the shared pictogram's Italian verb (verbs_it.csv)
 *     -> app/src/main/assets/lexicon/lexicon_en.json (+ LICENSE, PROVENANCE.md)
 */
private const val MODAL_LEMMAS = "can,must"
private val MODALS = MODAL_LEMMAS.split(",").toSet()

fun main() {
    val repoRoot = findRepoRoot(Paths.get("").toAbsolutePath())
    val dir = repoRoot.resolve("tools/lexicon-build-en")
    val vocabCsv = repoRoot.resolve("tools/core-vocabulary.csv")

    val enRows = parseVocabulary(vocabCsv, "en")
    val transitiveById = transitivityFromItalian(vocabCsv, repoRoot.resolve("tools/lexicon-build/verbs_it.csv"))
    val irregularVerbs = parseIrregularVerbs(dir.resolve("irregular_verbs_en.csv"))
    val irregularPlurals = parseIrregularPlurals(dir.resolve("irregular_nouns_en.csv"))
    val nonAttributive = parseSet(dir.resolve("non_attributive_en.csv"))
    val articles = parseArticles(dir.resolve("articles_en.csv"))

    val entries = mutableListOf<EnEntry>()
    val unsupported = mutableListOf<String>()

    for (row in enRows) {
        // Multi-word lemmas ("brush teeth") cannot be inflected word-by-word -> the engine
        // uses them verbatim (concat), so they are unsupported here.
        if (row.lemma.contains(' ') && row.pos in setOf("VERB", "NOUN", "DESCRIPTOR")) {
            unsupported += row.lemma
            continue
        }
        when (row.pos) {
            "VERB" -> {
                // Transitivity is inherited from the shared pictogram's Italian verb; a few
                // English verbs map to a multi-word Italian phrase (pee <- fare pipì) with no
                // verbs_it entry — default to intransitive (the safe, never-guess choice).
                val transitive = transitiveById[row.arasaacId] ?: false
                entries += buildVerb(row.lemma, irregularVerbs[row.lemma], transitive)
            }
            "NOUN" -> entries += buildNoun(row.lemma, irregularPlurals[row.lemma], articles[row.lemma])
            "DESCRIPTOR" ->
                if (row.lemma in nonAttributive) {
                    unsupported += row.lemma
                } else {
                    entries += EnAdjective(row.lemma, row.lemma, articles[row.lemma])
                }
            else -> Unit // MISC/SOCIAL/PROPER_NAME need no morphology
        }
    }

    val assetsDir = repoRoot.resolve("app/src/main/assets/lexicon")
    Files.createDirectories(assetsDir)
    val lexicon = EnglishLexicon(language = "en", entries = entries, unsupported = unsupported.sorted())
    Files.writeString(assetsDir.resolve("lexicon_en.json"), Json { prettyPrint = true }.encodeToString(lexicon))
    writeProvenance(assetsDir)

    val verbs = entries.count { it is EnVerb }
    val nouns = entries.count { it is EnNoun }
    val adjectives = entries.count { it is EnAdjective }
    println("lexicon_en.json: ${entries.size} entries ($verbs verbs, $nouns nouns, $adjectives adjectives)")
    println("unsupported (concat fallback): ${unsupported.size} -> ${unsupported.sorted().joinToString(", ")}")
}

private fun buildVerb(
    lemma: String,
    irregular: IrregularVerb?,
    transitive: Boolean,
): EnVerb =
    EnVerb(
        lemma = lemma,
        base = lemma,
        thirdSingular = irregular?.thirdSingular ?: EnglishMorphologyRules.thirdSingular(lemma),
        past = irregular?.past ?: EnglishMorphologyRules.past(lemma),
        pastParticiple = irregular?.pastParticiple ?: EnglishMorphologyRules.past(lemma),
        transitive = transitive,
        modal = lemma in MODALS,
    )

private fun buildNoun(
    lemma: String,
    irregularPlural: String?,
    article: String?,
): EnNoun =
    EnNoun(
        lemma = lemma,
        singular = lemma,
        plural = irregularPlural ?: EnglishMorphologyRules.plural(lemma),
        indefinite = article,
    )

private fun writeProvenance(assetsDir: Path) {
    Files.writeString(
        assetsDir.resolve("PROVENANCE_en.md"),
        """
        # English lexicon provenance

        English has no free morphological database equivalent to Morph-it!, so the English
        lexicon is generated by RULE plus curated exceptions, fully reproducible with
        `./gradlew :tools:lexicon-build:runEn`:
        - Vocabulary: tools/core-vocabulary.csv (lang=en) — the same ARASAAC pictograms as
          the Italian pack, with English labels.
        - Regular inflection: computed by EnglishMorphologyRules (3sg, past, plural).
        - Irregulars: tools/lexicon-build-en/irregular_verbs_en.csv and
          irregular_nouns_en.csv (hand-curated, linguist-reviewable).
        - Verb transitivity: inherited from the shared pictogram's Italian verb
          (tools/lexicon-build/verbs_it.csv) — the same action has the same transitivity.
        - Non-attributive descriptors and a/an exceptions:
          tools/lexicon-build-en/non_attributive_en.csv, articles_en.csv.

        The generated lexicon carries no third-party data license (it is derived from the
        curated word lists and rules in this repository); it is covered by the code license.
        """.trimIndent() + "\n",
    )
}

private fun findRepoRoot(start: Path): Path {
    var dir: Path? = start
    while (dir != null) {
        if (dir.resolve("settings.gradle.kts").exists()) return dir
        dir = dir.parent
    }
    return fail("run from inside the repository")
}

private fun fail(message: String): Nothing {
    System.err.println("ERROR: $message")
    exitProcess(1)
}
