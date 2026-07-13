# Third-party notices

PictoSpeak deliberately separates the licensing of code, symbol assets, linguistic data,
and (optional, never-bundled) model weights. This file is the single source of truth.
When adding any dependency or asset, update this matrix.

| Component | What | License | Where |
|---|---|---|---|
| PictoSpeak code | All Kotlin/Gradle sources in this repo | Apache-2.0 | `LICENSE` |
| ARASAAC pictograms + metadata | Curated pictogram bundle and keyword catalog (added at M2) | CC BY-NC-SA (author: Sergio Palao; owner: Government of Aragon; origin: [arasaac.org](https://arasaac.org)) | `app/src/main/assets/arasaac/LICENSE` + `ATTRIBUTION.md` (with the bundle) |
| Morph-it! derived lexicon | Curated Italian morphological lexicon (added at M2) | LGPL (chosen from Morph-it!'s dual CC BY-SA 2.0 / LGPL); attribution: Baroni & Zanchetta, Morph-it! v0.48 | `app/src/main/assets/lexicon/LICENSE` + `PROVENANCE.md` (with the data) |
| Model weights (optional LLM) | **Never in this repository or in any APK.** User-imported at runtime (play flavor only) | Gemma: Google Terms of Use with flow-down obligations · Qwen: Apache-2.0 | `llm/NOTICE-models.md` |
| AndroidX / Jetpack / Kotlin / kotlinx / Coil | Build and runtime dependencies (see `gradle/libs.versions.toml`) | Apache-2.0 | Maven Central / Google Maven |
| JUnit 4 | Test-only dependency | EPL-1.0 | Maven Central |
| LiteRT-LM runtime | `com.google.ai.edge.litertlm` (`:llm` module, play flavor only) | Apache-2.0 (runtime only; weights licensed separately, see above) | Google Maven |

## Attribution requirements in the app

The ARASAAC attribution must always be visible in:
1. the **About** screen,
2. the **first-run** setup flow,
3. this repository's `README.md`,
4. any store listing text.

Official wording (Italian): *"I simboli pittografici utilizzati sono di proprietà del
Governo di Aragona e sono stati creati da Sergio Palao per ARASAAC (https://arasaac.org),
che li distribuisce sotto Licenza Creative Commons BY-NC-SA."*

Official wording (English): *"The pictographic symbols used are the property of the
Government of Aragon and have been created by Sergio Palao for ARASAAC
(https://arasaac.org), which distributes them under Creative Commons License BY-NC-SA."*

`scripts/check-licenses.sh` (run in CI) fails the build if these surfaces go missing.
