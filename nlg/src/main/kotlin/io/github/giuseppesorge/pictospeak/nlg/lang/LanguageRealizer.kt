package io.github.giuseppesorge.pictospeak.nlg.lang

import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnglishLexicon
import io.github.giuseppesorge.pictospeak.nlg.lang.en.EnglishRealizer
import io.github.giuseppesorge.pictospeak.nlg.lang.it.ItalianLexicon
import io.github.giuseppesorge.pictospeak.nlg.lang.it.ItalianRealizer

/**
 * One realizer per LanguagePack (docs/language-packs.md). A realizer turns a telegraphic
 * token sequence into grammatical sentence candidates for ITS language only.
 *
 * This interface is the ONLY cross-language contract. Each pack owns its own lexicon shape
 * and parses its own JSON here — adding a language is a new `lang/<code>/` package plus one
 * `when` arm below; the engine core and the other packs stay untouched.
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
        /**
         * @param lexiconJson the pack's `lexicon_<code>.json` asset contents, or null (then
         *   the realizer is data-empty and every sentence degrades to concat).
         * @return the realizer for [language], or null for an unknown language (concat-only).
         */
        fun forLanguage(
            language: String,
            lexiconJson: String?,
        ): LanguageRealizer? =
            when (language) {
                "it" -> ItalianRealizer(ItalianLexicon.parse(lexiconJson))
                "en" -> EnglishRealizer(EnglishLexicon.parse(lexiconJson))
                else -> null
            }
    }
}
