package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.llm.LiteRtSentenceRefiner
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner

/**
 * play flavor: the refiner exists but is inert until the M6 experiment wires LiteRT-LM
 * (docs/adr/0004). Device capability gating (RAM, 64-bit ABI, low-RAM flag) arrives with it.
 */
object RefinerFactory {
    fun create(): SentenceRefiner? = LiteRtSentenceRefiner()
}
