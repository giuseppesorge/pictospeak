package io.github.giuseppesorge.pictospeak.nlg.lang.it

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The Italian LanguagePack's morphology data — Italian-specific by design (gender, the
 * essere/avere auxiliary, six-person conjugation). Each language owns its own lexicon
 * shape; the only cross-language contract is [io.github.giuseppesorge.pictospeak.nlg.lang.LanguageRealizer].
 * Produced offline by tools/lexicon-build; loaded from assets/lexicon/lexicon_it.json.
 *
 * [unsupported] lists catalog lemmas that deliberately have no entry (multi-word items,
 * function words, numbers): the engine degrades to citation form for them — never guesses.
 */
@Serializable
data class ItalianLexicon(
    val schemaVersion: Int = 1,
    val language: String,
    val entries: List<LexiconEntry>,
    val unsupported: List<String>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(lexiconJson: String?): ItalianLexicon =
            lexiconJson?.let { json.decodeFromString<ItalianLexicon>(it) }
                ?: ItalianLexicon(language = "it", entries = emptyList(), unsupported = emptyList())
    }
}

@Serializable
sealed interface LexiconEntry {
    val lemma: String
}

enum class Gender { MASCULINE, FEMININE }

enum class Auxiliary { ESSERE, AVERE }

/** Grammatical person+number, in conventional order. */
enum class Person {
    FIRST_SINGULAR,
    SECOND_SINGULAR,
    THIRD_SINGULAR,
    FIRST_PLURAL,
    SECOND_PLURAL,
    THIRD_PLURAL,
}

@Serializable
@SerialName("noun")
data class NounEntry(
    override val lemma: String,
    val gender: Gender,
    val singular: String,
    /** Null for invariable nouns. */
    val plural: String?,
) : LexiconEntry

@Serializable
@SerialName("verb")
data class VerbEntry(
    override val lemma: String,
    val auxiliary: Auxiliary,
    val pastParticiple: String,
    /** Present indicative, all six persons. */
    val presentIndicative: Map<Person, String>,
    /**
     * Whether the verb takes a bare direct object. Intransitive verbs (andare, dormire…)
     * take prepositional complements ("vado A casa"), which grammar v1 does not build — so
     * an object noun after an intransitive verb must degrade to concat, never become a
     * guessed direct object ("vado la casa"). Curated data, not rule-derivable.
     */
    val transitive: Boolean = false,
    /**
     * Reflexive verbs conjugate with clitics, which grammar v1 excludes — the engine uses
     * citation form for them until clitics enter the grammar scope (requires an ADR).
     */
    val reflexive: Boolean = false,
) : LexiconEntry

@Serializable
@SerialName("adjective")
data class AdjectiveEntry(
    override val lemma: String,
    val masculineSingular: String,
    val feminineSingular: String,
    val masculinePlural: String,
    val femininePlural: String,
) : LexiconEntry
