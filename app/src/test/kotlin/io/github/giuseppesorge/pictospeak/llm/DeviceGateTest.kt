package io.github.giuseppesorge.pictospeak.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceGateTest {
    private val fourGb = 4L * 1024 * 1024 * 1024
    private val twoGb = 2L * 1024 * 1024 * 1024

    @Test
    fun `a capable 4GB arm64 device is eligible`() {
        val cap = DeviceGate.evaluate(supportsArm64 = true, isLowRamDevice = false, totalMemBytes = fourGb)
        assertTrue(cap.eligible)
    }

    @Test
    fun `a 2GB floor device is not eligible`() {
        val cap = DeviceGate.evaluate(supportsArm64 = true, isLowRamDevice = false, totalMemBytes = twoGb)
        assertFalse(cap.eligible)
    }

    @Test
    fun `a low-ram-flagged device is not eligible even with enough RAM`() {
        val cap = DeviceGate.evaluate(supportsArm64 = true, isLowRamDevice = true, totalMemBytes = fourGb)
        assertFalse(cap.eligible)
    }

    @Test
    fun `a non-arm64 device is not eligible`() {
        val cap = DeviceGate.evaluate(supportsArm64 = false, isLowRamDevice = false, totalMemBytes = fourGb)
        assertFalse(cap.eligible)
    }

    @Test
    fun `the threshold is honoured exactly at the boundary`() {
        val exactly = DeviceGate.DEFAULT_MIN_TOTAL_MEM_BYTES
        assertTrue(
            DeviceGate.evaluate(supportsArm64 = true, isLowRamDevice = false, totalMemBytes = exactly).eligible,
        )
        assertFalse(
            DeviceGate.evaluate(supportsArm64 = true, isLowRamDevice = false, totalMemBytes = exactly - 1).eligible,
        )
    }
}
