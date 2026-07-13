package io.github.giuseppesorge.pictospeak.speech

/**
 * INVARIANT-1: the ONLY value [TtsGateway.speak] accepts.
 *
 * The constructor is internal to :speech and the sole mint is [ConfirmationGate.confirm],
 * which must be called from exactly one place: the user's explicit confirmation tap
 * handler. This type is deliberately NOT serializable and must never be persisted —
 * a confirmation can never be replayed from disk.
 */
class ConfirmedUtterance internal constructor(
    val text: String,
    /** Trace of the confirmed candidate, for logging/tests only. */
    val sourceTrace: String,
)
