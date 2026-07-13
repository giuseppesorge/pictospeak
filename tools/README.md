# Offline content pipelines

The app never touches the network — these tools are the only place downloads happen.
They run manually, per release; their outputs are committed and versioned.

- **arasaac-fetch/** *(lands at M2)* — snapshots the ARASAAC catalog API for each shipped
  language into `assets-src/arasaac/snapshot-YYYY-MM-DD/`, downloads the curated pictogram
  subset listed in `core-vocabulary.csv` (rate-limited, ≤3 req/s), converts to 256px WebP,
  and emits `app/src/main/assets/arasaac/`: `{id}.webp`, `catalog_{lang}.json`,
  `attribution-manifest.json`, `ATTRIBUTION.md`, `LICENSE`.
- **lexicon-build/** *(lands at M2)* — builds `app/src/main/assets/lexicon/lexicon_{lang}.json`
  from the language's morphological source (Italian: Morph-it!, LGPL option), plus the
  reference inflection table used by `:nlg` property tests, plus `PROVENANCE.md` + `LICENSE`.
- **llm-lab/** *(lands with the LLM experiment)* — evaluation set, prompts, fine-tuning
  notes and the measured go/no-go protocol from `docs/llm-experiment.md`.
- **core-vocabulary.csv** — the curated board vocabulary: one row per pictogram with the
  selection rationale. Deliberately a reviewable artifact for speech therapists.
