package io.github.giuseppesorge.pictospeak.nlg.api

/**
 * The rule/template sentence engine — the guaranteed baseline and the product.
 *
 * Contract:
 * - Pure and synchronous; must stay fast enough for every keystroke (≤50ms P95 on the
 *   floor device, docs/perf-budgets.md).
 * - NEVER throws on unmatched input: degrades to a [CandidateSource.FALLBACK_CONCAT]
 *   candidate instead.
 * - Missing morphology data degrades to citation form — the engine never guesses.
 * - Candidate 0 is the default proposal; further candidates are user-cyclable alternatives.
 */
interface SentenceEngine {
    fun propose(tokens: List<PictogramToken>): List<SentenceCandidate>
}
