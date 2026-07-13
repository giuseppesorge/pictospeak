package io.github.giuseppesorge.pictospeak.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start TTID/TTFD measurement — budget: TTID ≤ 2.0s, TTFD ≤ 3.5s on the physical
 * floor device (docs/perf-budgets.md). Run:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 * on a real device; results go to docs/benchmarks.md.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() {
        benchmarkRule.measureRepeated(
            packageName = "io.github.giuseppesorge.pictospeak",
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            compilationMode = CompilationMode.DEFAULT,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
