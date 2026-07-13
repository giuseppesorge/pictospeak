# ADR-0009: Grammar v1 scope frozen

**Status**: accepted

## Context
Hand-rolled NLG stays maintainable only if its scope is fenced. Grammatical completeness
is a research project; a reliable, testable fragment is a product.

## Decision
docs/grammar-v1.md lists the included phenomena and the explicit exclusion list per
language. Any addition requires a failing real-user scenario + a new ADR. The engine stays
under ~2k LOC per language; unmatched input falls back to concatenation, missing data to
citation forms.
