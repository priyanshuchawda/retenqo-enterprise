# Retenqo model guide

The production recommendation is **Qwen3.5-0.8B Q4_0 GGUF**, downloaded separately and verified for size, SHA-256 and format. [Offline models](docs/OFFLINE_MODELS.md) records provenance, configuration and setup. [AI implementation](docs/AI_IMPLEMENTATION.md) explains the local inference and retrieval architecture.

## Research

Earlier 2B/4B model, OCR, speech and embedding experiments are summarized in [model evaluation](eval-results/REPORT.md), [retrieval research](eval-results/MODEL_RAG_RESEARCH.md) and the [research index](docs/RESEARCH_INDEX.md). They are historical, not claims about the recommended production configuration.

The shipped app uses CPU GGUF inference; larger model profiles and learned embeddings are experimental. Model setup is explicit and personal prompts are processed locally.
