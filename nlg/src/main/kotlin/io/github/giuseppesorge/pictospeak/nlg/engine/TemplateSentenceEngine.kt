package io.github.giuseppesorge.pictospeak.nlg.engine

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.Lexicon
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.lang.LanguageRealizer

/**
 * Template-based sentence engine (docs/grammar-v1.md): delegates realization to the
 * language pack's realizer and ALWAYS appends the concat fallback as the last candidate,
 * so the user can select the literal sequence and unmatched input still yields a proposal.
 * The engine never throws: a realizer failure degrades to concat (and is carried in the
 * trace for golden tests, never shown to users).
 */
class TemplateSentenceEngine(
    language: String,
    lexicon: Lexicon = Lexicon(language = language, entries = emptyList(), unsupported = emptyList()),
) : SentenceEngine {
    private val realizer: LanguageRealizer? = LanguageRealizer.forLanguage(language, lexicon)

    override fun propose(tokens: List<PictogramToken>): List<SentenceCandidate> {
        if (tokens.isEmpty()) return emptyList()
        val realized =
            realizer
                ?.let { r -> runCatching { r.realize(tokens) }.getOrElse { emptyList() } }
                .orEmpty()
        return realized + fallbackConcat(tokens)
    }

    /** Plain label concatenation — never a crash, never a guess. */
    private fun fallbackConcat(tokens: List<PictogramToken>): SentenceCandidate =
        SentenceCandidate(
            text = tokens.joinToString(separator = " ") { it.label },
            source = CandidateSource.FALLBACK_CONCAT,
            trace = "fallback-concat(${tokens.size})",
        )
}
