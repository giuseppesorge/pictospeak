package io.github.giuseppesorge.pictospeak.nlg.lang.en

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The English LanguagePack's lexicon — English-specific by design (no grammatical gender;
 * a two-way present agreement plus the irregular copula; invariable adjectives). All forms
 * are precomputed offline by tools/lexicon-build-en, so the realizer only looks them up.
 * Loaded from assets/lexicon/lexicon_en.json.
 */
@Serializable
data class EnglishLexicon(
    val schemaVersion: Int = 1,
    val language: String,
    val entries: List<EnEntry>,
    val unsupported: List<String>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(lexiconJson: String?): EnglishLexicon =
            lexiconJson?.let { json.decodeFromString<EnglishLexicon>(it) }
                ?: EnglishLexicon(language = "en", entries = emptyList(), unsupported = emptyList())
    }
}

@Serializable
sealed interface EnEntry {
    val lemma: String
}

@Serializable
@SerialName("noun")
data class EnNoun(
    override val lemma: String,
    val singular: String,
    /** Null for uncountable/invariable nouns. */
    val plural: String?,
    /** Explicit "a"/"an" when the sound contradicts spelling ("an hour"); null = by rule. */
    val indefinite: String? = null,
) : EnEntry

@Serializable
@SerialName("verb")
data class EnVerb(
    override val lemma: String,
    val base: String,
    val thirdSingular: String,
    val past: String,
    val pastParticiple: String,
    val transitive: Boolean = false,
    /**
     * True for auxiliary-like modals (can, must): bare infinitive complement, no -s
     * inflection, direct "not" negation (no do-support). "want" is NOT a modal — it takes a
     * "to"-infinitive and normal do-support.
     */
    val modal: Boolean = false,
) : EnEntry

@Serializable
@SerialName("adjective")
data class EnAdjective(
    override val lemma: String,
    /** English descriptive adjectives are invariable; kept for symmetry and a/an sound. */
    val word: String,
    /** Explicit "a"/"an" when the sound contradicts spelling ("an old", "a European"). */
    val indefinite: String? = null,
) : EnEntry
