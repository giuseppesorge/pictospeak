package io.github.giuseppesorge.pictospeak.nlg.api

/**
 * Optional asynchronous enhancement layer (on-device LLM).
 *
 * Contract (CLAUDE.md hard rule 3):
 * - The result is only ever APPENDED to the candidate list as an extra, labeled proposal.
 *   It never replaces, reorders, or auto-selects the template candidates.
 * - Callers bound it with a timeout; a null result means "no proposal" and is normal.
 * - Implementations live in :llm and must be deletable without a trace.
 */
interface SentenceRefiner {
    suspend fun refine(
        tokens: List<PictogramToken>,
        baseline: SentenceCandidate,
    ): SentenceCandidate?
}
