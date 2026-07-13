package io.github.giuseppesorge.pictospeak.nlg.lang

import io.github.giuseppesorge.pictospeak.nlg.api.Lexicon
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.lang.it.ItalianRealizer

/**
 * One realizer per LanguagePack (docs/language-packs.md). A realizer turns a telegraphic
 * token sequence into grammatical sentence candidates for ITS language only.
 *
 * Contract (docs/grammar-v1.md):
 * - Return an empty list when no frozen-scope pattern matches — the engine appends the
 *   concat fallback unconditionally, so "no match" must never be an exception.
 * - Missing morphology data degrades to citation form or to no-match. Never guess.
 * - Candidate order: default first, then user-cyclable alternatives.
 */
interface LanguageRealizer {
    fun realize(tokens: List<PictogramToken>): List<SentenceCandidate>

    companion object {
        /** Unknown languages get no realizer: the engine is then concat-only. */
        fun forLanguage(
            language: String,
            lexicon: Lexicon,
        ): LanguageRealizer? =
            when (language) {
                "it" -> ItalianRealizer(lexicon)
                else -> null
            }
    }
}
