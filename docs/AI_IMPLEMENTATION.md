# AI implementation

Retenqo combines deterministic extraction with optional local generation to support evidence-aware recall and commitment follow-through.

| Stage | Implementation |
| --- | --- |
| Extraction | [RulesExtract.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/RulesExtract.kt), [NoteExtractor.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/NoteExtractor.kt): rules-first dates, amounts, parties and optional model assistance |
| Retrieval | [NoteRepository.kt](../android/mneme/app/src/main/java/com/mneme/app/data/NoteRepository.kt): lexical scoring of a bounded candidate set |
| Generation | [LlmBridge.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/LlmBridge.kt): CPU llama.cpp GGUF with bounded generation |
| Answer validation | [AskAnswerGuard.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/AskAnswerGuard.kt), [MemoryAsk.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/MemoryAsk.kt): citation identifier checks and refusal handling |
| Completion evidence | [CompletionEvidenceAgent.kt](../android/mneme/app/src/main/java/com/mneme/app/ai/CompletionEvidenceAgent.kt), [FollowThroughMatcher.kt](../android/mneme/app/src/main/java/com/mneme/app/data/FollowThroughMatcher.kt): local suggestions and human review |
| Speech | [WhisperBridge.kt](../android/mneme/app/src/main/java/com/mneme/app/voice/WhisperBridge.kt), [TtsSpeaker.kt](../android/mneme/app/src/main/java/com/mneme/app/voice/TtsSpeaker.kt) |

The current recommended model is Qwen3.5-0.8B Q4_0 (563,036,064 bytes). The recommended download is checked for size, SHA-256 and GGUF format. See [offline models](OFFLINE_MODELS.md). There is no cloud generation fallback.

## Research rationale

The August 2026 [model evaluation](../eval-results/REPORT.md) explored earlier 2B/4B models, thinking-token consumption, OCR noise and refusal behavior. [Embedding experiments](../eval-results/embed-qwen3-0.6b/rag_vs_hash.txt) investigated lexical/hash and learned ranking. These exploratory results informed model and retrieval choices but are not current-model accuracy metrics.

## Evidence and review policy

The citation guard validates referenced identifiers, while the original memory context and explicit user approval support review of generated answers and completion suggestions. Identifier validation is distinct from factual entailment. The repository selects recent candidates before ranking (20× the requested limit, capped at 1,000); older records outside that set may not be retrieved by Ask. Full-history search, screen-only answer handling and broader adversarial/accuracy evaluation are [roadmap](ROADMAP.md) priorities. Learned embeddings remain research, not a current production dependency.
