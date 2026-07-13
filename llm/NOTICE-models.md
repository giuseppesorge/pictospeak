# Model weights notice

**No model weights are distributed in this repository, in any APK, or in any release
artifact.** The `:llm` module ships only inference *code* (Apache-2.0 runtime).

Weights are imported by the user at runtime (Storage Access Framework, play flavor only).
At import time the app must:

1. show the license terms of the imported model and record acceptance;
2. display the file's SHA-256 and size;
3. store the file privately in `filesDir/models/`.

License obligations by candidate model family:

- **Gemma (Google)** — governed by the Gemma Terms of Use, which impose flow-down
  obligations on redistribution (copy of the terms + use restrictions must accompany the
  model). Because of this, Gemma weights must never be committed, bundled, or rehosted by
  this project.
- **Qwen (Alibaba)** — Apache-2.0-family licensing; simpler obligations, preferred if
  bundling is ever reconsidered.

Any change to model handling must update this notice and `THIRD_PARTY_NOTICES.md`.
