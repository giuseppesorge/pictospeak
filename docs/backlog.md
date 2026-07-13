# Product backlog (post-POC)

Ideas validated by AAC field practice, ordered roughly by value/effort. Each item becomes
an ADR when picked up.

## High value, low effort

- **Sentence history**: the Speak action atomically confirms + speaks + appends to a local
  history; a History view offers past sentences as one-tap re-speakable items (through the
  same ConfirmationGate), with caregiver-visible delete. Disproportionately valuable for
  slow communicators.
- **Usage-based ranking**: a per-item `usageCount` (+ `lastUsed`) bumped on every selection;
  order suggestion rows (and optionally category grids) by it. Offline, zero-privacy-cost
  personalization with a single integer.
- **Pinned quick-fire row**: a small always-visible strip (Sì / No / Aiuto / Basta) that
  speaks immediately on tap — user-initiated fixed phrases, compatible with INVARIANT-1 by
  the same reasoning as speakWordPreview (exact tapped content, nothing generated).
- **Sentence-level pictograms**: a cell may carry a complete polite sentence ("Apri la
  finestra, per favore") that bypasses the builder and goes straight to the proposal bar.

## Access methods (Phase 2)

- **Dwell selection**: pointer-enter starts a cancellable timer, leave cancels, fire on
  expiry — with a visible progress ring, configurable dwell time, and a post-fire cooldown.
  Dwell must never trigger irreversible actions without confirmation.
- **Switch scanning**: row-column scanning driven by the hoisted highlighted-cell state;
  grid stays geometrically regular. Verify Android Switch Access works out of the box first.
- **No-scroll paging mode**: grid-density presets that always fill the screen exactly and
  page between screens — scrolling is hostile to switch/dwell access.
- **Selection debounce**: minimum time between selections; inert rest margins in the grid.
- **Stronger activation feedback**: long cell flash + optional sound/haptic; animate the
  selected pictogram into the message strip.

## Later

- **Full-catalog offline pack**: pipeline builds a complete-catalog zip published with
  releases; the app imports it via SAF (keeps the APK small; on F-Droid also sheds the
  NonFreeAssets flag from the base APK).
- **Board sharing**: caregiver board editor + JSON import/export between families
  (Open Board Format codec — ADR-0008).
- **Composer beyond the app**: an IME or AccessibilityService so composed text can reach
  messaging apps.
- **Environment intents**: home-automation actions (local-only, e.g. Home Assistant) on
  the same grid, behind the same confirmation discipline.

## Phase 2 — validation & deployment backlog

Items surfaced by putting the POC in front of therapists in a structured validation
session and by running it on real shared devices in the field. These are anticipated
needs, not observed defects; any concrete issue a session actually turns up becomes its
own golden test case and (if it changes a decision) an ADR. Rough sizes are order-of-magnitude
only — S ≈ a few days, M ≈ one to two weeks, L ≈ several weeks, XL ≈ its own milestone.

### Feedback & iteration loop

- **Therapist-feedback intake (offline)**: a disciplined, on-device channel for the
  reviewer to capture observations while using the app — e.g. a "flag this sentence /
  suggestion" affordance behind the caregiver gate that appends a structured, timestamped
  note (which board, which pictogram sequence, which candidate was shown vs. wanted) to a
  local review file, exportable via SAF. No network, no telemetry — it writes the same kind
  of atomic JSON the app already uses. Turns validation into an auditable input stream for
  vocabulary and grammar work instead of relying on memory. _(rough size: M)_

- **On-device session log for review**: an opt-in, caregiver-visible local log of what was
  selected, cycled, and rejected during a session, so the alternative-ordering and default
  candidate choices can be tuned against real usage. Strictly on-device and exportable only
  by the caregiver — this is a review aid, never analytics, and never leaves the device on
  its own. Pairs with the existing *Usage-based ranking* item but is scoped to review rather
  than runtime personalization. _(rough size: M)_

- **Golden-corpus growth from validation**: a lightweight workflow (and a small importer)
  to convert real sentence scenarios observed in a session into golden NLG test cases under
  the existing golden-test resources, with the linguistic justification captured alongside.
  Codifies the "every field observation becomes a fixture" practice so the spec grows from
  evidence rather than opinion. _(rough size: S)_

- **Vocabulary & board tuning workflow**: deployment reliably surfaces gaps in the curated
  core vocabulary and in the default boards for a given setting (home, school, clinic). A
  repeatable, reviewer-facing process — propose, review, version — for revising vocabulary
  and board layouts per site without forking the app, keeping changes as versioned data.
  _(rough size: M)_

### Language & reach

- **Additional language packs**: each further language pack beyond those already implemented
  is a full `catalog_{lang}` + `lexicon_{lang}` + Realizer + `boards/default_{lang}` + TTS
  locale chain, prioritized by validation demand. Every new pack is also a test of the
  language-pluggability promise: no language may leak into the engine core, RTL packs must
  work start/end-only with no mirrored pictograms. Budget one pack at a time. _(rough size: L
  per pack)_

### Input speed & access

- **Next-pictogram prediction (T9-style)**: offline, on-device prediction of the likely next
  pictogram(s) to cut the number of taps for slow communicators — the "predictive input"
  layer over the telegraphic sequence. Must stay deterministic-enough and privacy-preserving
  (driven by local usage/context, no cloud, no learning that leaves the device) and must only
  ever reorder or suggest *input* symbols — it never composes or speaks anything, so
  INVARIANT-1 is untouched. Start with frequency/recency, consider simple bigram context
  later. _(rough size: M–L)_

- **Switch / scanning field hardening**: refines the existing *Switch scanning*,
  *No-scroll paging mode*, and *Selection debounce* items with what real switch users need,
  which only a validation session reveals — scan timing and auto-scan tuning, making sure the
  confirmation step and the alternative-cycling are themselves reachable by scanning, and
  audible/visible scan cues. Depends on first confirming Android Switch Access behaves as
  expected on the floor device. _(rough size: M)_

### Interoperability & authoring

- **Board interoperability — OBF import/export**: an Open Board Format codec so boards can be
  exchanged with other AAC tools and moved between families and devices, rather than being
  locked into the app's internal JSON. Realizes the interop half of the existing *Board
  sharing* item (ADR-0008) and lowers the switching cost for anyone already invested in
  another tool's boards. Asset-license fences on imported pictograms must be respected on the
  way in and out. _(rough size: M)_

- **Caregiver board editor**: an in-app, caregiver-gated editor to build and rearrange boards
  and cells on the device itself — the authoring half of the existing *Board sharing* item.
  Validation consistently shows caregivers want to adapt vocabulary and layout in the moment
  without a separate desktop tool. Editing writes the same versioned JSON with atomic
  temp-file+rename, and stays behind the caregiver gate. _(rough size: L)_

### Deployment mechanics

- **Site provisioning & offline onboarding**: deploying to several shared low-end tablets at
  one site needs a way to pre-load a curated board set, the right language pack(s), and the
  required TTS voices, and to run a caregiver-driven first-run that provisions the device with
  no network step. Reduces per-device setup from a manual chore to a repeatable, fully offline
  procedure. _(rough size: M)_

- **TTS voice availability & fallback audit**: real low-end devices vary widely in which
  offline voices are installed and how usable they sound. A per-locale voice-availability
  check at setup, a guided (offline-where-possible) path to obtain a better voice, and a
  documented, graceful fallback chain when the preferred voice is absent — so first-run never
  dead-ends on a missing or unintelligible voice. Extends the existing first-run TTS setup
  rather than replacing it. _(rough size: S–M)_
