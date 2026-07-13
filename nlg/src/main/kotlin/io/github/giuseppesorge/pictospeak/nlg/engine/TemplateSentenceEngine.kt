package io.github.giuseppesorge.pictospeak.nlg.engine

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.lang.LanguageRealizer

/**
 * Template-based sentence engine (docs/grammar-v1.md): delegates realization to a
 * [LanguageRealizer] and ALWAYS appends the concat fallback as the last candidate, so the
 * user can select the literal sequence and unmatched input still yields a proposal. The
 * engine is language-agnostic — it never names a lexicon type; each realizer owns its own.
 * A realizer failure degrades to concat (carried in the trace for golden tests, never shown).
 */
class TemplateSentenceEngine(
    private val realizer: LanguageRealizer?,
) : SentenceEngine {
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

    companion object {
        /** Builds the engine for a LanguagePack from its `lexicon_<code>.json` contents. */
        fun forLanguage(
            language: String,
            lexiconJson: String? = null,
        ): TemplateSentenceEngine = TemplateSentenceEngine(LanguageRealizer.forLanguage(language, lexiconJson))
    }
}
