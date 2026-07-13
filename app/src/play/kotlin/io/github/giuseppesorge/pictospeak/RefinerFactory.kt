package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.llm.LiteRtSentenceRefiner
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner

/**
 * play flavor: the refiner exists and wires LiteRT-LM (docs/adr/0004), but is only built once
 * every gate has passed — the device gate, the play flavor, the profile opt-in, an imported
 * model and its accepted license (all combined into [gatePassed] by AppContainer). Returns
 * null otherwise, which callers treat as "no LLM" — templates remain the product.
 */
object RefinerFactory {
    /** This build can run the LLM: the settings UI shows the section (gated further at runtime). */
    const val LLM_FLAVOR = true

    fun create(
        modelPath: String?,
        gatePassed: Boolean,
    ): SentenceRefiner? = if (gatePassed && modelPath != null) LiteRtSentenceRefiner(modelPath) else null
}
