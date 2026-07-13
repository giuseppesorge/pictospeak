# llm-lab — M6 on-device LLM experiment

The template engine is the product. The on-device LLM is a **bounded, optional** experiment
that can only ever append ONE labeled extra candidate (CLAUDE.md hard rule 3, INVARIANT-1).
A documented **no-go is a valid, pitch-usable result** — "we measured it; 2 GB can't run a
generative model well, so PictoSpeak ships templates-only there" is a finding, not a failure.

This folder holds the offline eval assets. Nothing here runs on the device or touches the
network from the app — fine-tuning and conversion happen on a workstation / free Colab; the
resulting `.litertlm` is moved onto the device by the user via SAF.

## What already ships in the app (M6 code, done)

- `:llm/LiteRtSentenceRefiner` — wired to LiteRT-LM (`Engine`/`Session`, CPU backend, model
  loaded once and kept warm, released on low memory). Never throws; degrades to "no proposal".
- `:llm/LlmRewrite` — the pure prompt + response-cleaning logic (JVM-tested). **The prompt
  here (`prompt-template.txt`) MUST match `LlmRewrite.buildPrompt`** — fine-tune on that shape.
- `:app` `DeviceGate` — arm64 + `!isLowRamDevice` + RAM ≥ threshold (default 3.5 GB; the
  270M-on-2GB threshold is an OUTPUT of this experiment, not an assumption).
- `:app` `ModelStore` — SAF import into `filesDir/models/`, SHA-256 + size shown, license
  acceptance recorded (llm/NOTICE-models.md).
- Settings → "AI assistant (experimental)" (play flavor only): device status, import model,
  accept license, enable. Default OFF. Foss build has none of this and no `:llm` at all.

The feature is only ever built when EVERY gate passes: play flavor ∧ device eligible ∧ model
imported ∧ license accepted ∧ opt-in. Otherwise `RefinerFactory.create` returns null.

## Candidates (docs/llm-experiment.md)

- **Primary**: Gemma 3 270M, QLoRA fine-tuned for "pictogram keyword sequence → sentence".
- **Backup (≥4 GB only)**: Qwen3-0.6B int4 (Apache-2.0, simplest licensing).
- **Rejected**: 1B-class on 2 GB; AI21 Jamba 3B (SSM — no LiteRT-LM path).

## Eval assets

- `eval-set.tsv` — `lang <TAB> words <TAB> gold`. The telegraphic tap sequence and a natural
  faithful target. Extend it; keep it therapist-reviewable.
- `prompt-template.txt` — the exact shared prompt (`{WORDS}`, `{DRAFT}` placeholders).

## Runbook (workstation + free Colab, then the device)

1. **Build training pairs.** Expand `eval-set.tsv` into instruction pairs using
   `prompt-template.txt` (input) → `gold` (output). Hold out ~20% as the test split; never
   train on the test rows. Synthesize additional pairs from real board compositions.
2. **QLoRA fine-tune Gemma 3 270M** on free Colab (T4). Short outputs (≤25 tokens), low
   temperature. Keep the adapter small; merge for export.
3. **Convert to `.litertlm`** with the LiteRT-LM converter, q8 (and int4 if it fits the tier).
   Never commit the weights (llm/NOTICE-models.md; `check-licenses.sh` blocks `*.litertlm`).
4. **Install the play build** and import the model: Settings → AI assistant → Import model →
   accept license → enable. (`adb push` the file to the device's Download first if needed.)
5. **Measure per tier** — floor tablet (2 GB) AND one ≥4 GB phone (Redmi Note 15 / Samsung):

   | Criterion | Threshold |
   |---|---|
   | Peak RSS over 20 consecutive generations | ≤ 50–60% of totalMem, zero LMK kills |
   | Engine load time | < 10 s (kept warm after) |
   | Full-sentence latency (≤25 tokens, CPU) | ≤ 4 s |
   | Grammatical acceptability on the test split | ≥ 90% |
   | Human preference vs. template output | ≥ 60% |

   Any criterion failing on a tier ⇒ **templates-only on that tier** (still a valid result).
6. **Record** the numbers in `docs/llm-experiment.md` → Results, per model per tier, with the
   device model and date. That table is the M6 deliverable.

## Scoring notes

- *Grammatical acceptability*: a native speaker marks each test output grammatical / not.
  Count only faithful outputs (must not drop or invent content words).
- *Human preference*: blind A/B of template vs. LLM sentence for the same input; count LLM
  wins. Ties split. ≥60% LLM wins to justify surfacing the extra candidate.
- The app already de-duplicates: if the LLM merely echoes the template draft, no extra
  candidate is shown (`LlmRewrite.cleanResponse`). Preference is only meaningful on rows
  where the LLM actually differs.
