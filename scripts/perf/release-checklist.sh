#!/usr/bin/env bash
# Human-attested physical-device gates before any release (docs/perf-budgets.md).
# This script does not measure — it makes skipping a gate an explicit act.
set -euo pipefail

GATES=(
  "Cold TTID <= 2.0s on the FLOOR device (:benchmark StartupBenchmark, numbers in docs/benchmarks.md)"
  "Grid jank < 1% on fast fling over 500 cells (FrameTimingMetric)"
  "30-min soak: steady PSS <= 250MB, zero LMK kills (scripts/perf/soak.sh)"
  "Confirm tap -> audio <= 500ms warm (UtteranceProgressListener.onStart)"
  "TTS matrix: voice present/absent, install flow, airplane-mode speak"
  "Airplane-mode end-to-end demo dry-run on BOTH devices"
  "Accessibility pass: TalkBack labels, touch targets >= 48dp"
  "Base foss APK <= 12MB"
)

echo "Release checklist — answer y only with measured evidence recorded in docs/benchmarks.md"
for gate in "${GATES[@]}"; do
  read -r -p "[y/N] $gate " answer
  [[ "$answer" == "y" ]] || { echo "NOT RELEASABLE: gate failed/skipped: $gate"; exit 1; }
done
echo "All gates attested. Tag and release per docs/handover.md."
