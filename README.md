# PictoSpeak

**A free, open-source, 100% offline AAC communicator for Android.**

PictoSpeak is a pictogram-based communication app (AAC — Augmentative and Alternative
Communication) for non-verbal people: autism, cerebral palsy, aphasia, ALS. The user
composes a message by tapping pictograms;
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

## Quickstart — build & run

### What you need

- **[Android Studio](https://developer.android.com/studio)** (latest stable). It bundles a
  JDK 17 and the Android SDK — installing it is the whole toolchain. (CLI-only alternative:
  a JDK 17+ and the Android command-line SDK tools.)
- **A device to run on:** either an Android phone/tablet on **Android 10 or newer**
  (`minSdk 29`) with USB debugging enabled, **or** an emulator (create one below).

### Run it in Android Studio (easiest)

1. `git clone https://github.com/giuseppesorge/pictospeak.git` and **open the folder** in
   Android Studio. Let the Gradle sync finish (first sync downloads dependencies).
2. In **Build Variants** (bottom-left panel), select **`fossDebug`** for the `:app` module.
3. Pick a device in the toolbar dropdown — your connected phone, or **Device Manager →
   Create device** → any phone → a **system image with API 29+** (a ~2 GB profile mirrors the
   real target).
4. Press **Run ▶**. The app builds, installs, and launches.

### Run it from the command line

```bash
# macOS with Android Studio installed (points JAVA_HOME at its bundled JDK):
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# Opening the project in Android Studio once creates local.properties (sdk.dir=...).
# For a pure CLI setup, set it yourself: echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

./gradlew installFossDebug   # build + install the FOSS debug app on the connected device/emulator
adb shell am start -n io.github.giuseppesorge.pictospeak/.MainActivity   # launch it
```

Other useful commands:

```bash
./gradlew check                                   # ktlint + detekt + all JVM tests
./gradlew assembleFossDebug                       # just build the installable debug APK
./gradlew assembleFossRelease assemblePlayRelease # both flavors must always build
scripts/check-licenses.sh                         # license/attribution gate
```

**First launch:** the app opens a one-time speech-setup screen. Tap **Continue**; if the
device has no offline voice for the language, it offers to install the voice data. Then you
land on the board — tap pictograms, cycle the proposed sentence, and confirm to speak.

More detail (SDK setup, release signing, asset pipelines) is in
[`docs/handover.md`](docs/handover.md).

### Two build flavors

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
