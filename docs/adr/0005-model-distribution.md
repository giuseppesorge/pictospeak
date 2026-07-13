# ADR-0005: Model weights via user-side import; never bundled

**Status**: accepted

## Context
Candidate model files are 250–530MB with non-trivial license flow-down obligations
(Gemma Terms of Use). The repo and all release artifacts must stay clean.

## Decision
Weights are NEVER committed, bundled, or rehosted. POC: the caregiver imports a .litertlm
file via SAF; the app shows SHA-256 + size and records license acceptance
(llm/NOTICE-models.md). If the experiment graduates on Play, Play Asset Delivery
install-time packs are the documented path. Qwen (Apache-2.0) is preferred over Gemma if
bundling is ever reconsidered.
