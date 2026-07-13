package io.github.giuseppesorge.pictospeak.nlg.api

/** Where a candidate came from. The UI must always label non-template sources. */
enum class CandidateSource {
    TEMPLATE,
    FALLBACK_CONCAT,
    LLM,
}

/**
 * A sentence proposal shown to the user. Proposals are NEVER spoken directly:
 * speech requires explicit user confirmation through :speech's ConfirmationGate.
 *
 * @property trace pattern id + morphology decisions, for debugging and golden tests only —
 *   never shown to users.
 */
data class SentenceCandidate(
    val text: String,
    val source: CandidateSource,
    val trace: String,
)
