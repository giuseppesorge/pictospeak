# Benchmark results

Measured numbers per milestone and device. Budgets and procedures: `perf-budgets.md`.
No entry = not yet measured. Emulator numbers are never recorded here.

## Device matrix

| Device | RAM | Android | Tier | Notes |
|---|---|---|---|---|
| *(floor tablet — to be acquired)* | ~2–3 GB | 10/11 | floor | launch-criterion device |
| Redmi Note 15 (25098RA98G) | 8 GB | 16 (API 36) | mainstream | also LLM-experiment device; HyperOS quirks: shader-cache-drop broadcast blocked by process freezer (run with `androidx.benchmark.dropShaders.enable=false`), gestures need the "USB debugging (Security settings)" developer toggle |
| Samsung phone | ≥4 GB | current | mainstream | |

## Results

### M1 — feasibility gate (520-pictogram board)

How to run (on a PHYSICAL device with USB debugging):

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest   # StartupBenchmark + ScrollBenchmark
# Baseline profile: run BaselineProfileGenerator, commit output to app/src/main/baseline-prof.txt
# TTS probe: install foss debug, airplane mode, compose, Speak; then voice-data install flow
```

| Metric | Floor | Mainstream (Redmi, 2026-07-13) | Budget (floor) | Pass? |
|---|---|---|---|---|
| Cold TTID | — | **681 ms median** (629–738, 5 iter, no baseline profile, shader-drop disabled) | ≤ 2.0 s | mainstream: well under |
| Jank (fast fling) | — | *pending — HyperOS blocks pointer injection even with the security toggle (device reboot may be required); measure on the floor tablet* | < 1% | |

Baseline Profile: generated on the Redmi (2026-07-13, startup + first-frame journey;
scroll journey skipped — pointer injection blocked) and committed to
`app/src/main/baseline-prof.txt` (8,653 rules, verified packaged as
`assets/dexopt/baseline.prof` in the release APK). Regenerate on a device with working
gesture injection to add the scroll journey.

TTS probe findings (engine present? offline voice for the active language? install flow?
airplane-mode speak?): *to be recorded on physical devices.*

Functional smoke (2026-07-13, 2GB-RAM tablet AVD — functional only, never performance):
board renders the full 520-pictogram catalog with Fitzgerald coloring, selection strip +
proposal + Speak enablement work end-to-end, grid scrolls through the whole catalog. PASS.
