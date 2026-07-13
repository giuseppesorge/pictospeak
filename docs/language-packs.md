# Language packs

Every layer of the app is language-pluggable. A **LanguagePack** is the complete contract
for one language; nothing about any specific language may leak into engine or UI core code.

## The contract

| Element | Where | Notes |
|---|---|---|
| ISO 639-1 code | pack id | e.g. `it`, `en` |
| RTL flag | pack metadata | drives layout direction; see `ui-conventions.md` |
| `catalog_{lang}.json` | `app/src/main/assets/arasaac/` | pictogram id → label, lemma ref, POS, color slot, tags; produced by `tools/arasaac-fetch` from a versioned snapshot |
| `lexicon_{lang}.json` | `app/src/main/assets/lexicon/` | morphology data per lemma; produced by `tools/lexicon-build`; carries its own LICENSE + PROVENANCE |
| Realizer | `nlg/src/main/kotlin/.../lang/{lang}/` | implements the frozen per-language grammar scope (`grammar-v1.md`) |
| Golden corpus | `nlg/src/test/resources/golden/{lang}/` | the language's executable spec |
| `boards/default_{lang}/` | `app/src/main/assets/boards/` | per-language board packs (one locale per board — Open Board Format convention) |
| TTS locale + fallback chain | pack metadata | preferred voice locale, offline-voice requirement, guided install text |

The first two implemented packs are `it` and `en`; the second exists precisely to prove
the contract (an arch test asserts no language-specific code in the engine core).

## Symbol-set coverage constraints (measured on the full ARASAAC catalog, 2026-07)

ARASAAC keyword coverage is near-total for it/es/en/pt/de (~100%), high for fr (99%),
he (98%), ru/fa (95%), partial for zh (82%), ar (62%), tr (19%), and **absent for
hi/ja/ur** (not in the API's language list). Languages beyond ARASAAC's coverage need a
different symbol source (e.g. the Global Symbols aggregator: Mulberry, Tawasol, Jellow)
and are out of scope until the symbol layer supports multiple sets.

## Rollout tiers

- **Now**: `it` (reference implementation), `en` (contract proof).
- **Next**: `es` (ARASAAC's native language), `fr`.
- **Then**: `de`, `pt-BR`, optionally `nl`.
- **Later**: `he` (first RTL pack — cheapest RTL pilot: 98% symbol coverage + common
  offline TTS voice), `ar` (needs symbol supplementation + MSA-vs-dialect decision),
  `zh` (isolating grammar = cheap realizer; offline TTS availability varies by device),
  `ru` (largest morphology effort).
- **Blocked on symbol strategy**: `hi`, `ja`, `ur`.

Per-language TTS caveats: Google's offline-capable TTS covers the tiers above except
Persian; devices without Google services need a documented fallback engine (e.g.
eSpeak NG, or neural options like sherpa-onnx/Piper) — see `architecture.md` TTS section.

## Rules for new packs

1. Never extend the engine core for one language — extend the pack.
2. A pack ships only when its golden corpus passes and a native speaker has reviewed both
   the corpus and the bundled keyword labels for the core vocabulary.
3. Per-verb/per-word irregularities go in the lexicon, not in code.
4. RTL readiness is not per-pack work — the UI is RTL-correct by construction
   (`ui-conventions.md`); a new RTL pack only sets its flag.
