# Best Pulse storage format (Fedora eval 2026-08-25)

> Historical development record. Current implementation and release evidence: [engineering status](../../docs/IMPLEMENTATION_STATUS.md), [verification](../../docs/TESTING.md), [research index](../../docs/RESEARCH_INDEX.md).

Screenshot OCR → LLM memory card → Room note. Goal: Ask later (“when is the meet?”) without OCR junk.

## Schemas tested (WhatsApp Scrape-Verse OCR)

| Schema | What it stores | 2B quality | 4B quality |
|--------|----------------|------------|------------|
| **A** (old) | title, body, due_date | Body = OCR dump ❌ | Better but still thin |
| **B** (rich card) | kind, people, chat_with, body, when_text, due_date, tags, important, ask_hints | Noisy / invents times | **Best** — clean body, `when_text: 9:30`, `due_date: null` ✅ |
| **C** (facts + embed_text) | facts[], entities, embed_text | Good facts; **wrong** due_date from “today” | (running / optional) |

## Winning shape (now in NoteExtractor)

LLM returns rich JSON; app **flattens** into existing columns:

```
title:   Wemade Devs video model discussion
body:    [meeting] Aditya, Sneha, Vaishnavi
         The team discussed showing future possibilities in a video.
         Sneha suggested a quick meeting at 9:30…
         When: 9:30
         People: Aditya, Sneha, Vaishnavi
         Tags: Wemade, video, model, meeting
dueAt:   null   (time-only — do not invent a calendar day)
embed:   embed_text paragraph (for HashEmbed now / Qwen3-Embedding later)
```

## Rules that matter

1. **Clean English in body** — never raw OCR chrome (`13:49`, `Message`, checkmarks).
2. **`when_text` vs `due_date`** — clock times stay in text; `due_date` only for real calendar dates.
3. **`embed_text`** — separate searchable paragraph (names + facts + time).
4. **`important`** — skip pure casual chat after extract.
5. Prefer **Qwen3.5-4B** for extract quality when RAM allows; **2B** OK with the richer prompt on Redmi.

## Files

- App: `android/mneme/.../NoteExtractor.kt`, `PulsePipeline.kt`, `NoteRepository.kt`
- Generated outputs: `eval-results/pulse-store/*_schema{A,B,C}.out` (historical outputs remain in repository history).
