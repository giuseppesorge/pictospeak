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

## Fine-tune walkthrough (verified against Google AI Edge docs, 2026-07)

Two phases. **Do Phase 0 first** — it proves the whole on-device path works before you spend
any time training. Every command below was checked against the LiteRT-LM "convert-and-run"
tutorial and the litert-community model card; pin/verify tool versions as they move (LiteRT-LM
is pre-1.0).

Prerequisites: a Hugging Face account, and **accept the Gemma Terms of Use** at
`huggingface.co/google/gemma-3-270m-it` (the model is gated). Gemma weights carry flow-down
obligations — never commit or rehost them (llm/NOTICE-models.md).

### Phase 0 — validate the pipeline with the pre-converted baseline (~30 min, no training)

1. Desktop smoke test (downloads the ready-made `.litertlm`):
   ```bash
   pip install -U litert-lm
   huggingface-cli login                      # paste your HF token
   litert-lm run --from-huggingface-repo=litert-community/gemma-3-270m-it \
     --prompt="Rewrite into one natural Italian sentence: io volere mangiare pizza"
   ```
2. Find the downloaded file and copy it onto the phone, then import it in the app:
   ```bash
   # the model lands in the HF cache; locate the .litertlm it just ran
   find ~/.cache/huggingface -name '*.litertlm' | head
   adb push <that-file>.litertlm /sdcard/Download/gemma3-270m.litertlm
   ```
   On the phone (play flavor): Settings → "AI assistant" → Import model → pick it in Download
   → accept the license → enable. Compose a sentence and confirm a labeled "AI suggestion"
   candidate appears. **This validates :llm/LiteRtSentenceRefiner + ModelStore + DeviceGate on
   real hardware.** If the raw baseline's Italian is weak, that's expected — Phase 1 fixes it.

### Phase 1 — QLoRA fine-tune for the pictogram→sentence task

1. **Build the training data** (pure Python, local):
   ```bash
   cd tools/llm-lab && python3 build_training_data.py     # → data/train.jsonl, data/val.jsonl
   ```
   The seed set is small (~45 rows); **augment it** with more real board compositions before a
   serious run. Upload `train.jsonl` + `val.jsonl` to the Colab session.

2. **Fine-tune on a free Colab T4** (paste into cells; TRL's API moves — adjust arg names to
   your installed version):
   ```python
   !pip install -q -U transformers trl peft bitsandbytes accelerate datasets
   from huggingface_hub import login; login()            # HF token; Gemma ToU accepted
   import torch
   from datasets import load_dataset
   from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
   from peft import LoraConfig, PeftModel
   from trl import SFTTrainer, SFTConfig

   MODEL = "google/gemma-3-270m-it"
   tok = AutoTokenizer.from_pretrained(MODEL)
   bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_quant_type="nf4",
       bnb_4bit_compute_dtype=torch.bfloat16, bnb_4bit_use_double_quant=True)
   model = AutoModelForCausalLM.from_pretrained(MODEL, quantization_config=bnb,
       device_map="auto", attn_implementation="eager")

   ds = load_dataset("json", data_files={"train":"train.jsonl","validation":"val.jsonl"})
   peft = LoraConfig(r=16, lora_alpha=32, lora_dropout=0.05, bias="none", task_type="CAUSAL_LM",
       target_modules=["q_proj","k_proj","v_proj","o_proj","gate_proj","up_proj","down_proj"])
   cfg = SFTConfig(output_dir="out", num_train_epochs=3, per_device_train_batch_size=8,
       gradient_accumulation_steps=2, learning_rate=2e-4, lr_scheduler_type="cosine",
       warmup_ratio=0.03, logging_steps=10, bf16=True, max_length=256,
       completion_only_loss=True, report_to="none")   # train only on the completion
   SFTTrainer(model=model, args=cfg, train_dataset=ds["train"],
       eval_dataset=ds["validation"], peft_config=peft, processing_class=tok).train()
   ```

3. **Merge the adapter and push to HF** (export needs a plain HF model, not a bare adapter):
   ```python
   base = AutoModelForCausalLM.from_pretrained(MODEL, torch_dtype=torch.bfloat16)
   merged = PeftModel.from_pretrained(base, "out").merge_and_unload()
   merged.save_pretrained("gemma3-270m-pictospeak"); tok.save_pretrained("gemma3-270m-pictospeak")
   merged.push_to_hub("YOUR-USER/gemma3-270m-pictospeak", private=True)
   tok.push_to_hub("YOUR-USER/gemma3-270m-pictospeak", private=True)
   ```

4. **Convert to `.litertlm`** (same tool as Phase 0; check `--help` for q8/int4 quant flags):
   ```bash
   !uv tool install litert-torch-nightly litert-lm
   !litert-torch export_hf --model=YOUR-USER/gemma3-270m-pictospeak \
       --output_dir=/content/out-litertlm --externalize_embedder
   !litert-lm run /content/out-litertlm/model.litertlm --prompt="Words: io andare casa\nDraft: io andare casa\nSentence:"
   ```
   Download `/content/out-litertlm/model.litertlm`.

5. **Import + measure on real hardware** (the actual M6 deliverable):
   ```bash
   adb push model.litertlm /sdcard/Download/gemma3-270m-pictospeak.litertlm
   ```
   Re-import in the app, then run the go/no-go protocol per tier (floor tablet + a ≥4 GB phone)
   and record the numbers in `docs/llm-experiment.md` → Results. A documented **no-go on 2 GB**
   is a valid result.

Notes: the training Draft is the telegraphic word sequence (worst-case), so the model learns to
always produce a grammatical sentence. For higher fidelity, regenerate the Draft field from the
real template engine's output and retrain. The app de-duplicates: if the model just echoes the
template draft, no extra candidate is shown (LlmRewrite.cleanResponse).
