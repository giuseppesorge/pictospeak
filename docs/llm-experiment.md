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
- **Evaluated and rejected — AI21 Jamba Reasoning 3B** (2026-07, fact-checked): Apache-2.0
  and officially multilingual (incl. Italian), but its hybrid Mamba/SSM architecture cannot
  run on LiteRT-LM (decoder-only transformers only; no converter building blocks, no
  community conversion) — the only mature Android runtime is llama.cpp, rejected in
  ADR-0004; footprint is 8 GB-tier (Q4 ≈ 1.9 GB file, ~2.5 GB inference RAM); the chat
  template hard-wires `<think>` reasoning with no off-switch, blowing the ≤4 s latency
  budget at phone-CPU speeds; fine-tuning tooling immature vs Gemma's pipeline. Revisit
  only if LiteRT-LM adds SSM/hybrid support.

Runtime: LiteRT-LM (`com.google.ai.edge.litertlm`, pinned in the version catalog),
CPU backend only. All symbols confined to `LiteRtSentenceRefiner` (ADR-0004).

## Implementation status (M6 code — done; measurement — pending hardware + model)

The full software path is wired, compiles into the **play flavor only**, and is unit-tested;
what remains is the QLoRA fine-tune (free Colab) and the on-device measurements below.

- `:llm/LiteRtSentenceRefiner` drives LiteRT-LM (`Engine(EngineConfig(modelPath, Backend.CPU(),
  maxNumTokens)).initialize()` → `createSession(SessionConfig())` →
  `generateContent(listOf(InputData.Text(prompt)))`). The engine is loaded lazily and kept
  warm; inference is serialized; `close()` releases native memory on `onTrimMemory`. It NEVER
  throws — any error degrades to null ("no proposal"); the template candidates are always
  already published.
- `:llm/LlmRewrite` (pure, JVM-tested) builds the language-neutral prompt and cleans the
  response (first line, strip label/quotes, collapse whitespace, reject blank / over-long /
  duplicate-of-draft). **`tools/llm-lab/prompt-template.txt` mirrors this — fine-tune on it.**
- Device gate `:app/DeviceGate`: `arm64-v8a` ∈ `SUPPORTED_64_BIT_ABIS` ∧ `!isLowRamDevice` ∧
  `totalMem ≥ DEFAULT_MIN_TOTAL_MEM_BYTES` (3.5 GB default — the 270M-on-2GB threshold is an
  OUTPUT of this experiment; lower it only once measured).
- Model handling `:app/ModelStore`: SAF import into `filesDir/models/`, streamed SHA-256 +
  size, single-model (re-import replaces), atomic write. License acceptance is recorded in the
  profile (`llmModelLicenseAccepted`); weights are never bundled (llm/NOTICE-models.md).
- The refiner is assembled only when EVERY gate passes: play flavor ∧ `llmEnabled` ∧
  `llmModelLicenseAccepted` ∧ device eligible ∧ a model imported. Otherwise null. Default OFF.
- Runbook to obtain numbers: `tools/llm-lab/README.md`.

## Device gate — minimum characteristics (no cost, fully offline, local)

The on-device LLM is **free and offline**: it runs locally with no network and no per-token
billing (a "token" here is only the free Hugging Face access string used to *download* the
open weights). The gate is purely about whether the hardware can run the model well.

Minimum characteristics (`:app/DeviceGate`):

- **`arm64-v8a`** ∈ `Build.SUPPORTED_64_BIT_ABIS` — the LiteRT-LM runtime ships no 32-bit
  libraries. Hard requirement.
- **not** `ActivityManager.isLowRamDevice()`.
- **RAM ≥ max(absolute floor 3 GB, model file size × 4)** — the requirement scales with the
  imported model, because the model's peak RSS ≈ weights + KV/activations (≈2×) and that peak
  must stay near half of total RAM (the go/no-go budget). So:

  | Model | Approx. file | Min device RAM (× 4, ≥ floor) | Runs on the 2 GB floor? |
  |---|---|---|---|
  | Gemma 3 270M (int4 QAT ~125 MB / q8 ~300 MB) | ~0.1–0.3 GB | **~3 GB** (the floor) | maybe — an experiment OUTPUT (soak/LMK), not an assumption |
  | Qwen3-0.6B (int4) | ~1 GB | **~4 GB** | no |

- Storage: room for the model file (~0.1–1 GB) in `filesDir/models/`.
- OS: Android 10+ (app minSdk 29).
- Plus the non-hardware gates: play flavor ∧ per-profile opt-in (default off) ∧ model imported
  ∧ its license accepted.

The 3 GB absolute floor is deliberately above the 2 GB launch-floor tablet until a soak test
proves a tiny model survives that device's low-memory killer — a measured OUTPUT of this
experiment. If a model is imported that the device's RAM can't satisfy, Settings says so
(never a silent no-op) and the feature stays off.

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

### 2026-07-13 — first on-device run, ≥4 GB tier (Redmi Note 15 5G, Android 16, 7.2 GB, MediaTek Dimensity)

Model: **Qwen3-0.6B int4** (`litert-community/Qwen3-0.6B`, `qwen3_0_6b_mixed_int4.litertlm`,
498 MB, Apache-2.0, ungated — no account/token), **base (not fine-tuned)**, imported via SAF,
CPU/XNNPACK backend.

| Criterion | Threshold | Result |
|---|---|---|
| Pipeline works end-to-end | — | **PASS** — model imported, opened, XNNPACK CPU delegate initialized, generation runs (logcat: `litert_lm_loader` → `TfLiteXNNPackDelegate`) |
| Engine load time | < 10 s | ~6 s to build both subgraphs (acceptable) |
| Full-sentence latency | ≤ 4 s | **FAIL** — a single generation ran **>120 s** (logcat monitor-contention). The base model rambles toward the token cap; the device was also swapping heavily (~1.6 GB) under normal app load |
| Grammatical acceptability | ≥ 90% | not measured (needs the fine-tuned model) |
| Human preference | ≥ 60% | not measured |

**Verdict (≥4 GB tier, base model): NO-GO at the 4 s interactive budget.** The wiring is proven
on real hardware, but an un-fine-tuned 0.6B is far too slow. Follow-on fixes shipped from this
run: total-token cap 512→128 and `Session.cancelProcess()` on timeout so a runaway generation
no longer holds the engine lock for minutes.

**2 GB floor tier:** gated out by design (RAM < the 3 GB `DeviceGate` floor) — **templates-only**,
as intended.

**Decision:** M6 closed as a documented measurement. The template engine carries the product
(IT+EN, offline sentence NLG); the LLM stays an optional, off-by-default extra. A bounded QLoRA
fine-tune (short output → likely fast) remains available in `tools/llm-lab/README.md` if a
mainstream-device "AI suggestion" demo is ever wanted, but it is not on the critical path and
does not affect the 2 GB launch criterion.
