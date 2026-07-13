# Handover runbook

Everything needed to build, test, and release PictoSpeak without asking anyone.
If a step here goes stale, fixing it is part of the change that broke it.

## Build

Prerequisites: JDK 17+ and the Android SDK. `local.properties` needs `sdk.dir=<path>` (not
committed). On macOS with Android Studio installed:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew check                                   # ktlint + detekt + all JVM tests
./gradlew assembleFossDebug                       # installable debug APK (foss flavor)
./gradlew assembleFossRelease assemblePlayRelease # both flavors must always build
scripts/check-licenses.sh                         # license/attribution gate
```

Module map and dependency rules: `architecture.md`. Version pins: `gradle/libs.versions.toml`
(single source of truth; renovate by hand, deliberately).

## Test

- JVM tests run in `check` and CI on every push. NLG golden corpora under
  `nlg/src/test/resources/golden/` are the executable grammar spec.
- Performance: physical devices only. `./gradlew :benchmark:connectedBenchmarkAndroidTest`
  on the floor device; 30-min soak via `scripts/perf/soak.sh`. Record numbers in
  `benchmarks.md`.

## Asset pipelines (run manually, outputs committed)

- `tools/arasaac-fetch` — snapshots the ARASAAC catalog, downloads the curated pictogram
  subset (rate-limited), converts to 256px WebP, emits catalog + attribution manifest +
  LICENSE. Re-run deliberately per release; snapshots are versioned by date+count under
  `assets-src/`.
- `tools/lexicon-build` — builds `lexicon_{lang}.json` from the morphological source
  (Italian: Morph-it!), plus the reference inflection table used by property tests, plus
  PROVENANCE and LICENSE files.
- The app itself never touches the network — pipelines are the only place downloads happen.

## LLM runtime swap procedure (churn insurance, ADR-0004)

All LiteRT-LM symbols live in one file: `llm/src/main/kotlin/.../LiteRtSentenceRefiner.kt`.
To swap runtimes: replace that file's internals, keep `SentenceRefiner` untouched, update
the pinned artifact in the version catalog and `THIRD_PARTY_NOTICES.md`. To remove the LLM
entirely: delete `:llm`, the play `RefinerFactory`, and the settings include — the foss
flavor never knew it existed.

## Release

1. `./gradlew check assembleFossRelease bundlePlayRelease` must be green, plus
   `scripts/check-licenses.sh` and the CI no-`liblitertlm`-in-foss assertion.
2. Physical-device checklist (budgets in `perf-budgets.md`; record in `benchmarks.md`):
   run `scripts/perf/release-checklist.sh` and the airplane-mode `scripts/demo-dryrun.sh`.
3. Tag `vX.Y.Z` → `release.yml` builds the artifacts.
4. Distribution: GitHub Releases APK (foss). F-Droid and Play submission are post-POC
   steps; the foss flavor is already built for F-Droid's constraints (FOSS-only deps, no
   network, committed assets, reproducible-friendly).

### Signing

Release signing reads `keystore.properties` at the repo root — **gitignored, never
committed**. To sign locally:

```bash
keytool -genkeypair -v -keystore pictospeak-release.jks -alias pictospeak \
  -keyalg RSA -keysize 2048 -validity 10000
cat > keystore.properties <<EOF
storeFile=pictospeak-release.jks
storePassword=…
keyAlias=pictospeak
keyPassword=…
EOF
./gradlew assembleFossRelease   # now signed
```

Without `keystore.properties` (CI, fresh clones) the release APK is left unsigned — sign it
out of band, or add the file. Keep the `.jks` and the properties out of version control
(both are already in `.gitignore`).

## Recurring obligations

- ARASAAC attribution surfaces (About, first-run, README, store listing) are checked by
  `scripts/check-licenses.sh` — keep it passing.
- Any new dependency or asset: entry in `THIRD_PARTY_NOTICES.md`, justification in the PR.
- Decisions of consequence: ADR in `docs/adr/`.
