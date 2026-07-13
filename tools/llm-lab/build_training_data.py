#!/usr/bin/env python3
"""Expand eval-set.tsv into instruction pairs for QLoRA fine-tuning (docs/llm-experiment.md).

Pure Python, no ML dependencies — run it anywhere. It reads the same eval set and the same
prompt template the app uses at runtime (LlmRewrite.buildPrompt), so the model is trained on
EXACTLY the prompt shape it will see on-device.

For each eval row it emits {"prompt": <rendered prompt>, "completion": <gold sentence>}, then
splits into train/val JSONL (the val split is the held-out set for the go/no-go grammaticality
and human-preference scoring — never train on it).

The training draft is the telegraphic word sequence (the worst-case "concat fallback" draft),
so the model learns to always produce a grammatical sentence — which is exactly the case where
the template engine can't help. For higher fidelity you can regenerate the Draft field from the
real template engine's output; see the README.

Usage:
    python3 build_training_data.py            # writes data/train.jsonl + data/val.jsonl
    python3 build_training_data.py --val 0.15 # custom validation fraction
"""
from __future__ import annotations

import argparse
import json
import pathlib

HERE = pathlib.Path(__file__).parent
EVAL_SET = HERE / "eval-set.tsv"
PROMPT_TEMPLATE = HERE / "prompt-template.txt"
OUT_DIR = HERE / "data"


def load_rows(path: pathlib.Path) -> list[tuple[str, str, str]]:
    rows: list[tuple[str, str, str]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.rstrip("\n")
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) != 3:
            continue
        lang, words, gold = (p.strip() for p in parts)
        if lang == "lang" and words == "words":  # header row
            continue
        rows.append((lang, words, gold))
    return rows


def render_prompt(template: str, words: str, draft: str) -> str:
    return template.replace("{WORDS}", words).replace("{DRAFT}", draft)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--val", type=float, default=0.15, help="validation fraction (default 0.15)")
    args = ap.parse_args()

    template = PROMPT_TEMPLATE.read_text(encoding="utf-8").rstrip("\n")
    rows = load_rows(EVAL_SET)
    if not rows:
        raise SystemExit(f"no rows parsed from {EVAL_SET}")

    examples = []
    for lang, words, gold in rows:
        # Draft = telegraphic words (the concat-fallback shape) so the model always learns to
        # produce grammatical output; runtime substitutes the real template draft.
        prompt = render_prompt(template, words=words, draft=words)
        examples.append({"lang": lang, "prompt": prompt, "completion": " " + gold})

    # Deterministic split (stride) so it's reproducible without a RNG: every 1/val-th row → val.
    val_every = max(2, round(1 / args.val)) if args.val > 0 else 0
    train, val = [], []
    for i, ex in enumerate(examples):
        (val if val_every and i % val_every == 0 else train).append(ex)

    OUT_DIR.mkdir(exist_ok=True)
    for name, split in (("train", train), ("val", val)):
        out = OUT_DIR / f"{name}.jsonl"
        with out.open("w", encoding="utf-8") as f:
            for ex in split:
                f.write(json.dumps({"prompt": ex["prompt"], "completion": ex["completion"]}, ensure_ascii=False) + "\n")
        print(f"wrote {len(split):3d} examples → {out}")

    langs = {}
    for ex in examples:
        langs[ex["lang"]] = langs.get(ex["lang"], 0) + 1
    print(f"total {len(examples)} ({', '.join(f'{k}:{v}' for k, v in sorted(langs.items()))})")
    print("NOTE: this seed set is small — augment it (more real board compositions) before a real run.")


if __name__ == "__main__":
    main()
