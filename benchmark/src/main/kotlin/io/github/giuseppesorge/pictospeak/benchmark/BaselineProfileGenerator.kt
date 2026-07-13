package io.github.giuseppesorge.pictospeak.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile (mandatory perf mitigation from release one,
 * docs/perf-budgets.md): cold startup + first grid scroll journey.
 *
 * Run on a rooted device/emulator or a userdebug build:
 *   ./gradlew :app:generateBaselineProfile
 * The generated profile ships automatically via the androidx.baselineprofile plugin.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect("io.github.giuseppesorge.pictospeak") {
            pressHome()
            startActivityAndWait()
            val grid = device.wait(Until.findObject(By.res("board-grid")), FIND_TIMEOUT_MS)
            if (grid != null) {
                grid.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
                grid.fling(Direction.DOWN)
                device.waitForIdle()
                grid.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val FIND_TIMEOUT_MS = 5_000L
        const val GESTURE_MARGIN_DIVISOR = 5
    }
}
