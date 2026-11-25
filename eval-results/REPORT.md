# Mneme — Fedora model eval results (fixed)

**Date:** 2026-08-23  
**Host:** Fedora laptop (~16 GB RAM) · `llama-completion` from `tools/llama.cpp/build-host`  
**Author:** Priyanshu Chawda  
**Related:** [MODELS.md](../MODELS.md) · [GENERATION_FIX.md](./GENERATION_FIX.md) · [MODEL_RAG_RESEARCH.md](./MODEL_RAG_RESEARCH.md)

This doc is the **source of truth** for what we actually measured after fixing generation. Older blank/garbage runs are explained under [Failures (not model quality)](#failures-not-model-quality).

---

## Verdict — what works best

| Rank | Stack | Best for | Why |
|------|--------|----------|-----|
| **1 (ship now)** | **ML Kit + Qwen3.5-2B** + Whisper base + **Qwen3-Embedding-0.6B** (HashEmbed interim) | **Redmi 13 5G (6 GB)** | Fits RAM with care; Pulse JSON OK with no-think; Ask OK with closed-think / extractive fallback |
| **1b (best quality we proved)** | **ML Kit + Qwen3.5-4B** + Whisper + embeds | **Fedora A/B · iQOO 15 · 8 GB+ phones** | Same `qwen35` family as 2B; **best Ask answers** in this suite after CLI fix |
| Avoid as daily | Qwen3-4B-Instruct-2507 / Qwen2.5-3B on *this* host | — | Loaded but **garbage decode** — not a fair quality score |
| Defer | Qwen3-VL | Vision Pulse | Needs **mmproj + mtmd**; OCR→text LLM is enough for v1 |

**One-line product pick**

```text
Redmi (daily):     ML Kit → Qwen3.5-2B (no-think / closed <think>) → cite notes
iQOO / quality:    same pipeline, swap LLM → Qwen3.5-4B Q4_K_M
Always:            Whisper base · Qwen3-Embedding-0.6B (wire) · extractive Ask fallback
Never:             default thinking with tiny -n (looks “blank”)
```

---

## How generation must be run (or results look empty)

Qwen3.5 often fills `<think>…</think>` and burns the token budget. Short `-n` → strip think → **blank**.

**Working Fedora pattern** (see `scripts/eval-fedora-text-models.sh`):

1. `-no-cnv` (disable interactive chat wrapper)
2. Manual Qwen chat turns ending with a **closed empty think**:

```text
<|im_start|>assistant
<think>
</think>

```

3. Post-process: strip `<think>…</think>` and `[end of text]`

`--reasoning off` alone is **unreliable** on this binary without jinja (jinja crashed). `/no_think` alone is not enough for long Ask prompts.

---

## Models tested

| Model | Path | Arch | Size | Status |
|-------|------|------|------|--------|
| **Qwen3.5-2B** (phone) | `models/phone1/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf` | `qwen35` | ~1.07 GB | **Works** — ship on Redmi |
| **Qwen3.5-4B** | `models/eval/Qwen3.5-4B-Q4_K_M.gguf` (unsloth) | `qwen35` | 2,740,937,888 B | **Works** — best Ask in suite |
| Qwen3-4B-Instruct-2507 | deleted after eval | `qwen3` | exact HF size | **Garbage** on this build |
| Qwen2.5-3B-Instruct | deleted after eval | `qwen2` | exact bartowski size | **Garbage** on this build |
| Whisper base Q5 | `ggml-base-q5_1.bin` | — | ~60 MB | **Excellent** (JFK) |
| HashEmbedder | app | — | — | OK for tiny note DB |
| Tesseract (laptop OCR) | Fedora | — | — | **Poor** on UI screens — use **ML Kit on phone** |
| Qwen3-VL | not scored | VL + mmproj | — | Not run; different toolchain |

---

## Qwen3.5-4B — fixed suite (best Ask)

**Command:** `./scripts/eval-fedora-text-models.sh models/eval/Qwen3.5-4B-Q4_K_M.gguf`  
**Raw outs:** `eval-results/fedora-suite/text/Qwen3.5-4B-Q4_K_M/*.out`

Shared CONTEXT (4 notes): Uber ₹620 · Netflix ₹1,299 · Priya Thursday 4pm · doctor meds.

| Prompt | Output | Score |
|--------|--------|-------|
| How much was my airport Uber? | `Your airport Uber cost ₹620 [1].` | **Excellent** |
| When/where meeting with Priya? | `Your meeting with Priya is on Thursday at 4pm at a cafe near gate 2 [3].` | **Excellent** |
| What did Netflix charge? | `Netflix charged INR 1,299 [2].` | **Excellent** |
| Passport number? (missing) | `I don't have that in your memory.` | **Excellent** |
| What do I have on Thursday? | `You have a meeting at 4pm at a cafe near gate 2. [3]` | **Good** (cite OK) |
| Pulse JSON (noisy WA OCR) | See below | Structure OK; body inherits OCR junk |
| Pulse JSON (noisy Uber OCR) | See below | Structure OK; OCR junk (`P8620`, `UPL`) |

### Pulse — noisy WhatsApp OCR

Input OCR: `Mams301 Yesterday plssubmitch-11 by 25108, and viva nextweek monday You okmam`

```json
{
  "title": "Mams301 Submission and Viva",
  "body": "Submitted assignment mams301 yesterday. Deadline was 25108. Viva scheduled for next week Monday.",
  "due_date": "2026-08-23"
}
```

→ JSON valid; **date/body still wrong** when OCR is garbage (needs clean ML Kit text + “today is …” prompt).

### Pulse — noisy Uber OCR

```json
{
  "title": "Uber Trip Receipt Altportdrop",
  "body": "Total P8620. Paid via UPL. Driver: Ramesh.",
  "due_date": null
}
```

---

## Qwen3.5-2B — earlier Fedora / phone-aligned results

**File:** phone GGUF · older suite under `eval-results/fedora-suite/qwen2b_*.out` and `REPORT.md` (pre-fix).

| Prompt | Output | Score |
|--------|--------|-------|
| Airport Uber (multi-note context, old flags) | `I don't have that in your memory.` | **Weak** — over-refuse (retrieval/prompt issue, not missing note) |
| Priya meeting (same) | `I don't have that in your memory.` | **Weak** — same |
| Missing fact | `I don't have that in your memory.` | **Excellent** |
| Pulse clean OCR | `{"title":"CS301 Assignment & Viva","body":"Submit ch-11 by 25/08. Viva next week Monday.","due_date":"2026-08-25"}` | **Good** (year correct when today injected) |
| Pulse noisy OCR | title/body messy; due often wrong | **OK only with clean OCR** |
| Chapter 11 Ask (short context, no-think) | `Chapter 11 is due on August 25.` | **Good** |
| Hinglish Ask | `Chapter 11 homework is due August 25 [1].` | **Good** |
| Thinking ON, short `-n` | long “Thinking Process…”, no JSON/answer | **Fail for Mneme** |

**2B vs 4B (honest):** With the **fixed** prompt mode, **4B Ask is clearly better** (citations, fewer false refuses). **2B Pulse** on clean text is already good enough for Redmi RAM. Re-run 2B with the same `-no-cnv` + closed-think script before declaring Ask parity.

---

## Other components

| Component | Result | Keep? |
|-----------|--------|-------|
| Whisper base (JFK) | Classic line, ~3.5s laptop CPU | **Yes** |
| HashEmbedder | Ranks small corpora correctly | Interim until Qwen3-Embedding-0.6B |
| Extractive Ask (“show the note”) | Always correct when retrieve hits | **Yes — fallback** |
| Laptop Tesseract OCR | Mangled UI fixtures | **No** — phone ML Kit only |

---

## Failures (not model quality)

| Symptom | Cause | Fix |
|---------|--------|-----|
| Blank newlines / only whitespace | Think burned `-n`; strip left nothing | Closed empty `<think></think>` + `-no-cnv` |
| `77777…` / random chars / `is is is` | `qwen3` / `qwen2` GGUFs degenerate on this host binary | Use **Qwen3.5** (`qwen35`) family |
| `.aria2` next to GGUF | Leftover control file | Ignore if byte size = HF `Content-Length` |
| `--jinja` crash | Host `llama-completion` + jinja | Avoid; use manual template |
| VL “not tested” | Need mmproj + `llama-mtmd-cli` | Optional later |

Details: [GENERATION_FIX.md](./GENERATION_FIX.md)

---

## Recommended stacks (locked after this eval)

### Redmi 13 5G (6 GB) — ship

```text
Pulse:  ML Kit OCR → Qwen3.5-2B JSON extract (no-think / closed think)
Ask:    retrieve → Qwen3.5-2B short cite → else extractive note text
Voice:  Whisper base → same Ask path
Embed:  HashEmbed now → Qwen3-Embedding-0.6B when wired
```

### iQOO 15 / Fedora quality target

```text
Same pipeline, LLM = Qwen3.5-4B-Q4_K_M
(models/eval/Qwen3.5-4B-Q4_K_M.gguf proven on Fedora 2026-08-23)
```

### Do not prioritize yet

- Qwen3-VL / mmproj vision Pulse (OCR path is enough)
- Qwen2.5-3B / Qwen3-4B-Instruct on this llama.cpp host
- 7B+ as daily default on mid-range phones

---

## How to reproduce

```bash
cd retenqo
./scripts/eval-fedora-text-models.sh \
  models/phone1/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf \
  models/eval/Qwen3.5-4B-Q4_K_M.gguf
# → eval-results/fedora-suite/text/<model-tag>/*.out
```

Download 4B if missing:

```bash
hf download unsloth/Qwen3.5-4B-GGUF Qwen3.5-4B-Q4_K_M.gguf \
  --local-dir models/eval
```

---

## Bottom line

1. **Blank outputs were CLI/thinking issues (and wrong model family), not “4B broken.”**  
2. **Qwen3.5-4B with fixed prompts gives the best Ask answers we measured.**  
3. **Qwen3.5-2B remains the best daily model for 6 GB Redmi.**  
4. **OCR quality dominates Pulse** — use ML Kit; dirty OCR ⇒ bad JSON even with 4B.  
5. **Product reliability:** retrieve + cite + extractive fallback > bigger LLM alone.
