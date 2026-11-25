# Retenqo — Offline Personal Memory Assistant

**Author:** [Priyanshu Chawda](https://github.com/priyanshuchawda) (`priyanshuchawda`)  
**Product:** Private on-device memory for daily phone use  
**Language:** English (v1)  
**Devices:** Phone 1 Redmi 13 5G (daily) · Phone 2 Note 11 Pro+ · iQOO (upcoming)  

---

## One-line pitch

**Retenqo** captures important moments from your phone (notifications, WhatsApp media, on-demand screen pulses), turns them into **private notes**, and lets you **ask in text offline** — it answers from what it saved, cites the source, and reminds you about due dates. **Your context never leaves the device.**

---

## What we are NOT building

- ❌ Offline clone of ChatGPT / Gemini  
- ❌ 24/7 screen video recording  
- ❌ Full WhatsApp database scraping  
- ❌ ADB / Termux phone automation as the core demo  
- ❌ “Knows everything” before app install  

## What we ARE building

- ✅ **Private local memory** (notes + embeddings + RAG)  
- ✅ **Smart capture** from multiple sources  
- ✅ **Ephemeral screen understanding** (screenshot → AI → delete image → keep fact)  
- ✅ **Voice Q&A** with citations from saved notes  
- ✅ **Phone tools** (alarm, calendar, save note)  
- ✅ **NPU-native pipeline** on iQOO 15 (VLM/OCR + embed + LLM)  

---

## vs Google Assistant / Gemini

| | Google Assistant / Gemini | Retenqo |
|--|----------------------------|--------|
| Long-term memory | Cloud, Google-controlled | **Local SQLite + embeddings you own** |
| Screen understanding | Live, online session | **Pulse → extract → delete pixels** |
| Offline brain | Limited | **Full Q&A on iQOO NPU** |
| Cross-app personal search | Weak | **RAG over captured notes** |
| Privacy demo | Hard | **Airplane mode + visible timeline** |
| iQOO NPU story | N/A | **Multi-model on Hexagon NPU** |

**Unique wedge:** *Ephemeral sight, permanent knowledge* — process pixels, don’t hoard pixels.

---

## User experience (English)

### During the day
Phone quietly saves **short notes** from notifications, clipboard, and when user taps **Pulse** on important screens.

### Anytime
```
You:  "When is chapter 11 due?"
Retenqo: "On August 20, from a screen note: Chapter 11 due August 25."
       "Want a reminder the day before?"

You:  "Set alarm for 7 AM"
Retenqo: "Done."  → actually sets alarm
```

---

## Capture logic (exact triggers)

### Rule: Never “every 5 seconds” by default

Continuous 5s screenshots = battery drain, duplicates, privacy concerns, judge pushback.

### TYPE A — Manual Pulse (MVP #1, build first)

**When:** User taps floating **Pulse** button OR says *"Remember this"*

```
IF user taps Pulse OR voice intent == REMEMBER_SCREEN:
    IF pulse_in_progress → ignore (debounce 3 sec)
    ELSE:
        screenshot → RAM only
        AI extract facts → save note
        DELETE screenshot immediately
        toast: "Saved: Chapter 11 due Aug 25"
```

### TYPE B — Smart automatic (no timer)

| Trigger | Screenshot? | Logic |
|---------|-------------|--------|
| **B1 Notification** | No | WhatsApp/Gmail/SMS preview → importance filter → save text |
| **B2 Clipboard** | No | User copies text → filter → save |
| **B3 App-switch pulse** | Yes (optional) | Leaving allowed app → one pulse, debounce 2 min/app |
| **B4 Share intent** | No | User shares content → save full text |

**App-switch logic (optional, OFF by default):**
```
IF auto_pulse_on_app_switch enabled:
    IF foreground_app changed FROM allowed_app:
        wait 500ms
        IF not home/launcher/keyboard:
            ONE screenshot → AI → delete → save
        max 1 per app per 2 minutes
```

### TYPE C — Focus session (optional, later)

**When:** User enables “Focus mode” (study/work)

```
IF focus_mode ON:
    every 90 seconds (NOT 5):
        IF screen ON AND content hash changed:
            pulse → AI → delete → save
    foreground notification: "Retenqo Focus active"
    auto-stop after 2–4 hours
```

### Importance filter (before saving)

**SAVE if contains:**
- Dates: Aug 25, 11th, tomorrow, due  
- Keywords: deadline, submit, exam, chapter, meeting, homework, reminder  
- Phone, URL, email  
- User said "remember"

**SKIP if:**
- Empty or &lt; 10 chars  
- Only "ok", "thanks", "lol"  
- Duplicate text hash in last 10 minutes  

---

## Architecture

```
┌──────────────── CAPTURE ─────────────────┐
│ Pulse (manual) · Notifications           │
│ Clipboard · Share · Voice "remember"       │
│ App-switch pulse (optional)                │
└──────────────────┬─────────────────────────┘
                   ▼
         ┌─────────────────┐
         │ Importance filter │
         └────────┬─────────┘
                  ▼
         ┌─────────────────┐
         │ Notes + embeddings│  SQLite + sqlite-vec
         └────────┬─────────┘
                  ▼
    ┌─────────────┴─────────────┐
    ▼                             ▼
┌─────────┐                 ┌──────────┐
│ ASK      │                 │ DO        │
│ RAG+LLM  │                 │ alarm,    │
│ → TTS    │                 │ calendar  │
└─────────┘                 └──────────┘
```

### LLM prompt rule (anti-hallucination)

> Answer ONLY using the CONTEXT notes below. Cite source app and date. If not found, say "I don't have that in your memory."

---

## Phone tools (Google Assistant-like)

| Voice command | Tool | Android API |
|---------------|------|-------------|
| Set alarm for 7 AM | `set_alarm` | AlarmManager |
| Remind me Aug 24 at 9 | `create_calendar_event` | CalendarContract |
| Remember this: … | `save_note` | Local DB |
| What did I save today? | `list_timeline` | Query DB |
| When is chapter 11 due? | `query_memory` | RAG + LLM |

LLM outputs structured JSON → app executes tool → confirms by voice.

---

## NPU model stack (iQOO 15 event device)

| Job | Model | Notes |
|-----|--------|--------|
| Read screen (Pulse) | Gemma 3n E2B or OCR + Gemma 1B | Extract JSON facts |
| Index notes | Qwen3-Embedding-0.6B | RAG search |
| Answer + tool routing | Gemma 1B–3B INT4 | QNN / MediaPipe |
| Speech in | Whisper-small or Android STT | English |
| Speech out | Android TTS | English |

**Judge line:** *Four models on NPU — see, remember, think, act.*

### Realistic model sizes on iQOO 15

| RAM | Sweet spot | Max |
|-----|------------|-----|
| 12 GB | 1B–3B INT4 | 4B tight, 8B slow |
| 16 GB | 2B–4B INT4 | 7–8B possible |

---

## Dev phones

| | Redmi 13 5G (daily now) | iQOO (upcoming) |
|--|-------------------------|-----------------|
| ADB serial | `1431df87` | TBD |
| RAM | 6 GB | 12–16 GB (typical) |
| Build UI, Pulse, DB | ✅ | ✅ |
| Local LLM | Qwen3.5-2B daily | Larger Qwen / NPU later |
| Embeddings RAG | Qwen3-Embedding-0.6B | Same + headroom |
| Airplane / offline | ✅ | ✅ |

**Strategy:** One codebase, pluggable `AiBackend` — prototype on Redmi, NPU on iQOO.

---

## MVP scope (MVP scope)

### Must ship
- [ ] Timeline UI + SQLite notes  
- [ ] Manual **Pulse** → understand → delete SS → save note  
- [ ] Notification capture (text only)  
- [ ] Voice ask → answer with source citation  
- [ ] `set_alarm` tool  
- [ ] Airplane mode demo (on iQOO)  

### Should ship
- [ ] Clipboard capture  
- [ ] Embedding RAG on NPU  
- [ ] `create_calendar_event`  
- [ ] Share intent  

### Skip for v1
- [ ] 5-second screenshot loop  
- [ ] Full Accessibility 24/7 scrape  
- [ ] ADB automation  
- [ ] Hinglish  
- [ ] Train LLM from scratch  

### Optional SL training
- [ ] Intent classifier: English phrase → `QUERY` / `SET_ALARM` / `REMEMBER_SCREEN` (200 examples)

---

## Demo script (3 minutes)

1. Open browser: *"Chapter 11 — submit by August 25"*  
2. Tap **Pulse** → timeline shows note (no screenshot in gallery)  
3. WhatsApp notification arrives → auto-saved note  
4. **Airplane mode ON**  
5. *"When is chapter 11 due?"* → spoken answer + source  
6. *"Remind me the day before at 9 AM"* → calendar/alarm  
7. Show NPU pipeline slide  

---

## User settings (defaults)

```
[x] Save from notifications
[x] Save from clipboard
[ ] Auto-pulse when leaving apps     ← OFF default
[ ] Focus mode (90 sec pulse)        ← OFF default

Allowed apps: WhatsApp, Chrome, Gmail, Messages, …
```

---

## Inspiration from past iQOO builds (not copies)

| Project | Takeaway |
|---------|----------|
| Pocket Agent | Tool execution on Android (MCP-style) |
| Drishti | Offline multimodal, cite sources, Compose UI |
| DP on DAG | 100% on-device, no cloud |
| CLUTCH | Strong demo moment (we use Pulse + airplane mode) |

**Our combo:** Memory (RAG) + ephemeral screen pulse + tools + offline NPU.

---

## Project structure (planned)

```
mneme/
├── app/                    # Android Kotlin + Compose
│   ├── capture/            # Pulse, NotificationListener, Clipboard
│   ├── memory/             # Room DB, embeddings, RAG
│   ├── ai/                 # AiBackend interface, NPU/cloud impl
│   ├── tools/              # Alarm, Calendar, SaveNote
│   └── voice/              # STT, TTS, intent routing
├── docs/
│   └── idea.md             # this file
└── scripts/
    └── push-to-github.sh
```

---

## Open questions (to decide on device)

- [ ] Final name: **Retenqo** vs **Recall** vs **Pulse**  
- [ ] MediaProjection vs Accessibility for text capture on Redmi 13  
- [ ] Gemma 3n vs OCR+LLM for Pulse extract on 6 GB RAM  
- [ ] Cloud LLM fallback during Redmi dev phase  
- [ ] Which notification apps to allow in MVP  

---

## Credits & license

**Author:** Priyanshu Chawda (`priyanshuchawda`)  
**Email:** hellopriyanshu4@gmail.com  

This document and associated code are authored for the Retenqo project.  
Implementation on physical devices is in progress — logic and scope may evolve.

---

## References

- [MediaPipe LLM Inference (Android)](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android)
- [llama.cpp](https://github.com/ggerganov/llama.cpp)
- [Qwen3.5](https://huggingface.co/Qwen)

---

*Last updated: 2026-08-22*
