package io.github.giuseppesorge.pictospeak.llm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.SessionConfig
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * The ONLY class allowed to import com.google.ai.edge.litertlm (docs/adr/0004). Swap/removal
 * procedure: replace this file; the rest of the app depends only on [SentenceRefiner].
 *
 * Threading model (all native access is confined to ONE dedicated thread, so the engine and
 * sessions are never touched from two threads):
 * - refine() runs on a single-thread executor. That thread owns the [Engine] (built lazily,
 *   kept warm) and every session — inference is naturally serialized, no lock needed.
 * - Using a dedicated thread (not Dispatchers.Default) means a slow generation can't starve
 *   the shared pool that the template engine also uses.
 * - close() is NON-BLOCKING: it flags the refiner released and posts the engine teardown to the
 *   same thread (so it runs after any in-flight generation, on the owning thread). It never
 *   blocks the caller — critically, never the main thread from onTrimMemory.
 *
 * Behaviour (docs/llm-experiment.md, CLAUDE.md hard rule 3): loads the SAF-imported `.litertlm`
 * on the CPU backend; NEVER throws (any error degrades to null, "no proposal"); the result is
 * only ever an appended, LLM-labeled extra candidate. Prompt/response logic lives in the pure,
 * JVM-tested [LlmRewrite]; this class is only the native-runtime glue.
 */
class LiteRtSentenceRefiner(
    private val modelPath: String,
    private val maxTokens: Int = DEFAULT_MAX_TOKENS,
) : SentenceRefiner,
    AutoCloseable {
    private val inferenceExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "llm-inference") }
    private val inferenceDispatcher = inferenceExecutor.asCoroutineDispatcher()

    // Only ever read/written on the inference thread, except the volatile released flag.
    private var engine: Engine? = null

    @Volatile
    private var released = false

    override suspend fun refine(
        tokens: List<PictogramToken>,
        baseline: SentenceCandidate,
    ): SentenceCandidate? {
        if (released) return null
        val prompt = LlmRewrite.buildPrompt(tokens, baseline)
        // runCatching also absorbs a RejectedExecutionException if close() raced ahead.
        val raw =
            runCatching {
                withContext(inferenceDispatcher) {
                    val ready = engine ?: buildEngine().also { engine = it }
                    ready.createSession(SessionConfig()).use { session ->
                        session.generateContent(listOf(InputData.Text(prompt)))
                    }
                }
            }.getOrNull()
        return raw?.let { LlmRewrite.cleanResponse(it, baseline) }?.let { LlmRewrite.candidate(it) }
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

    /**
     * Release native memory. Non-blocking: the teardown is queued on the inference thread so it
     * runs after any in-flight generation and never on the caller's (possibly main) thread.
     */
    override fun close() {
        released = true
        runCatching {
            inferenceExecutor.execute {
                runCatching { engine?.close() }
                engine = null
            }
            inferenceExecutor.shutdown()
        }
    }

    private companion object {
        // Bounds total context (prompt ~40 tok + output). Kept small so a base model that
        // "rambles" can't run for minutes on a low-end CPU — the whole feature has a few-second
        // interactive budget. A fine-tuned model emits one sentence + EOS and stops well short.
        const val DEFAULT_MAX_TOKENS = 128
    }
}
