package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner

/**
 * foss flavor: there is no LLM. No :llm dependency exists in this binary at all —
 * CI asserts the APK contains no liblitertlm (CLAUDE.md hard rule 3).
 */
object RefinerFactory {
    fun create(): SentenceRefiner? = null
}
