# UI conventions

## Fitzgerald color coding

Grid cells use the modified Fitzgerald key — the color convention AAC users and speech
therapists already know. One palette, applied consistently, defined in ONE place
(`ui/board/Fitzgerald.kt`):

| Slot | Color | ARASAAC POS type code |
|---|---|---|
| PEOPLE | yellow `0xFFFFF176` | 1 (proper names) |
| NOUNS | orange `0xFFFFB74D` | 2 |
| VERBS | green `0xFF81C784` | 3 |
| DESCRIPTORS | blue `0xFF64B5F6` | 4 (adjectives/adverbs) |
| SOCIAL | pink `0xFFF06292` | 5 |
| MISC | white `0xFFFFFFFF` | 6 |

## Board layout

- **Top message window** (selection strip): selected pictograms accumulate in reading
  order; tap an item to remove it. Below it, the proposal bar shows the current sentence
  candidate (tap to cycle alternatives) and the speak/backspace/clear controls.
- **Grid**: homogeneous fixed-size cells (`LazyVerticalGrid`, stable `key`, single
  `contentType`, trivial cell content: one image + one label). Grid density is a profile
  setting; presets from 3×2 up to 6×10 cover the motor-ability range.
- **Core + fringe**: the home board pins high-frequency core vocabulary in stable
  positions; fringe vocabulary lives in category folders. Stable positions matter — motor
  planning is how AAC users get fast.
- **Touch targets** ≥ 48dp always. TalkBack labels on every actionable element.

## RTL rules (always on, not per-language)

- `android:supportsRtl="true"`; **only** `start`/`end` in layouts and modifiers — never
  `left`/`right` (lint enforces this).
- The selection strip and grid follow layout direction automatically; sentence text uses
  the platform's bidi handling.
- **Pictograms are never mirrored** — only navigational icons may auto-mirror.
- Screenshot-test the board under "Force RTL layout direction" before release.

## Scanning readiness (future access method)

Row-column switch scanning is a planned access method. The rules that keep it cheap:

- The grid stays geometrically regular (no spanned cells on the board).
- Cell focus/highlight state is hoisted ViewModel state (an externally drivable
  "highlighted cell" — not internal Compose focus), so an external scanner loop can drive
  it without touching cell composables.
- No gesture-only interactions: everything reachable by simple taps.
