package io.github.giuseppesorge.pictospeak.nlg.engine

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine

/**
 * Template-based sentence engine.
 *
 * M0 skeleton: only the concat fallback is implemented — it is the permanent safety net
 * for unmatched input, so it comes first. The per-language pipeline
 * (SlotTagger → PatternMatcher → Realizer → CandidateBuilder, docs/grammar-v1.md)
 * lands at M3 behind this same interface, keyed by [language].
 */
class TemplateSentenceEngine(
    @Suppress("UnusedPrivateProperty") // consumed when the per-language Realizer lands (M3)
    private val language: String,
) : SentenceEngine {
    override fun propose(tokens: List<PictogramToken>): List<SentenceCandidate> {
        if (tokens.isEmpty()) return emptyList()
        return listOf(fallbackConcat(tokens))
    }

    /** Plain label concatenation — never a crash, never a guess. */
    private fun fallbackConcat(tokens: List<PictogramToken>): SentenceCandidate =
        SentenceCandidate(
            text = tokens.joinToString(separator = " ") { it.label },
            source = CandidateSource.FALLBACK_CONCAT,
            trace = "fallback-concat(${tokens.size})",
        )
}
