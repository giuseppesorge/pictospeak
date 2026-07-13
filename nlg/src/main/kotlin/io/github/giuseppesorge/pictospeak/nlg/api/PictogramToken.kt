package io.github.giuseppesorge.pictospeak.nlg.api

import kotlinx.serialization.Serializable

/**
 * Part of speech, mapped from ARASAAC keyword type codes
 * (1=proper name, 2=common noun, 3=verb, 4=descriptive adj/adv, 5=social, 6=misc).
 * The lexicon entry is authoritative when it disagrees with the catalog.
 */
enum class Pos {
    PROPER_NAME,
    NOUN,
    VERB,
    DESCRIPTOR,
    SOCIAL,
    MISC,
    ;

    companion object {
        // The literals ARE the documented ARASAAC protocol codes (see KDoc above).
        @Suppress("MagicNumber")
        fun fromArasaacTypeCode(code: Int): Pos =
            when (code) {
                1 -> PROPER_NAME
                2 -> NOUN
                3 -> VERB
                4 -> DESCRIPTOR
                5 -> SOCIAL
                else -> MISC
            }
    }
}

/**
 * One selected pictogram in the message strip.
 *
 * @property id stable catalog id (ARASAAC pictogram id once the asset pipeline lands)
 * @property lemma citation form used as the lexicon key
 * @property pos coarse part of speech (see [Pos])
 * @property label text shown under the pictogram and used verbatim by the concat fallback
 */
@Serializable
data class PictogramToken(
    val id: String,
    val lemma: String,
    val pos: Pos,
    val label: String,
)
