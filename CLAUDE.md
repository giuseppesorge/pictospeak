# PictoSpeak — AAC communicator, Kotlin/Android

Open-source pictogram AAC (Augmentative and Alternative Communication) app for non-verbal
users. Offline, multilingual, low-end hardware. Built to outlive any single maintainer:
boring architecture, handover-grade docs.

## Hard rules — never violate, never "temporarily" bypass
1. 100% offline. No network code anywhere. The foss flavor has NO network permission —
   keep it that way. No analytics, no telemetry, no cloud SDKs, ever.
2. INVARIANT-1: generated text is NEVER spoken without explicit user confirmation.
   TtsGateway.speak() takes only ConfirmedUtterance (minted solely by ConfirmationGate in
   :speech from the user's tap handler). Never add a speak(String) overload. Never add a
   dependency from :nlg or :llm to :speech. Every new speech feature goes through
   ConfirmationGate.
3. Templates are the product; the LLM is an optional, appended, labeled extra candidate.
   The app must be fully functional (and demoed) with the LLM off. :llm compiles only into
   the play flavor; deleting :llm + one RefinerFactory must leave a clean build.
4. ARASAAC compliance is part of the definition of done: the official attribution string
   must render in About + first-run; pictogram-derived files stay inside license-fenced
   asset directories (CC BY-NC-SA), never blended into branding. CI license-check must pass.
5. Performance on the 2GB floor device is a launch criterion. Respect docs/perf-budgets.md.
   Never sign off performance on an emulator.
6. minSdk 29. Test on Android 10.

## Architecture (see docs/architecture.md)
- Modules: :app (UI/DI/data), :nlg (pure JVM — keep it free of Android imports),
  :speech (TTS + confirmation), :llm (only LiteRT-LM importer), :benchmark.
- Manual DI in AppContainer. No Hilt, no Room, no navigation library, no Retrofit —
  these are ADR'd decisions (docs/adr/); don't reintroduce them casually.
- State: ViewModel + StateFlow; Compose UI is a thin shell (a Views fallback of the board
  screen must remain possible).
- Persistence: versioned JSON via kotlinx.serialization; atomic temp-file+rename writes.
- Language support is a LanguagePack: catalog_{lang}.json + lexicon_{lang}.json +
  Realizer in :nlg lang/{lang}/ + boards/default_{lang}/ + TTS locale chain
  (docs/language-packs.md). Keep every layer language-pluggable; no language may leak
  into the engine core. RTL-ready always: start/end only, never left/right; pictograms
  are never mirrored.

## Grammar engine
- Scope is FROZEN per language in docs/grammar-v1.md. Adding any grammatical phenomenon
  requires a failing real-user scenario + a new ADR. Keep :nlg under ~2k LOC per language.
- Morphology facts (verb auxiliary, participles, irregulars) are LEXICON DATA, not rules.
- Unmatched input → FALLBACK_CONCAT candidate. Missing data → citation form. Never throw,
  never guess silently. Ambiguity → alternatives the user cycles, not silent choices.
- Every field bug becomes a golden test case in nlg/src/test/resources/golden/.

## Workflow
- JAVA_HOME must point at a JDK 17+ (this machine: Android Studio JBR —
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`).
- ./gradlew check           # ktlint, detekt, all JVM tests
- ./gradlew assembleFossRelease assemblePlayRelease   # both must always build
- scripts/check-licenses.sh # license/attribution gate, also run in CI
- Golden NLG tests are the spec: change goldens only with linguistic justification in the PR.
- Decisions of consequence get an ADR in docs/adr/. Update docs/handover.md when build,
  release, or pipeline steps change.
- Asset pipelines (tools/arasaac-fetch, tools/lexicon-build) run manually per release —
  the app never touches the network. Snapshot outputs are committed and versioned.
- Conventional commits; PRs must keep CI green incl. the foss-no-liblitertlm assertion.
- Git authorship: commits are authored by the repository owner only — do NOT add
  Co-Authored-By trailers or any tool attribution to commit messages.

## Licensing map (never mix)
Code: Apache-2.0 · ARASAAC assets: CC BY-NC-SA (attribution mandatory) · Lexicon:
Morph-it!-derived, LGPL option · Model weights: never in repo; Gemma ToU flow-down or
Qwen Apache-2.0. When adding any dependency or asset, update THIRD_PARTY_NOTICES.md.
