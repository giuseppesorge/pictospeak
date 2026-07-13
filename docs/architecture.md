# Architecture

Native Android, Kotlin, Jetpack Compose (Material 3), minSdk 29 (Android 10).
Hardware floor: a ~2 GB-RAM tablet with a Cortex-A53/A55-class CPU. Code is Apache-2.0;
asset licensing is strictly separated (see `THIRD_PARTY_NOTICES.md`).

## Hard decisions → architecture rules

| Decision | Consequence |
|---|---|
| 100% offline, no backend | **No network permission in any flavor.** No HTTP client anywhere. All content is committed assets produced by offline pipelines (`tools/`). |
| ARASAAC CC BY-NC-SA | License-fenced asset directories + attribution manifest; official attribution rendered from first run; `scripts/check-licenses.sh` fails CI if a compliance surface goes missing. |
| Hybrid sentence engine | The template engine (`:nlg`) is a synchronous pure function — the product. The LLM is an async `SentenceRefiner` in `:llm`, compiled only into the `play` flavor. Deleting `:llm` + one `RefinerFactory` compiles clean. |
| User confirms before speech | **INVARIANT-1**, triple-enforced (below). |
| Low-end hardware target | Numeric budgets in `perf-budgets.md`; single-density WebP assets; hardware bitmaps; `onTrimMemory`; runtime capability gate for the LLM; physical-device sign-off only. |
| Native Android TTS | `AndroidTtsGateway` with an explicit readiness state machine; offline-voice preference; guided voice-data install; eSpeak NG documented as last resort. |
| Boring + handover-grade | 4 production modules, manual constructor DI, no Hilt/Room/nav-library, versioned JSON files, pinned version catalog, ADRs. |

## Modules

```
:app        Compose UI, ViewModels, AppContainer (manual DI), repositories, device gate,
            model store, first-run setup. Single activity, sealed Screen + when.
:nlg        PURE Kotlin JVM. Domain types, SentenceEngine + SentenceRefiner interfaces,
            per-language template engines, morphology, lexicon. Zero Android imports.
:speech     ConfirmedUtterance, ConfirmationGate, TtsGateway, AndroidTtsGateway,
            TtsReadinessMachine (pure, JVM-tested).
:llm        LiteRtSentenceRefiner — the ONLY module importing LiteRT-LM. play flavor only.
:benchmark  Macrobenchmark + (M1+) Baseline Profile generation. Never shipped.
```

Dependency rules, enforced by the Gradle graph:
`:speech → :nlg` and `:llm → :nlg` only. **No edge from `:nlg` or `:llm` to `:speech`** —
generation code cannot reach audio by construction. `:app` sees everything; all wiring
lives in `AppContainer`.

## Flavors

| Flavor | Channel | `:llm` in binary | Network permission |
|---|---|---|---|
| `foss` (default) | F-Droid / GitHub APK | no — CI asserts no `liblitertlm_jni.so` | none |
| `play` | Google Play (AAB) | yes, inert until enabled; model imported via SAF | none |

The seam is one `RefinerFactory.kt` per flavor source set (foss returns `null`).

## INVARIANT-1: confirm-before-speak

A generated sentence is NEVER spoken automatically. Enforcement layers:

1. **Type system** — `TtsGateway.speak()` accepts only `ConfirmedUtterance`, whose
   constructor is internal to `:speech`. The sole mint is `ConfirmationGate.confirm()`,
   called from exactly one place: the user's confirmation tap handler. The type is not
   serializable and never persisted, so a confirmation cannot be replayed from disk.
2. **Module graph** — `:nlg` and `:llm` cannot even name `:speech` types.
3. **Tests** — ViewModel invariant tests with a recording fake `TtsGateway` assert zero
   `speak()` calls for any event sequence without an explicit confirm; no `speak(String)`
   overload may exist.
4. **Review rule** — every future speech feature (quick phrases, favorites, repeat-last)
   passes through `ConfirmationGate`. PRs adding another speech entry point are rejected.

Exception, by design: `speakWordPreview(token)` speaks the single label of a pictogram the
user just tapped (profile-gated, default off). That is user-initiated speech of exactly the
tapped word, not generated text.

## Core flow

tap pictogram → selection strip (max 8) → `SentenceEngine.propose()` (pure, ≤50ms,
`Dispatchers.Default`) publishes template candidates immediately → *optionally* (play
flavor ∧ device gate ∧ profile flag) the refiner runs with a 4s timeout and **appends**
one labeled extra candidate — never replaces, reorders, or auto-selects → the user cycles
alternatives (article/tense ambiguity is resolved by proposal, never silently) → the user
taps the speak button → `ConfirmationGate` → `TtsGateway.speak`.

Degradation ladder: LLM absent/gated/timed out → templates only (the normal case) ·
pattern unmatched → `FALLBACK_CONCAT` (label concatenation) · missing morphology →
citation form · no offline voice → readiness banner + guided install; composing still
works · Compose misses budget on hardware → the board screen is rebuilt in Views behind
the same ViewModel (ADR-0001).

## Data

Versioned JSON everywhere (`kotlinx.serialization`), atomic temp-file+rename writes,
`schemaVersion` on every root. Read-only content ships in `assets/` (pipeline-generated,
per-language); mutable state lives in `filesDir/` (`settings.json` = the `Profile`, imported
models). Import/export via the Storage Access Framework (zero permissions on API 29+). Board
data is deliberately isomorphic to Open Board Format so `.obf/.obz` interop is mechanical
later (ADR-0008). No Room (ADR-0002).

## Profile, settings and first-run

The `Profile` (language, speech rate/pitch, tap-to-hear, grid density, llmEnabled,
setupComplete) is a `MutableStateFlow` in `AppContainer`, persisted by `ProfileRepository`.
Every setting change saves immediately. The active LanguagePack, the TTS locale, and the
speech rate/pitch all follow the profile — switching the language in Settings rebuilds the
board (keyed by language) with the right engine, vocabulary, and voice; nothing else changes.
On first run (or when TTS is not ready) the app shows the **TTS setup wizard**, which surfaces
`TtsReadiness` and offers voice-data install and a voice test — a mute AAC device is shown,
never assumed. Caregiver Settings are reached from the About screen.

## Threading

Main = Compose + marshaled TTS callbacks only · `Dispatchers.Default` = sentence engine ·
`Dispatchers.IO` = JSON/asset IO · a dedicated single-thread dispatcher = LLM (serialized,
cancellable, timeout-bounded). No Services, no WorkManager, no locks — state lives in
`StateFlow`s under `viewModelScope`.
