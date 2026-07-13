# Pipeline source snapshots (not shipped)

Versioned inputs for the offline content pipelines in `tools/`. Nothing under this
directory is packaged into the APK.

- `arasaac/snapshot-YYYY-MM-DD/` — full catalog metadata JSON per language as returned by
  the ARASAAC API on the snapshot date, kept so asset builds are reproducible and
  reviewable. Re-snapshot deliberately, per release, never automatically.
