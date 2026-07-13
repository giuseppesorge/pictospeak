# Contributing to PictoSpeak

Thank you! This project is built to be maintainable by people who are not its original
author. That goal shapes every rule below.

## Ground rules (non-negotiable)

1. **100% offline.** No network code, no telemetry, no cloud SDKs. The `foss` flavor
   declares no network permission — any PR that adds one will be rejected.
2. **INVARIANT-1.** Generated text is never spoken without explicit user confirmation.
   `TtsGateway.speak()` accepts only a `ConfirmedUtterance`. Do not add `speak(String)`.
   Do not add a dependency from `:nlg` or `:llm` to `:speech`.
3. **Templates are the product.** The optional LLM only appends one labeled candidate.
   Everything must work with the LLM absent.
4. **Non-commercial assets.** The ARASAAC pictograms are CC BY-NC-SA. Forks must not add
   monetization. Keep pictogram-derived files inside their license-fenced asset
   directories and keep attribution surfaces intact (`scripts/check-licenses.sh`).
5. **Frozen grammar scope.** `docs/grammar-v1.md` lists what the sentence engine does and
   deliberately does not do. Extending it requires a failing real-user scenario and a new
   ADR — not just a good idea.
6. **Low-end hardware is the target.** Budgets in `docs/perf-budgets.md`. Performance
   claims need numbers from a physical device, never an emulator.

## Building

```bash
export JAVA_HOME=<path to a JDK 17+>
./gradlew check                                   # ktlint + detekt + all JVM tests
./gradlew assembleFossDebug                       # installable APK
./gradlew assembleFossRelease assemblePlayRelease # both flavors must build
scripts/check-licenses.sh                         # license/attribution gate
```

## Making changes

- **Style:** ktlint + detekt run in CI; `./gradlew ktlintFormat` fixes most issues.
- **Tests:** every behavior change needs a JVM test. NLG changes need golden-file cases in
  `nlg/src/test/resources/golden/` — goldens change only with linguistic justification in
  the PR description.
- **Decisions:** anything of architectural consequence gets an ADR in `docs/adr/`.
- **Docs:** if you change build, release, or pipeline steps, update `docs/handover.md`.
- **Commits:** conventional commits (`feat:`, `fix:`, `docs:`, `test:`, `build:`, `chore:`).
- **Dependencies:** additions need a justification in the PR and an entry in
  `THIRD_PARTY_NOTICES.md`. The default answer to new dependencies is no — see
  `docs/adr/` for what is deliberately absent (Hilt, Room, navigation-compose, …).

## Definition of done (checklist for any PR)

- [ ] CI green (`check`, both release flavors, foss-APK-contains-no-`liblitertlm` assertion)
- [ ] No new network surface; no INVARIANT-1 violation
- [ ] Tests added/updated; goldens justified if changed
- [ ] `THIRD_PARTY_NOTICES.md` updated if dependencies/assets changed
- [ ] Docs/ADRs updated if behavior or decisions changed
