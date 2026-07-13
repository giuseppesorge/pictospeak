# Lexicon provenance

- Source: Morph-it! v0.48 (505,074 forms), Zanchetta & Baroni, University of Bologna.
- Download: https://docs.sslmit.unibo.it/doku.php?id=resources:morph-it
  (archive `morph-it.tgz`, kept in `assets-src/morph-it/`, sha256 `b137343dc095e038ebfaef7c743185e85ec35f5322a1b8629b600e6e898c7024`).
- Derivation: `./gradlew :tools:lexicon-build:run` — selects the curated vocabulary's
  lemmas (`tools/core-vocabulary.csv`), extracts present indicative, past participle,
  noun gender/number and adjective agreement forms; merges the hand-curated
  auxiliary table `tools/lexicon-build/aux_it.csv` (essere/avere is lexical data,
  not rule-derivable) and the data patches in `tools/lexicon-build/overrides_it.csv`.
- Encoding note: Morph-it! is ISO-8859-1; outputs are UTF-8.
