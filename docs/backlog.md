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
