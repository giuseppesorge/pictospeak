# ADR-0004: LiteRT-LM, pinned and isolated to one class

**Status**: accepted

## Context
Google's on-device LLM stack has churned (the MediaPipe LLM Inference API went
maintenance-only within ~18 months; its successor LiteRT-LM is pre-1.0). Runtime churn
must never leak into the app.

## Decision
Runtime: com.google.ai.edge.litertlm (LiteRT-LM), version pinned in the catalog. Every
LiteRT-LM symbol is confined to LiteRtSentenceRefiner in :llm. The stable interface
(SentenceRefiner) is owned by :nlg. Swap procedure: replace one file (docs/handover.md).
Removal: delete :llm + one flavor RefinerFactory.

## Rejected
MediaPipe tasks-genai (maintenance-only), llama.cpp/MLC/ONNX-GenAI (JNI/per-device build
maintenance burden incompatible with boring architecture).
