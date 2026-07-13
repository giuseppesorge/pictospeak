# Performance budgets

Performance on cheap hardware is a **launch criterion**. Budgets are verified on physical
devices only — Macrobenchmark numbers from emulators are officially non-representative and
are rejected in review.

Device tiers:

- **Floor** (the launch criterion): ~2 GB-RAM tablet, Android 10/11, Cortex-A53/A55 class
  (e.g. Galaxy Tab A7 Lite / Lenovo Tab M8 class).
- **Mainstream**: ≥4 GB phone/tablet — also the only tier where the optional LLM may run.

| Metric | Budget (floor device) | Measured by |
|---|---|---|
| Cold start TTID / TTFD | ≤ 2.0 s / ≤ 3.5 s | `:benchmark` StartupBenchmark, physical device |
| Grid scroll jank | < 1% janky frames on a fast fling over 500 cells | `FrameTimingMetric` |
| Memory | steady-state PSS ≤ 250 MB; **zero LMK kills** in a 30-min soak | `scripts/perf/soak.sh` |
| Sentence proposal | ≤ 50 ms P95 on device | in-app trace |
| Confirm tap → audio start | ≤ 500 ms with a warm engine | `UtteranceProgressListener.onStart` timestamp |
| Base APK (foss) | ≤ 12 MB (target 10) incl. bundled pictograms | CI size gate |
| LLM (play, gated devices) | see `llm-experiment.md` go/no-go | `tools/llm-lab` protocol |

Mandatory build mitigations from release one: app-specific Baseline Profile +
R8 full mode (minify + shrink resources) — both are wired in `app/build.gradle.kts`
and must never be disabled "temporarily".

Memory tactics: only visible grid cells resident (single-density 256px WebP, hardware
bitmaps via Coil, off-Java-heap); originals never decoded; image cache bounded;
`onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` drops caches and releases the LLM engine;
LeakCanary in debug builds.

Results per milestone/device are recorded in `benchmarks.md`. If Compose misses the
startup/jank budget on the floor device after mitigations, the board screen is rebuilt in
Views behind the same ViewModel — the trigger and procedure live in ADR-0001.
