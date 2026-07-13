package io.github.giuseppesorge.pictospeak.speech

import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate

/**
 * The single factory for [ConfirmedUtterance] (INVARIANT-1).
 *
 * Call sites are audited: there must be exactly ONE, inside the user-confirmation tap
 * handler of the board UI. Every future speech feature (quick phrases, favorites,
 * repeat-last) must pass through here — PRs adding another speech entry point are rejected.
 */
object ConfirmationGate {
    fun confirm(candidate: SentenceCandidate): ConfirmedUtterance =
        ConfirmedUtterance(text = candidate.text, sourceTrace = candidate.trace)
}
