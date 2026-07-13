# PictoSpeak

**A free, open-source, 100% offline AAC communicator for Android.**

PictoSpeak is a pictogram-based communication app (AAC — Augmentative and Alternative
Communication / CAA — Comunicazione Aumentativa e Alternativa) for non-verbal people:
autism, cerebral palsy, aphasia, ALS. The user composes a message by tapping pictograms;
PictoSpeak proposes a grammatically correct sentence; **the user confirms it**; the device
speaks it with the native Android text-to-speech voice.

## Why another AAC app

- **Grammar, not word salad.** PictoSpeak turns a telegraphic pictogram sequence
  ("I want eat pizza") into a grammatical sentence ("I want to eat pizza") with a
  rule-based engine that runs entirely on the device — in every supported language.
  No surveyed app does this offline on Android.
- **100% offline, forever free.** No backend, no account, no analytics, no ads, no
  in-app purchases, zero recurring costs. Privacy by design: nothing ever leaves the device.
- **Made for cheap hardware.** The performance target is a ~2 GB-RAM Android 10 tablet —
  the device class families and schools actually have. Performance on low-end hardware is
  a launch criterion, not an optimization.
- **The user always has the last word.** A generated sentence is *never* spoken
  automatically. The person composes, sees the proposal, and confirms. The device says what
  the person chose to say — this is a hard, architecturally enforced rule (INVARIANT-1).
- **Multilingual by design.** Every layer (vocabulary, grammar engine, voices, boards)
  is pluggable per language, with RTL support built in from day one.
- **Built to be handed over.** Boring architecture, exhaustive docs, ADRs for every
  consequential decision. The project must survive without its original author.

## Status

Early development (POC phase). See `docs/` for architecture, performance budgets, and the
frozen grammar scope. Not yet ready for end users.

## Building

```bash
# Requires JDK 17+ and the Android SDK (see docs/handover.md for details)
./gradlew check                                   # lint + all JVM tests
./gradlew assembleFossDebug                       # installable debug build (FOSS flavor)
./gradlew assembleFossRelease assemblePlayRelease # both flavors must always build
```

Two build flavors:

| Flavor | Purpose | Network permission | On-device LLM module |
|---|---|---|---|
| `foss` | F-Droid / GitHub releases | **none** | absent from binary |
| `play` | Google Play | Play delivery only | included, off by default, device-gated |

The optional on-device LLM only ever *proposes an extra labeled sentence candidate*; the
rule-based engine is the product, and the app is fully functional with the LLM absent.

## Legal & licenses

This repository deliberately separates code from third-party content:

- **Code** — [Apache License 2.0](LICENSE).
- **Pictograms** — the pictographic symbols used are the property of the Government of
  Aragon and have been created by Sergio Palao for [ARASAAC](https://arasaac.org), which
  distributes them under a Creative Commons BY-NC-SA license. They are **not** covered by
  the code license. *Italiano:* I simboli pittografici utilizzati sono di proprietà del
  Governo di Aragona e sono stati creati da Sergio Palao per ARASAAC (https://arasaac.org),
  che li distribuisce sotto Licenza Creative Commons BY-NC-SA.
- **Italian morphological lexicon** — derived from
  [Morph-it!](https://docs.sslmit.unibo.it/doku.php?id=resources:morph-it) (Baroni &
  Zanchetta), used under its LGPL option.
- **Model weights** — never distributed in this repository. See `llm/NOTICE-models.md`.

Full matrix in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

**Note for forks:** the ARASAAC pictograms are licensed for *non-commercial* use only.
Any fork that adds monetization (ads, paid features) violates the pictogram license.
PictoSpeak itself is free forever.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Start with the docs in `docs/` — especially
`architecture.md` (the INVARIANT-1 rule) and `grammar-v1.md` (the frozen grammar scope).
