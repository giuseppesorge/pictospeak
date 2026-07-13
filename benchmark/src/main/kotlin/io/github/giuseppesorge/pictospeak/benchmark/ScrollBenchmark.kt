package io.github.giuseppesorge.pictospeak.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Grid scroll jank over the full bundled board — budget: <1% janky frames on a fast fling
 * on the physical floor device (docs/perf-budgets.md). The grid exposes testTag
 * "board-grid" as a resource id.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun gridFling() {
        benchmarkRule.measureRepeated(
            packageName = "io.github.giuseppesorge.pictospeak",
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            compilationMode = CompilationMode.DEFAULT,
        ) {
            pressHome()
            startActivityAndWait()
            val grid = device.wait(Until.findObject(By.res("board-grid")), FIND_TIMEOUT_MS)
            checkNotNull(grid) { "board-grid not found — is the catalog bundled?" }
            grid.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
            repeat(FLING_REPEATS) {
                grid.fling(Direction.DOWN)
                device.waitForIdle()
            }
            repeat(FLING_REPEATS) {
                grid.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val FIND_TIMEOUT_MS = 5_000L
        const val GESTURE_MARGIN_DIVISOR = 5
        const val FLING_REPEATS = 3
    }
}
