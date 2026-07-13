package io.github.giuseppesorge.pictospeak.llm

import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner

/**
 * The ONLY class allowed to import com.google.ai.edge.litertlm (docs/adr/0004).
 * Swap/removal procedure: replace this file; the rest of the app depends only on
 * [SentenceRefiner].
 *
 * Inert until the model-lab experiment wires the engine (model import via SAF,
 * CPU backend, short prompt, ~25-token cap, kept warm, released on memory pressure —
 * docs/llm-experiment.md). Returning null means "no proposal", which callers treat
 * as normal.
 */
class LiteRtSentenceRefiner : SentenceRefiner {
    override suspend fun refine(
        tokens: List<PictogramToken>,
        baseline: SentenceCandidate,
    ): SentenceCandidate? = null
}
