package io.github.giuseppesorge.pictospeak.llm

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Result of the on-device capability check that guards the optional LLM feature — computed
 * once, exposed to the settings UI so a caregiver can see *why* the feature is or isn't
 * available (docs/llm-experiment.md, "device gate").
 *
 * This type lives in `:app` and imports NOTHING from `:llm` — the gate must be evaluable in
 * the foss flavor too (where it simply never matters), and it never touches LiteRT-LM.
 */
data class DeviceCapability(
    val supportsArm64: Boolean,
    val isLowRamDevice: Boolean,
    val totalMemBytes: Long,
    val minTotalMemBytes: Long,
) {
    /** All three conditions must hold before the LLM feature is even offered. */
    val eligible: Boolean
        get() = supportsArm64 && !isLowRamDevice && totalMemBytes >= minTotalMemBytes

    val totalMemGib: Double
        get() = totalMemBytes.toDouble() / BYTES_PER_GIB

    val minTotalMemGib: Double
        get() = minTotalMemBytes.toDouble() / BYTES_PER_GIB

    private companion object {
        const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0
    }
}

/**
 * Pure, testable device-gate logic plus the thin Android reader. The gate is the FIRST of
 * several conditions (also: play flavor ∧ profile opt-in ∧ a model imported ∧ its license
 * accepted) — see AppContainer.
 *
 * Minimum characteristics to run a free, fully-offline on-device LLM (docs/llm-experiment.md):
 * - **arm64-v8a** ABI (the LiteRT-LM runtime ships no 32-bit libraries) — hard requirement.
 * - **not** flagged `isLowRamDevice`.
 * - RAM ≥ an absolute floor AND ≥ a multiple of the imported model's size. The RAM need is
 *   driven by the MODEL, not a fixed number: a ~300 MB Gemma-3-270M needs far less headroom
 *   than a ~1 GB Qwen3-0.6B, so we scale the requirement to the actual file ([fitsModel]).
 * There is NO monetary cost and no network at any point — the model runs locally.
 */
object DeviceGate {
    /**
     * Absolute RAM floor below which the feature is never offered, regardless of model size —
     * the OS + app + any LLM need some headroom. The 2 GB floor tablet is intentionally below
     * this until a soak test proves a tiny model survives its low-memory killer (an OUTPUT of
     * the M6 experiment, not an assumption).
     */
    const val DEFAULT_MIN_TOTAL_MEM_BYTES = 3_000_000_000L

    /**
     * Total RAM must be at least this multiple of the model file size. The model's peak
     * resident set is roughly its weights + KV cache/activations (≈2× for our short prompts),
     * and that peak should stay near half of total RAM (the go/no-go RSS budget) → ~4×.
     */
    const val RAM_TO_MODEL_FACTOR = 4L

    /** The runtime ships no 32-bit libraries, so arm64 is mandatory (docs/adr/0004). */
    private const val REQUIRED_ABI = "arm64-v8a"

    /**
     * Does this device have enough RAM for a model of [modelSizeBytes]? Combines the absolute
     * floor with the model-size-relative headroom, so a bigger model needs a bigger device.
     */
    fun fitsModel(
        totalMemBytes: Long,
        modelSizeBytes: Long,
    ): Boolean = totalMemBytes >= maxOf(DEFAULT_MIN_TOTAL_MEM_BYTES, modelSizeBytes * RAM_TO_MODEL_FACTOR)

    fun evaluate(
        supportsArm64: Boolean,
        isLowRamDevice: Boolean,
        totalMemBytes: Long,
        minTotalMemBytes: Long = DEFAULT_MIN_TOTAL_MEM_BYTES,
    ): DeviceCapability =
        DeviceCapability(
            supportsArm64 = supportsArm64,
            isLowRamDevice = isLowRamDevice,
            totalMemBytes = totalMemBytes,
            minTotalMemBytes = minTotalMemBytes,
        )

    fun forDevice(
        context: Context,
        minTotalMemBytes: Long = DEFAULT_MIN_TOTAL_MEM_BYTES,
    ): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val supportsArm64 = Build.SUPPORTED_64_BIT_ABIS.any { it == REQUIRED_ABI }
        return evaluate(
            supportsArm64 = supportsArm64,
            isLowRamDevice = activityManager.isLowRamDevice,
            totalMemBytes = memoryInfo.totalMem,
            minTotalMemBytes = minTotalMemBytes,
        )
    }
}
