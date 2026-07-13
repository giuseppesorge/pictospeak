package io.github.giuseppesorge.pictospeak.nlg.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Morphology data the sentence engine needs and the pictogram catalog lacks.
 * Facts that are not rule-derivable (verb auxiliaries, irregular forms) live HERE as data,
 * never as code (docs/grammar-v1.md). Produced offline by tools/lexicon-build.
 *
 * [unsupported] lists catalog lemmas that deliberately have no entry (multi-word items,
 * function words, numbers): the engine must degrade to citation form for them — never guess.
 */
@Serializable
data class Lexicon(
    val schemaVersion: Int = 1,
    val language: String,
    val entries: List<LexiconEntry>,
    val unsupported: List<String>,
)

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
