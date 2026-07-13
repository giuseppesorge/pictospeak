package io.github.giuseppesorge.pictospeak.llm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.SessionConfig
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/**
 * The ONLY class allowed to import com.google.ai.edge.litertlm (docs/adr/0004). Swap/removal
 * procedure: replace this file; the rest of the app depends only on [SentenceRefiner].
 *
 * Behaviour (docs/llm-experiment.md, CLAUDE.md hard rule 3):
 * - Loads the SAF-imported `.litertlm` model on the CPU backend, lazily on first use, and
 *   keeps the [Engine] warm for subsequent refinements (load is the expensive step).
 * - One inference at a time (a native session is not shared across threads); callers already
 *   cancel stale jobs and bound this with a timeout.
 * - NEVER throws: any runtime/model error degrades to null ("no proposal"), exactly like the
 *   inert stub it replaces. The template candidates are always already published.
 * - The result is only ever an appended, LLM-labeled extra candidate — never a replacement.
 *
 * Prompt construction and response cleaning live in the pure, JVM-tested [LlmRewrite]; this
 * class is only the native-runtime glue.
 */
class LiteRtSentenceRefiner(
    private val modelPath: String,
    private val maxTokens: Int = DEFAULT_MAX_TOKENS,
    private val inferenceDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SentenceRefiner,
    AutoCloseable {
    // Guards the engine handle and serializes inference. Held across the blocking native
    // call (always on [inferenceDispatcher]); close() therefore waits for any in-flight
    // generation before releasing, avoiding a native use-after-free.
    private val engineLock = Any()

    private var engine: Engine? = null

    override suspend fun refine(
        tokens: List<PictogramToken>,
        baseline: SentenceCandidate,
    ): SentenceCandidate? =
        withContext(inferenceDispatcher) {
            val prompt = LlmRewrite.buildPrompt(tokens, baseline)
            val job = coroutineContext[Job]
            val raw =
                runCatching {
                    synchronized(engineLock) {
                        val ready = engine ?: buildEngine().also { engine = it }
                        ready.createSession(SessionConfig()).use { session ->
                            // The native generateContent is a blocking, non-interruptible call.
                            // If the caller's timeout cancels this coroutine, stop the native
                            // generation so it releases the engine lock in seconds, not minutes,
                            // instead of running the model to its token cap in the background.
                            val cancel =
                                job?.invokeOnCompletion { cause ->
                                    if (cause != null) runCatching { session.cancelProcess() }
                                }
                            try {
                                session.generateContent(listOf(InputData.Text(prompt)))
                            } finally {
                                cancel?.dispose()
                            }
                        }
                    }
                }.getOrNull() ?: return@withContext null
            LlmRewrite.cleanResponse(raw, baseline)?.let { LlmRewrite.candidate(it) }
        }

    private fun buildEngine(): Engine =
        Engine(
            EngineConfig(
                modelPath = modelPath,
                // CPU only: the runtime ships no GPU/NPU path we rely on, and the device gate
                // already guarantees arm64 (docs/adr/0004, docs/llm-experiment.md).
                backend = Backend.CPU(),
                maxNumTokens = maxTokens,
            ),
        ).apply { initialize() }

    /** Release native memory (call on low-memory / when the feature is turned off). */
    override fun close() {
        synchronized(engineLock) {
            runCatching { engine?.close() }
            engine = null
        }
    }

    private companion object {
        // Bounds total context (prompt ~40 tok + output). Kept small so a base model that
        // "rambles" can't run for minutes on a low-end CPU — the whole feature has a few-second
        // interactive budget. A fine-tuned model emits one sentence + EOS and stops well short.
        const val DEFAULT_MAX_TOKENS = 128
    }
}
