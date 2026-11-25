# Mneme: RAG / embeddings / model choice — research + tests

Date: 2026-08-23

## What “working well” means for Mneme

Mneme is **not** ChatGPT. Pipeline:

1. **Retrieve** relevant notes (keyword / hash embed / EmbeddingGemma)
2. **Generate** a short answer from those notes only (small LLM)
   — or skip LLM and **show the note** (extractive)

Quality = retrieval correctness × generation faithfulness × latency on 6 GB phone.

---

## Docs findings (web)

### Qwen3.5 “thinking”
- Hybrid thinking models often dump tokens into `<think>` and hit `max_tokens` with **empty visible answers**.
- llama.cpp: `--reasoning-budget 0` is the reliable kill switch; `--reasoning off` alone is flaky; `/no_think` is inconsistent; `--jinja` + this GGUF **crashed** our host binary.
- On Android JNI we don’t get those CLI flags → must **strip think**, give enough tokens, and prefer **non-thinking Instruct** checkpoints when possible.

Sources: [llama.cpp #20182](https://github.com/ggml-org/llama.cpp/issues/20182), [PR #13771](https://github.com/ggml-org/llama.cpp/pull/13771).

### EmbeddingGemma
- Google’s on-device embedder (~300M), multilingual, built for RAG.
- Needs **SentencePiece tokenizer + TFLite**; LiteRT / AI Edge RAG samples exist.
- We already have both files on phone; we only **probe** today — HashEmbedder is interim.

Sources: [EmbeddingGemma overview](https://ai.google.dev/gemma/docs/embeddinggemma), [litert-community/embeddinggemma-300m](https://huggingface.co/litert-community/embeddinggemma-300m), [AI Edge RAG Android](https://developers.google.com/edge/mediapipe/solutions/genai/rag/android).

### Other SLMs (2026 edge guides)
| Model | Fit for Mneme on 6 GB Redmi |
|-------|-----------------------------|
| **Qwen3.5-2B (current, thinking)** | Fits (~1 GB Q4) but thinking hurts short RAG |
| **Qwen2.5-1.5B Instruct** (we have `.task` fallback) | Smaller, no heavy think — good Pulse/Ask candidate |
| **Gemma 3 1B / Gemma 3n E2B** | Phone-first; strong IF; may need separate download |
| **Phi-4-mini ~3.8B** | Better reasoning but **tight/risky** on 6 GB with Android + Whisper |
| **Qwen3-4B** | Better quality; **too heavy** as daily default on mid-range |

Guides stress: under ~8 GB usable RAM prefer **≤2B** class for always-on assistants.

---

## Our tests (Fedora, same GGUF)

### Retrieval (no LLM)
HashEmbedder and keyword **both ranked correctly** on Uber / Priya / Netflix / medicine / Thursday.  
Hash embed falsely preferred Priya for “passport” (noise); keyword returned no strong hit — **combine**: embed + keyword + min score threshold.

### Generation (LLM)
| Setup | Result |
|-------|--------|
| Chapter-11 style short context | Often correct |
| 4 notes in context, “airport Uber ₹620?” | Sometimes **wrongly refuses** (“don’t have that”) |
| Top-1 Uber note only | Sometimes empty (think ate budget) |
| Top-1 Priya meeting | **Good** cited answer |
| Reasoning ON | Long think, **no answer** |
| Extractive (return matching note text) | **Always correct** when retrieval hits |

**Insight:** For personal memory, **retrieval + show/cite the note** is more reliable than trusting 2B generative paraphrase. LLM is best as a *thin* layer: short rewrite + citation, with extractive fallback.

---

## Best architecture for Mneme (recommended)

```text
Ask
  → retrieve top-3 (EmbeddingGemma when ready; Hash+keyword now)
  → if empty: fixed “not in memory”
  → else:
       try LLM short answer (/no_think, low temp, cite [n])
       if empty/refuse-but-hit: FALL BACK to “From your notes: …” + note bodies
```

Pulse stays: OCR → (optional) LLM JSON → save + embed.

---

## Should we switch models?

| Priority | Action | Why |
|----------|--------|-----|
| P0 | Keep Qwen 2B for now + **extractive fallback** | Already on device; fix reliability without new downloads |
| P0 | Wire **EmbeddingGemma + SentencePiece** | Real semantic RAG; files already on phone |
| P1 | A/B **Qwen2.5-1.5B Instruct** (LiteRT `.task` we have) for Ask | Less thinking drama; smaller RAM |
| P2 | On iQOO 12–16 GB: try **Gemma 3 1B/4B** or Qwen Instruct **non-thinking** 2–3B | Event phone headroom |
| Avoid now | Jump to 4B+ on Redmi 6 GB as default | OOM / thermal / kills |

---

## Bottom line

1. **RAG is the right design** — model should not “know” your life without notes.  
2. **Embeddings matter more than a bigger LLM** for “real questions” once memory grows.  
3. **Current Qwen3.5-2B thinking variant is OK but unreliable** for short answers; fix with budget/fallback, or swap to Instruct non-think / 1.5B.  
4. **Best “good enough” product path:** EmbeddingGemma retrieve + extractive fallback + thin LLM rewrite — not “smarter chat.”
