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
 */
object DeviceGate {
    /**
     * Conservative default RAM floor for the *visible* feature. The plan calls for ≥3.5–4 GB
     * for 0.6B-class models; a lower threshold for the 270M model is an OUTPUT of the M6
     * experiment, not an assumption — lower this only once measured on the floor tablet.
     */
    const val DEFAULT_MIN_TOTAL_MEM_BYTES = 3_500_000_000L

    /** The runtime ships no 32-bit libraries, so arm64 is mandatory (docs/adr/0004). */
    private const val REQUIRED_ABI = "arm64-v8a"

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
