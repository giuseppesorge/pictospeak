# ADR-0010: INVARIANT-1 — confirm-before-speak, enforced structurally

**Status**: accepted (non-negotiable)

## Context
An AAC device must say what the person composed — not what a generator inferred. This is a
domain-safety requirement, not a UX preference.

## Decision
TtsGateway.speak() accepts only ConfirmedUtterance: internal constructor in :speech, sole
mint ConfirmationGate.confirm(), exactly one call site (the user's confirmation tap),
non-serializable, never persisted. No dependency edge from :nlg or :llm to :speech.
Recording-fake tests assert zero speak() calls without an explicit confirm. speak(String)
must never exist. The single documented exception is speakWordPreview: user-initiated
speech of exactly the tapped pictogram label, profile-gated, default off.
