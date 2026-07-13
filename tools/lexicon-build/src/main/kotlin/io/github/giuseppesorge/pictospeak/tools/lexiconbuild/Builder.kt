package io.github.giuseppesorge.pictospeak.tools.lexiconbuild

import io.github.giuseppesorge.pictospeak.nlg.api.AdjectiveEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Auxiliary
import io.github.giuseppesorge.pictospeak.nlg.api.Gender
import io.github.giuseppesorge.pictospeak.nlg.api.LexiconEntry
import io.github.giuseppesorge.pictospeak.nlg.api.NounEntry
import io.github.giuseppesorge.pictospeak.nlg.api.Person
import io.github.giuseppesorge.pictospeak.nlg.api.VerbEntry

internal val PERSON_TAGS =
    mapOf(
        "1+s" to Person.FIRST_SINGULAR,
        "2+s" to Person.SECOND_SINGULAR,
        "3+s" to Person.THIRD_SINGULAR,
        "1+p" to Person.FIRST_PLURAL,
        "2+p" to Person.SECOND_PLURAL,
        "3+p" to Person.THIRD_PLURAL,
    )

sealed interface BuildResult {
    data class Entry(
        val entry: LexiconEntry,
    ) : BuildResult

    data object Unsupported : BuildResult

    data class Problem(
        val message: String,
    ) : BuildResult
}

class LexiconBuilder(
    private val byLemma: Map<String, List<MorphRow>>,
    private val auxTable: Map<String, Pair<Auxiliary, Boolean>>,
    private val overrides: Overrides,
) {
    fun build(item: VocabularyItem): BuildResult {
        overrides.entries[item.lemma.lowercase()]?.let { return BuildResult.Entry(it) }
        return when (item.pos) {
            "VERB" -> verb(item.lemma)
            "NOUN" -> noun(item.lemma)
            "DESCRIPTOR" -> adjective(item.lemma)
            else -> BuildResult.Unsupported
        }
    }

    @Suppress("ReturnCount") // early-return validation style is deliberate
    private fun verb(lemma: String): BuildResult {
        val rows = byLemma[baseLemma(lemma.lowercase())].orEmpty().filter { it.features.startsWith("VER:") }
        if (rows.isEmpty()) return BuildResult.Unsupported
        val aux =
            auxTable[lemma.lowercase()]
                ?: return BuildResult.Problem("$lemma: found in Morph-it! but missing from aux_it.csv")
        val formPatches = overrides.verbForms[lemma.lowercase()].orEmpty()
        val present = mutableMapOf<Person, String>()
        for ((tag, person) in PERSON_TAGS) {
            val candidates = rows.filter { it.features == "VER:ind+pres+$tag" }.map { it.form }
            // Poetic truncations (van/son/mangian) are shorter: prefer the longest, then
            // lexicographically last for determinism on equal-length variants. Variant
            // selections the rule gets wrong are patched via overrides_it.csv.
            val patched = formPatches[person]
            if (patched != null && patched !in candidates) {
                return BuildResult.Problem("$lemma: override form '$patched' not in Morph-it! candidates $candidates")
            }
            val choice =
                patched
                    ?: candidates.maxWithOrNull(compareBy({ it.length }, { it }))
                    ?: return BuildResult.Problem("$lemma: missing present indicative $tag in Morph-it!")
            present[person] = choice
        }
        val participle =
            rows
                .filter { it.features == "VER:part+past+s+m" }
                .map { it.form }
                .maxWithOrNull(compareBy({ it.length }, { it }))
                ?: return BuildResult.Problem("$lemma: missing past participle in Morph-it!")
        return BuildResult.Entry(
            VerbEntry(
                lemma = lemma,
                auxiliary = aux.first,
                pastParticiple = participle,
                presentIndicative = present,
                reflexive = aux.second,
            ),
        )
    }

    private fun noun(lemma: String): BuildResult {
        val rows = byLemma[lemma.lowercase()].orEmpty().filter { it.features.startsWith("NOUN-") }
        if (rows.isEmpty()) return BuildResult.Unsupported
        val gender = if (rows.any { it.features.startsWith("NOUN-F") }) Gender.FEMININE else Gender.MASCULINE
        val genderTag = if (gender == Gender.FEMININE) "NOUN-F" else "NOUN-M"
        val singular =
            rows
                .filter { it.features == "$genderTag:s" }
                .map { it.form }
                .maxWithOrNull(compareBy({ it.length }, { it })) ?: lemma
        val plural =
            rows
                .filter { it.features == "$genderTag:p" }
                .map { it.form }
                .maxWithOrNull(compareBy({ it.length }, { it }))
        return BuildResult.Entry(NounEntry(lemma = lemma, gender = gender, singular = singular, plural = plural))
    }

    @Suppress("ReturnCount") // early-return validation style is deliberate
    private fun adjective(lemma: String): BuildResult {
        val rows = byLemma[lemma.lowercase()].orEmpty().filter { it.features.startsWith("ADJ:pos+") }
        if (rows.isEmpty()) return BuildResult.Unsupported

        fun form(tag: String): String? =
            rows
                .filter { it.features == "ADJ:pos+$tag" && !it.form.endsWith("'") }
                .map { it.form }
                .maxWithOrNull(compareBy({ it.length }, { it }))
        val ms = form("m+s") ?: return BuildResult.Unsupported
        return BuildResult.Entry(
            AdjectiveEntry(
                lemma = lemma,
                masculineSingular = ms,
                feminineSingular = form("f+s") ?: ms,
                masculinePlural = form("m+p") ?: ms,
                femininePlural = form("f+p") ?: form("m+p") ?: ms,
            ),
        )
    }
}
