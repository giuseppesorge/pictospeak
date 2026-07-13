package io.github.giuseppesorge.pictospeak

import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner

/**
 * foss flavor: there is no LLM. No `:llm` dependency exists in this binary at all — CI
 * asserts the APK contains no liblitertlm (CLAUDE.md hard rule 3). The signature matches the
 * play flavor so AppContainer is flavor-agnostic; the arguments are simply ignored here.
 */
object RefinerFactory {
    /** This build ships no LLM: the settings UI hides the whole section. */
    const val LLM_FLAVOR = false

    fun create(
        @Suppress("UNUSED_PARAMETER") modelPath: String?,
        @Suppress("UNUSED_PARAMETER") gatePassed: Boolean,
    ): SentenceRefiner? = null
}
