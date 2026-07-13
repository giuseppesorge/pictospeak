package io.github.giuseppesorge.pictospeak.llm

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate

/**
 * Pure prompt construction and response cleaning for the LLM refiner — kept separate from
 * the LiteRT-LM runtime glue so it is JVM-unit-testable without the native engine.
 *
 * The prompt is language-neutral: it never names a language, it just asks the model to
 * rewrite the telegraphic words into one natural sentence "in the same language", anchored
 * on the template draft (which is already in the target language). This keeps the refiner
 * language-agnostic, exactly like the engine core.
 *
 * The fine-tuned model (docs/llm-experiment.md) MUST be trained with this same prompt shape.
 */
object LlmRewrite {
    /** Hard ceiling on a usable rewrite — anything longer is treated as the model rambling. */
    const val MAX_SENTENCE_CHARS = 160

    fun buildPrompt(
        tokens: List<PictogramToken>,
        baseline: SentenceCandidate,
    ): String {
        val words = tokens.joinToString(separator = " ") { it.label }
        return buildString {
            appendLine(
                "Rewrite the words into one short, natural sentence in the same language. " +
                    "Keep it faithful to the words, add only function words. " +
                    "Reply with only the sentence, nothing else.",
            )
            appendLine("Words: $words")
            appendLine("Draft: ${baseline.text}")
            append("Sentence:")
        }
    }

    /**
     * Normalize a raw model completion into a single usable sentence, or null if there is
     * nothing worth appending. Never throws.
     *
     * Returns null when the cleaned text is blank, implausibly long, or merely echoes the
     * template draft (no point offering a duplicate candidate).
     */
    fun cleanResponse(
        raw: String,
        baseline: SentenceCandidate,
    ): String? {
        val firstLine =
            raw
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                ?: return null
        val collapsed =
            firstLine
                .removePrefixesIgnoreCase(RESPONSE_PREFIXES)
                .trim { it in TRIM_CHARS }
                .replace(WHITESPACE, " ")
                .trim()
        val usable =
            collapsed.isNotEmpty() &&
                collapsed.length <= MAX_SENTENCE_CHARS &&
                !collapsed.equalsNormalized(baseline.text)
        return collapsed.takeIf { usable }
    }

    /** The extra candidate to append — always labeled LLM so the UI can mark it (hard rule 3). */
    fun candidate(text: String): SentenceCandidate =
        SentenceCandidate(text = text, source = CandidateSource.LLM, trace = "llm-refine")

    private val WHITESPACE = Regex("\\s+")

    // Straight and typographic quotes, plus stray list bullets a small model might emit.
    private const val TRIM_CHARS = "\"'“”‘’`-•* "

    private val RESPONSE_PREFIXES = listOf("Sentence:", "Rewrite:", "Output:", "Answer:")

    private fun String.removePrefixesIgnoreCase(prefixes: List<String>): String {
        for (p in prefixes) {
            if (startsWith(p, ignoreCase = true)) return substring(p.length)
        }
        return this
    }

    private fun String.equalsNormalized(other: String): Boolean =
        trim().trimEnd('.', '!', '?', ' ').equals(other.trim().trimEnd('.', '!', '?', ' '), ignoreCase = true)
}
