# ADR-0006: Apache-2.0 code license with strictly split asset licensing

**Status**: accepted

## Context
The pictograms (CC BY-NC-SA) and lexicon source (LGPL option) have their own licenses.
GPL code licensing has a documented history of GPL-vs-CC bundling doubt in this exact
asset ecosystem, and would block linkage of some MPL-1.x linguistic resources.

## Decision
Code: Apache-2.0. Assets: license-fenced directories each carrying their own LICENSE,
ATTRIBUTION/PROVENANCE files, all mapped in THIRD_PARTY_NOTICES.md. Apache-2.0 is one-way
compatible into GPLv3, so the project can later be folded into a copyleft umbrella; the
reverse would be impossible.

## Note
Revisit window closes at first external code contributions.
