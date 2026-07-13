# Positioning

What this project is, who it is for, and how it relates — honestly — to the other free and
commercial AAC (Augmentative and Alternative Communication) tools in the same space. This
document is descriptive, not promotional: every comparative claim below is meant to survive
a skeptical reader with the competing product open in another tab.

## What it is

A pictogram AAC communicator for people who cannot rely on speech (e.g. autism, cerebral
palsy, aphasia, ALS). The user taps a sequence of ARASAAC pictograms; a template-based
natural-language generator (NLG) turns that telegraphic sequence into a **grammatical
sentence**, and offers cycle-able alternatives so the final wording is always the user's
choice. Nothing is ever spoken until the user explicitly confirms it.

Concretely: `io volere mangiare pizza` → **`io voglio mangiare la pizza`**, with the
conjugated verb and the inserted article, plus alternatives to cycle through before speaking.

Defining properties:

- **100% offline, on-device.** No network permission in the FOSS flavor. No accounts, no
  analytics, no cloud. It works in airplane mode.
- **Native, low-end Android.** Kotlin / Jetpack Compose, minSdk 29 (Android 10), tuned for a
  2 GB-RAM tablet as the hardware floor.
- **Deterministic sentence NLG.** The template engine is the product. An optional on-device
  LLM (in the `play` flavor only) is **off by default** and, when on, only ever *appends one
  labeled extra suggestion* — it never replaces the deterministic output.
- **Multilingual by construction.** Ships two language packs; the first implemented language
  pack is Italian, the second is English. The architecture is language-pluggable
  (`LanguagePack`) and RTL-ready.
- **Open source, license-clean.** Apache-2.0 code, ARASAAC pictograms (CC BY-NC-SA) with the
  official attribution, asset and code licenses kept strictly separate (F-Droid-friendly).

## Who it serves

- **Symbol-based communicators** who benefit from full spoken sentences rather than
  word-by-word telegraphic output, and who need the tool to work anywhere, without a network.
- **Families and therapists** who want a free, private, low-cost tool that runs on inexpensive
  Android hardware they may already own, with a small (~400-word) therapist-reviewable core
  vocabulary rather than an overwhelming symbol library.
- **Deployments where connectivity, cost, or data privacy rule out cloud features** — the
  sentence engine never leaves the device.

It is *not* aimed at replacing eye-gaze text keyboards for late-stage motor impairment, nor at
users who are fluent literate typists; those needs are already served well by other tools.

## How it compares

The table summarizes the axes that actually differ. Read the notes below it — each competitor
is genuinely good at something, and the comparison is only meaningful once you separate the
individual axes (offline / automatic / multilingual / sentence-level) from the *combination*.

| | This project | AsTeRICS Grid | Cboard | Proloquo2Go | Grid 3 |
|---|---|---|---|---|---|
| **Price** | Free, open-source (Apache-2.0) | Free, open-source | Free, open-source (GPLv3) | ~USD 300 (iOS) | ~GBP 550 (Windows) |
| **Platform** | Native Android 10+, 2 GB floor | Browser PWA (cross-platform) | Browser PWA + wrapped app builds | Apple only (iOS/macOS) | Windows-centric desktop |
| **Core comms offline** | Yes, no network permission | Yes (cached PWA, local data) | Yes (cached PWA, browser TTS) | Yes (on-device; net for voice downloads) | Yes (installed desktop app) |
| **Grammar level** | Sentence-level reconstruction of the whole sequence | Automatic sentence correction **or** manual word-forms | Concatenation of tile labels | Word-level inflection (manual pick) | Word/cell-level inflection (automatic) |
| **Automatic sentence NLG** | Yes, on-device | Yes — but **online + Spanish-only** | Only via **cloud LLM** (OpenAI) | No (word-level only) | No (per-word, context-driven) |
| **NLG works offline** | Yes | Automatic path: **no** (Word forms is offline but manual) | Sentence AI: **no** | n/a (no sentence NLG) | n/a (no sentence NLG) |
| **Multilingual NLG** | Italian + English, pluggable | Automatic path: Spanish only | n/a | 4 languages (word-level) | Many locales (word-level) |
| **Symbol set** | ARASAAC | ARASAAC | Mulberry (default) + ARASAAC + others | Proprietary + SymbolStix | Proprietary / Widgit etc. |

Notes, competitor by competitor:

- **AsTeRICS Grid** — the closest free competitor, and a strong one: free, open-source,
  actively maintained in 2026, ARASAAC-based, offline-capable PWA, multilingual, with rich
  input support (touch, switch scanning, eye gaze) and prediction. It *does* offer automatic
  sentence-level grammatical correction — but that path is a **remote call to ARASAAC's
  Natural Language API** (it requires an internet connection) and is currently **Spanish
  only**. Its offline grammar path, "Word forms," is not automatic sentence synthesis: a
  configurer must **manually author each inflected form** per element, and at runtime the app
  swaps in the best-matching pre-authored label. So *offline + automatic + full-sentence +
  multilingual, all at once* is the gap — not grammar in general, which it clearly has. A PWA
  reaches more device types than a native app; a native app buys tighter offline/TTS/perf
  integration on the low-end Android floor. Both are legitimate.
- **Cboard** — free, open-source (GPLv3), UNICEF-supported, ~40 languages, uses ARASAAC among
  its symbol sets. Offline, it **concatenates the labels** of tapped tiles and speaks them via
  the browser's speech synthesis (telegraphic output, no inflection or inserted function
  words). It reaches full sentences only through an **optional cloud LLM feature** (OpenAI),
  which needs connectivity and sends the utterance off-device. So it has no offline, on-device,
  deterministic sentence engine — but it is neither "unable to make sentences" nor "not
  offline," and both would be false to claim.
- **Proloquo2Go** (commercial, ~USD 300, Apple-only) and **Grid 3** (commercial, ~GBP 550,
  Windows-centric) both do capable **word-level** grammar. Proloquo2Go lets the user pick
  inflected forms from a popup (Ultralingua engine); Grid 3 automatically conjugates
  individual verbs from the preceding words and does suffix "smart grammar." Neither ingests a
  whole telegraphic symbol sequence and reconstructs a complete sentence — inserting missing
  articles/prepositions, resolving clause-wide agreement, and offering cycle-able
  *whole-sentence* alternatives. Note the nuance: Grid 3 genuinely does automatic conjugation,
  so the honest line is **word/cell-level inflection vs. sentence-level reconstruction**, not
  "they have no grammar." Offline is *not* the axis that separates us from these two; cost,
  platform (low-end Android), and the sentence-level pipeline are.

### The one differentiating claim, stated carefully

To our knowledge, and among the free tools we reviewed, no product offers **fully offline,
on-device, automatic pictogram-sequence-to-full-sentence generation on low-end Android across
multiple languages**. Each individual axis is matched by someone: AsTeRICS Grid has automatic
sentence correction (online, Spanish only) and an offline manual word-forms path; Cboard has
cloud sentence AI; the commercial tools have strong word-level inflection. The defensible
differentiator is the **combination** — offline **and** automatic **and** multilingual **and**
sentence-level — not any single axis, and it is stated with hedging on purpose. We do not
claim to be "the first" or "the only" anything.

## What we deliberately do NOT do

Scope discipline is a feature. We intentionally leave these out:

- **No cloud, no network, no accounts.** No online grammar API, no cloud LLM, no sync, no
  telemetry. The FOSS flavor has no network permission at all. This rules out the online-only
  capabilities some competitors offer — that is a deliberate trade for privacy and true
  offline use.
- **No LLM as the primary engine.** The deterministic template NLG is the product. The
  optional on-device LLM is off by default and can only append one clearly labeled extra
  suggestion; the app is fully functional (and demoed) with it removed entirely.
- **No speaking without explicit confirmation.** Generated text is never spoken until the user
  taps to confirm. The last word is always the user's — by construction, not by policy.
- **No unbounded grammar.** The grammar scope is frozen per language; we cover a real,
  documented set of phenomena rather than chasing full linguistic coverage. Unmatched input
  degrades gracefully to a concatenated fallback, never a silent guess.
- **No giant symbol library.** A curated, therapist-reviewable core vocabulary (~400 words)
  instead of thousands of tiles, to keep the tool learnable and the boards navigable.
- **No premium-hardware assumption.** We do not target high-end tablets or dedicated AAC
  devices; the 2 GB Android 10 floor is a launch criterion, not an afterthought.
- **No competing on eye-gaze text entry.** Alternative-access text keyboards for late-stage
  motor impairment are a different modality served well elsewhere; our modality is symbols.

## Summary

The honest position: a free, open-source, fully-offline native Android communicator whose
distinguishing capability is turning a telegraphic pictogram sequence into a grammatical,
user-confirmed spoken sentence on-device, in more than one language, on cheap hardware — a
combination that, to our knowledge, the other free tools do not currently provide, even though
each of them is stronger than us on at least one individual axis.
