# ADR-0011: Languages ship as packs, engine core stays language-free

**Status**: accepted

## Context
The app targets many languages over time, but each shipped language needs a rule-based
baseline (hard rule 3) and symbol-set coverage varies drastically per language.

## Decision
Every language is a LanguagePack (docs/language-packs.md): catalog + lexicon + Realizer +
golden corpus + boards + TTS chain. The engine core contains no language-specific code
(arch-tested). Rollout follows measured symbol coverage and morphology effort; languages
without ARASAAC coverage (hi/ja/ur) are blocked on a multi-symbol-set layer, not on NLG.
RTL correctness is a property of the UI, not of any pack.
