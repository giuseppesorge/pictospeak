# ADR-0001: Jetpack Compose, with a real-hardware gate and a Views fallback

**Status**: accepted (gate pending measurement)

## Context
The board UI is one homogeneous grid — Compose's best case — and Compose is the toolkit
current and future Android contributors know (View-based Jetpack libraries are in
maintenance mode). But published Compose-vs-Views parity numbers come from mid-range
hardware; the only genuinely low-end published data predates Compose 1.9. Our launch
criterion is a ~2GB Android 10 tablet.

## Decision
Use Compose with mandatory mitigations from release one: app-specific Baseline Profile and
R8 full mode (minify + shrink). Before any real UI is built on top, a throwaway 500-cell
grid spike is Macrobenchmarked on the physical floor device. Budget: cold TTID < 2s,
< 1% janky frames on a fast fling.

## Fallback trigger
If the spike misses budget with mitigations applied, the board screen (only) is rebuilt in
Views/RecyclerView behind the unchanged ViewModel. The UI layer is a thin shell over
StateFlow precisely to keep this fallback bounded to one screen.

## Consequences
Modern, maintainable UI; a measured (not hoped) performance story; one week of gate work
before feature UI starts.
