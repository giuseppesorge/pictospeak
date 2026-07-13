# Demo script — the offline path (airplane-mode screen recording)

**Goal.** A single 2–3 minute screen recording that proves the whole offline pipeline end
to end, on a real device in airplane mode: first-run speech readiness → compose pictograms →
template turns the telegraphic sequence into a grammatical sentence → cycle an alternative →
confirm-to-speak → and it all runs with the optional AI assistant switched **off**.

**Deliverable.** One MP4 (portrait or landscape, matching the tablet), ~2:55, burned-in
captions, optional voiceover. No cuts that hide state changes: the airplane-mode icon stays
visible in the status bar the entire time.

**Non-negotiable honesty rules for this recording**

- Nothing is spoken until the user taps **Speak** (INVARIANT-1). Never fake the audio.
- Airplane mode is ON and visible before the first tap and never toggled off.
- No "first/only/best" claims. Just show the path working. Let the offline speech be the proof.

---

## Record this build / device

| What | Setting | Why |
|---|---|---|
| Flavor | **play** release, AI assistant **OFF** | So the AI-off toggle is visible on screen (the foss flavor has no AI section at all). |
| Device | 2 GB-RAM Android 10 tablet (the floor device) | Perf is a launch criterion; never demo on an emulator. |
| Connectivity | **Airplane mode ON** (Wi-Fi + mobile data off) | The single strongest proof the speech is on-device. |
| Language pack | The first implemented language pack (used for all on-screen examples below) | Cleanest inflection contrast; UI chrome captions are in English. |
| Voice data | Installed **once** beforehand, then airplane mode | Voice install is the only step that ever needs a network; it is done before recording. |
| Screen recorder | On-device recorder or `adb shell screenrecord` | Capture the real UI and the real audio path. |

> Footnote to add as a caption at least once: the **foss** release flavor ships with **no
> network permission at all** and does not contain the AI module — this recording uses the
> play flavor only so the AI-off switch is visible.

---

## The 20-second elevator opening (Beat 0 · 0:00–0:20)

A cold open that states the promise and immediately shows the offline condition, so a viewer
who watches only 20 seconds still gets the whole idea.

- **Screen:** App icon → home board fades in. Status bar pulled down for ~2s to show the
  **airplane-mode icon**, then released.
- **On-screen caption:** "A pictogram AAC communicator. Tap symbols → it speaks a full,
  grammatical sentence. 100% offline."
- **Voiceover (optional):** "This is an offline pictogram communicator. You tap symbols, it
  builds a grammatical sentence, and — with the device in airplane mode — it speaks. The user
  always has the last word. Here it is, start to finish."
- **Money frame:** side-by-side lower-third preview — telegraphic strip
  `io · want · eat · pizza` above, and the proposal `io voglio mangiare la pizza` below.

---

## Storyboard — the offline path

Each beat lists Screen / Action / Caption / Voiceover / Duration. Voiceover **goes silent**
during any beat where the device speaks, so the recording captures the real TTS.

### Beat 1 — First-run speech readiness (0:20–0:42, ~22s)

- **Screen:** Speech setup screen ("Speech setup"). Status line reads: *"An offline voice is
  ready. The device can speak without internet."*
- **Action:** Tap **Test voice**. The device speaks a short test phrase — **audible, in
  airplane mode.** Then tap **Continue**.
- **Caption:** "First run checks the voice before you rely on it. Offline voice ready — tested
  in airplane mode."
- **Voiceover:** "On first run it shows the speech state instead of assuming it — a mute AAC
  device is the worst kind of failure. The voice reports ready and offline…" *(go silent for
  the Test-voice audio)* "…and that is the device talking, with no connection."
- **Note:** If the floor device reports a missing voice, that install is done before recording;
  the recording begins from the *ready* state.

### Beat 2 — Compose pictograms (0:42–1:08, ~26s)

- **Screen:** Home board. Placeholder in the message window: "Tap pictograms to compose a
  sentence." Fitzgerald colors visible (people yellow, nouns orange, verbs green, descriptors
  blue, social pink).
- **Action:** Tap four cells in reading order, slowly, letting each land in the top selection
  strip:
  1. **io** (pronoun, white/misc)
  2. **volere** / *want* (verb, green)
  3. **mangiare** / *eat* (verb, green)
  4. **pizza** (noun, orange)
- **Caption:** "Tap in reading order. Selected symbols collect in the strip — telegraphic, like
  T9 before it guesses the word."
- **Voiceover:** "You tap the symbols in order. They collect at the top as a telegraphic
  sequence — subject, verb, verb, object. No grammar yet. That is the raw input."
- **Money frame:** the top strip now reads `io · volere · mangiare · pizza` (citation forms,
  no articles, no conjugation).

### Beat 3 — The template builds a grammatical sentence (1:08–1:30, ~22s)

- **Screen:** The proposal bar under the strip updates the instant the fourth symbol lands.
- **Action:** No tap needed — just hold on the frame so the contrast is unmistakable, then
  point (cursor/highlight) from the strip to the proposal.
- **Caption:** "The template turns it into a sentence — conjugates the verb, inserts the
  article: `io voglio mangiare la pizza`."
- **Voiceover:** "And there it is. The template engine conjugates *volere* to *voglio*, keeps
  the second verb as an infinitive, and inserts the article *la* — a full grammatical sentence
  from four symbols. This is deterministic: same symbols, same sentence, every time. On-device.
  No network, no model required."
- **Money frame:** strip `io · volere · mangiare · pizza` (top) vs proposal
  `io voglio mangiare la pizza` (below), both on screen.

### Beat 4 — Cycle an alternative (1:30–1:54, ~24s)

- **Screen:** Proposal bar (tap-to-cycle).
- **Action:** Tap the proposal bar once → it cycles to the indefinite-article alternative
  `io voglio mangiare una pizza`. Pause. Tap again to return to the definite
  `io voglio mangiare la pizza`.
- **Caption:** "Not sure it guessed right? Tap to cycle alternatives — *la pizza* / *una
  pizza*. The choice is the user's."
- **Voiceover:** "The engine picks a sensible default — the definite article — but where the
  input is genuinely ambiguous it offers alternatives you cycle through: *la pizza*, *una
  pizza*. It never decides silently for you. The user lands on the one they meant."
- **Note:** Cycling back to the default before speaking demonstrates that the user, not the
  engine, has the last word.

### Beat 5 — Confirm-to-speak, in airplane mode (1:54–2:16, ~22s)

- **Screen:** Proposal bar showing `io voglio mangiare la pizza`; **Speak** control visible;
  airplane-mode icon still in the status bar.
- **Action:** Tap **Speak**. The device speaks the full sentence aloud. Hold the frame through
  the whole utterance.
- **Caption:** "Nothing is spoken until you tap Speak. Then — offline — the device says it:
  *io voglio mangiare la pizza*."
- **Voiceover:** "Here is the rule the whole app is built around: the proposal is never spoken
  on its own. Only your tap on Speak turns it into speech." *(go silent for the full spoken
  sentence)* — let the TTS play uninterrupted.
- **Money frame:** the airplane icon and the Speak tap in the same frame, then the audio.

### Beat 6 — It works with the AI assistant OFF (2:16–2:42, ~26s)

- **Screen:** Open caregiver settings (press-and-hold the settings affordance) → Settings →
  "AI assistant (experimental)". Toggle is **Off**.
- **Action:** Show the toggle in the **Off** state. Return to the board. Re-tap the four
  symbols (or reuse the state) and show the proposal bar has **exactly one** template candidate
  — no appended, labeled AI line.
- **Caption:** "Everything so far ran with the optional AI **off**. Templates are the product;
  AI is an extra you can add, never a dependency."
- **Voiceover:** "And to be clear — everything you just saw ran with the AI assistant switched
  off. The templates are the product. The optional on-device model, when enabled, only ever
  appends one clearly-labeled extra suggestion — and it is off by default. Turn it off, or use
  the build that doesn't ship it at all, and the app is fully functional."
- **Money frame:** the **Off** toggle, then the single-candidate proposal bar.

### Closing (2:42–2:55, ~13s)

- **Screen:** Back to the board with the spoken sentence still shown; slow zoom out; airplane
  icon in frame.
- **Caption:** "Offline. On-device. Open-source. The user has the last word."
- **Voiceover:** "Symbols in, a grammatical spoken sentence out — offline, on-device, and the
  last word is always the user's."

---

## Shot list (for the person holding the camera / driving the screen recorder)

| # | Timecode | Shot | On-screen action to capture | Audio | Must be in frame |
|---|---|---|---|---|---|
| 0 | 0:00–0:20 | Cold open | Icon → board; pull status bar to reveal airplane icon | VO only | Airplane-mode icon |
| 1 | 0:20–0:42 | First run | "Speech setup" ready line; **Test voice**; **Continue** | **Device TTS** (test phrase) | "…ready…without internet" line |
| 2 | 0:42–1:08 | Compose | Tap io → volere → mangiare → pizza; strip fills | VO only | Colored cells + filling strip |
| 3 | 1:08–1:30 | Sentence | Proposal bar shows `io voglio mangiare la pizza` | VO only | Strip **and** proposal both visible |
| 4 | 1:30–1:54 | Cycle | Tap proposal → `una pizza` → tap back → `la pizza` | VO only | Proposal bar text changing |
| 5 | 1:54–2:16 | Speak | Tap **Speak** | **Device TTS** (full sentence) | Airplane icon + Speak tap + audio |
| 6 | 2:16–2:42 | AI off | Hold-open Settings → AI assistant **Off**; back to single candidate | VO only | The **Off** toggle |
| 7 | 2:42–2:55 | Close | Zoom out on board | VO only | Airplane icon |

**Two speaking moments only:** shot 1 (test phrase) and shot 5 (full sentence). Voiceover
must be silent during both.

---

## Clean voiceover read-through (record this as one take, then align to picture)

> "This is an offline pictogram communicator. You tap symbols, it builds a grammatical
> sentence, and — with the device in airplane mode — it speaks. The user always has the last
> word.
>
> On first run it shows the speech state instead of assuming it. The voice reports ready and
> offline… *(let the test phrase play)* …and that is the device talking, with no connection.
>
> You tap the symbols in order — subject, verb, verb, object. They collect at the top as a
> telegraphic sequence. No grammar yet.
>
> And there it is: the template conjugates *volere* to *voglio*, keeps the infinitive, inserts
> the article — a full sentence from four symbols, deterministically, on-device.
>
> Where the input is ambiguous it offers alternatives you cycle through — *la pizza*, *una
> pizza* — it never decides silently for you.
>
> Now the rule the whole app is built around: nothing is spoken until you tap Speak. *(let the
> full sentence play.)*
>
> And everything you just saw ran with the AI assistant off. The templates are the product;
> the optional model only ever appends one labeled suggestion, and it is off by default.
>
> Symbols in, a spoken sentence out — offline, on-device, and the last word is always the
> user's."

---

## Silent / captions-only variant

For autoplay-muted web or social embeds, drop the voiceover and rely on the burned-in captions
listed per beat. Keep the two device-audio moments (shots 1 and 5) — they still carry sound and
are the whole point. Add a one-line lower-third at shot 1 and shot 5: "🔊 device speaking —
airplane mode".

---

## Pre-flight checklist (run before you hit record)

- [ ] Voice data for the demo language installed; then **airplane mode ON**.
- [ ] Verify offline speech works: tap Speak once off-camera and confirm you hear it.
- [ ] AI assistant toggle set to **Off** (play flavor).
- [ ] Board seeded so io / volere / mangiare / pizza are all reachable on the home board without
      navigating into folders (keeps the take short).
- [ ] Screen recorder set to also capture internal audio.
- [ ] Do a full silent dry run once — confirm the airplane icon never disappears.

## Post-record checklist

- [ ] Airplane-mode icon visible in every beat, including both speaking moments.
- [ ] Real TTS audio present at shots 1 and 5 (not added in edit).
- [ ] Total runtime 2:00–3:00.
- [ ] No superlative/"first/only" wording in captions or voiceover.
- [ ] The AI-off toggle is legible on screen in shot 6.
- [ ] Caption present at least once noting the foss flavor has no network permission.
