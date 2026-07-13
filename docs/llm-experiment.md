# LLM experiment — protocol and results

The template engine is the product. The on-device LLM is a bounded experiment that can
only ever add one labeled extra candidate (see `architecture.md`, INVARIANT-1 and hard
rule 3). This document is the protocol; results are appended as they are measured.

## Candidates

- **Primary**: Gemma 3 270M, QLoRA fine-tuned for the narrow task
  "pictogram keyword sequence → grammatical sentence", exported to `.litertlm`.
  Rationale: the only generative model class with a credible path onto low-RAM devices
  (~300 MB q8 file; sub-500 MB runtime footprint claimed for int4 fine-tunes).
- **Backup**: Qwen3-0.6B int4 (Apache-2.0 — simplest licensing), for ≥4 GB devices only.
- **Explicitly rejected**: 1B-class models on the 2 GB tier (~1.0–1.15 GB resident RAM;
  vendor guidance recommends ≥4 GB devices).

Runtime: LiteRT-LM (`com.google.ai.edge.litertlm`, pinned in the version catalog),
CPU backend only. All symbols confined to `LiteRtSentenceRefiner` (ADR-0004).

## Device gate (before the feature is even visible)

`arm64-v8a ∈ Build.SUPPORTED_64_BIT_ABIS` (the runtime has no 32-bit libs) AND
`!ActivityManager.isLowRamDevice()` AND `totalMem` above a per-model threshold
(≥3.5–4 GB for anything 0.6B+; the 270M threshold is an experiment OUTPUT, not an
assumption). Plus: play flavor ∧ per-profile opt-in (default off).

## Protocol

1. Build an eval set of 100–200 pictogram keyword sequences → gold sentences (reuse the
   golden corpus + real board compositions).
2. Baseline raw `gemma3-270m-it` (q8) and Qwen3-0.6B (int4) with a fixed short rewrite
   prompt, ≤25 output tokens.
3. QLoRA fine-tune 270M on synthetic pairs; convert to `.litertlm`.
4. Measure on BOTH tiers (floor tablet + ≥4 GB phone), model imported via SAF.

## Go/no-go criteria (all measured, none guessed)

| Criterion | Threshold |
|---|---|
| Peak RSS during 20 consecutive generations | ≤ 50–60% of device totalMem, zero LMK kills |
| Engine load time | < 10 s (kept warm afterwards) |
| Full-sentence latency (≤25 tokens, CPU) | ≤ 4 s |
| Grammatical acceptability on the eval set | ≥ 90% |
| Human preference vs. template output | ≥ 60% |

Any criterion failing on a tier → templates-only on that tier. A documented no-go is a
valid experiment result.

## Results

*None yet.*
