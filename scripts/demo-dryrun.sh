#!/usr/bin/env bash
# Airplane-mode demo dry-run on a connected physical device: proves the whole offline path
# (compose -> propose -> confirm -> speak) works with no network. Automates what it can and
# asks the operator to confirm the audio (a script cannot hear the device).
# Usage: scripts/demo-dryrun.sh
set -euo pipefail

ADB="${ADB:-adb}"
PKG="io.github.giuseppesorge.pictospeak"

"$ADB" get-state >/dev/null 2>&1 || { echo "No device connected" >&2; exit 1; }
DEVICE=$("$ADB" shell getprop ro.product.model | tr -d '\r')
echo "Device: $DEVICE"

# 1. Airplane mode ON (the whole point: offline).
echo "Enabling airplane mode…"
"$ADB" shell cmd connectivity airplane-mode enable 2>/dev/null || \
  { "$ADB" shell settings put global airplane_mode_on 1; "$ADB" shell am broadcast -a android.intent.action.AIRPLANE_MODE >/dev/null; }
AIRPLANE=$("$ADB" shell settings get global airplane_mode_on | tr -d '\r')
[ "$AIRPLANE" = "1" ] && echo "  airplane mode: ON" || echo "  WARNING: could not confirm airplane mode — enable it by hand"

# 2. Fresh launch.
"$ADB" shell am force-stop "$PKG"
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4

# 3. A TTS engine + a voice must exist (Google 'Speech Recognition & Synthesis' or similar).
# `|| true`: under `set -euo pipefail` a no-match grep (device without a Google TTS package)
# would abort the whole script before its own "no engine" warning below.
ENGINE=$("$ADB" shell "pm list packages | grep -iE 'com.google.android.tts|espeak|tts' || true" | head -1 | tr -d '\r')
[ -n "$ENGINE" ] && echo "  TTS engine present: $ENGINE" || echo "  WARNING: no TTS engine package found"

cat <<'STEPS'

Now drive the demo by hand on the device (the app is open, offline):
  1. Complete first-run setup if shown ("Continue").
  2. Tap:  io/I  ->  volere/want  ->  mangiare/eat.
  3. Open the food folder, tap pizza.
  4. Confirm the proposal reads "io voglio mangiare la pizza" / "I want to eat the pizza".
  5. Tap Speak.

STEPS

read -r -p "Did the device SPEAK the sentence, in the selected language, with no network? [y/N] " ok
if [ "$ok" = "y" ]; then
  echo "DEMO DRY-RUN: PASS (record device + language in docs/benchmarks.md)"
else
  echo "DEMO DRY-RUN: FAIL — investigate TTS readiness (first-run wizard) before demoing"
  exit 1
fi
