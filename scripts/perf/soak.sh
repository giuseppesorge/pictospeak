#!/usr/bin/env bash
# 30-minute memory soak on a connected physical device (docs/perf-budgets.md).
# Samples PSS every 15s and watches logcat for Low Memory Killer events.
# Usage: scripts/perf/soak.sh [package] [minutes]
set -euo pipefail

PKG="${1:-io.github.giuseppesorge.pictospeak}"
MINUTES="${2:-30}"
OUT_DIR="$(mktemp -d /tmp/pictospeak-soak.XXXXXX)"
SAMPLES="$OUT_DIR/pss.csv"
LMK_LOG="$OUT_DIR/lmk.log"

adb get-state >/dev/null 2>&1 || { echo "No device connected" >&2; exit 1; }

echo "Soaking $PKG for $MINUTES min. Interact with the app (scroll, compose, speak)."
echo "timestamp,totalPssKb" > "$SAMPLES"

adb logcat -c
adb logcat -s lowmemorykiller ActivityManager:E > "$LMK_LOG" &
LOGCAT_PID=$!
trap 'kill $LOGCAT_PID 2>/dev/null || true' EXIT

END=$((SECONDS + MINUTES * 60))
while ((SECONDS < END)); do
  PSS=$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | awk '/TOTAL PSS:/ {print $3; exit}')
  echo "$(date +%s),${PSS:-app-not-running}" >> "$SAMPLES"
  sleep 15
done

kill $LOGCAT_PID 2>/dev/null || true

PEAK=$(awk -F, 'NR>1 && $2+0>max {max=$2} END {print max+0}' "$SAMPLES")
KILLS=$(grep -c "$PKG" "$LMK_LOG" 2>/dev/null || true)

echo
echo "Samples:   $SAMPLES"
echo "LMK log:   $LMK_LOG"
echo "Peak PSS:  $((PEAK / 1024)) MB   (budget: steady-state <= 250 MB)"
echo "LMK hits:  ${KILLS:-0}          (budget: zero)"
[[ "${KILLS:-0}" == "0" ]] || { echo "SOAK FAIL: app was LMK-killed"; exit 1; }
