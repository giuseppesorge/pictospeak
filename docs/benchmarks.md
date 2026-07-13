# Benchmark results

Measured numbers per milestone and device. Budgets and procedures: `perf-budgets.md`.
No entry = not yet measured. Emulator numbers are never recorded here.

## Device matrix

| Device | RAM | Android | Tier | Notes |
|---|---|---|---|---|
| *(floor tablet — to be acquired)* | ~2–3 GB | 10/11 | floor | launch-criterion device |
| Redmi Note 15 | ≥4 GB | current | mainstream | also LLM-experiment device |
| Samsung phone | ≥4 GB | current | mainstream | |

## Results

### M1 — feasibility gate (520-pictogram board)

How to run (on a PHYSICAL device with USB debugging):

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest   # StartupBenchmark + ScrollBenchmark
# Baseline profile: run BaselineProfileGenerator, commit output to app/src/main/baseline-prof.txt
# TTS probe: install foss debug, airplane mode, compose, Speak; then voice-data install flow
```

| Metric | Floor | Mainstream | Budget | Pass? |
|---|---|---|---|---|
| Cold TTID | — | — | ≤ 2.0 s | |
| Jank (fast fling) | — | — | < 1% | |

TTS probe findings (engine present? offline voice for the active language? install flow?
airplane-mode speak?): *to be recorded on physical devices.*

Functional smoke (2026-07-13, 2GB-RAM tablet AVD — functional only, never performance):
board renders the full 520-pictogram catalog with Fitzgerald coloring, selection strip +
proposal + Speak enablement work end-to-end, grid scrolls through the whole catalog. PASS.
