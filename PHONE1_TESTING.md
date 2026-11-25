# Retenqo — Phone 1 testing & on-device AI guide

**Device:** Redmi 13 5G · ADB `1431df87` · 6 GB RAM · Snapdragon 4 Gen 2  
**Author:** Priyanshu Chawda  
**Last updated:** 2026-08-22

Related: [MODELS.md](./MODELS.md) · [DEV_DEVICE_REDMi13.md](./DEV_DEVICE_REDMi13.md)

---

## Current test status (live on phone)

| Component | Status | Notes |
|-----------|--------|-------|
| **Whisper STT** | ✅ **WORKING** | JFK sample transcribed correctly in **~6.4s** (11s audio) |
| **Qwen3.5-2B LLM** | 🔄 **Building** | llama.cpp compiling in Ubuntu (first run ~15–25 min) |
| **Qwen3-Embedding-0.6B** | ⏳ **Download next** | Default RAG embed (GGUF Q8); HashEmbed interim in app |
| **LiteRT fallback .task** | ⏳ **File only** | On `/sdcard/Retenqo`; runtime needs LiteRT-LM in app |
| **Models on device** | ✅ | `~/mneme-test/` — 1 GB LLM + 57 MB Whisper + 171 MB embed |

---

## Whisper — what we use (NOT faster-whisper)

### Runtime: **whisper.cpp** (normal C++ Whisper)

| | **whisper.cpp** ✅ our choice | **faster-whisper** ❌ not for phone |
|--|-------------------------------|-------------------------------------|
| Language | C/C++ — runs natively on Android | Python + CTranslate2 |
| Best on | **CPU / ARM / mobile** | NVIDIA GPU servers |
| Retenqo integration | **whisper.cpp JNI** (official Android example) | Needs Python env — bad for APK |
| Quant format | GGML `.bin` (q5_1, q8_0, etc.) | int8 / float16 via pip |

Sources: [whisper.cpp vs faster-whisper](https://getinvoke.dev/learn/whisper-cpp-vs-faster-whisper/), [whisper.cpp GitHub](https://github.com/ggerganov/whisper.cpp), [HF ggml models](https://huggingface.co/ggerganov/whisper.cpp).

### Model file we downloaded

| Field | Value |
|-------|--------|
| **File** | `ggml-base-q5_1.bin` |
| **Repo** | [ggerganov/whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp) |
| **Size on disk** | ~57 MB |
| **RAM at runtime** | ~400 MB (whisper.cpp docs) |
| **Quant** | **Q5_1** (5-bit — smaller + fast, small quality loss vs float) |
| **Variant** | **base** multilingual (not `base.en` — we need English v1 but base handles accents) |
| **Phase** | **P0 locked** for Phone 1 and Phone 2 |

### Whisper options for your 6 GB phone

| Model | Size | RAM | Speed (11s clip) | Quality | Retenqo recommendation |
|-------|------|-----|------------------|---------|----------------------|
| **tiny.en-q5_1** | 31 MB | ~273 MB | ~3–4s | OK for clear speech | Fallback if RAM tight |
| **base-q5_1** ✅ | 57 MB | ~388 MB | **~6–7s** ✅ tested | Good accents/noise | **P0 Phone 1 — keep this** |
| **base.en-q5_1** | 57 MB | ~388 MB | ~6s | Slightly better English-only | OK if English-only v1 |
| **small-q5_1** | 181 MB | ~852 MB | ~15–20s | Better | **Phone 2** or P1 upgrade |
| **small.en-q5_1** | 181 MB | ~852 MB | ~15s | Best EN balance | Phone 2 if voice quality matters |
| **distil-large-v3** | — | 1 GB+ | Too slow | Best | **Not for 6 GB phone** |

**Verdict:** `base-q5_1` + **whisper.cpp** is the **correct and best P0 choice** for Redmi 13 5G.  
Upgrade to **`small-q5_1`** on Phone 2 (8 GB) if voice Q&A accuracy needs improvement.  
Do **not** use faster-whisper on device — it's for GPU servers, not Android APKs.

---

## Full P0 stack on Phone 1

```
ML Kit (OCR) → Qwen3.5-2B Q4_K_M (llama.cpp) → Qwen3-Embedding-0.6B (GGUF)
             → Whisper base Q5 (whisper.cpp) → Android TTS
```

| # | Model | File | Runtime |
|---|--------|------|---------|
| 1 | Pulse OCR | ML Kit (bundled) | Android SDK |
| 2 | LLM | `Qwen3.5-2B-M-TS-Q4_K_M.gguf` (1 GB) | llama.cpp JNI |
| 3 | STT | `ggml-base-q5_1.bin` (57 MB) | whisper.cpp JNI |
| 4 | Embeddings | `Qwen3-Embedding-0.6B-Q8_0.gguf` | llama.cpp / ONNX |
| 5 | TTS | System Android TTS | — |

---

## How to test on phone (best workflow)

### One-time (Termux on phone)

1. **Settings → Apps → Termux → Files → Allow all files**
2. **Settings → Apps → Termux → Battery → Unrestricted**
3. In Termux: `bash /sdcard/Retenqo/scripts/termux-setup-mneme.sh`

### From laptop (SSH — recommended)

```bash
adb -s 1431df87 forward tcp:8022 tcp:8022
ssh -p 8022 u0_a349@127.0.0.1

# Whisper test (works now)
export PATH=$PREFIX/bin:$PATH
proot-distro login ubuntu --bind $HOME/mneme-test:/mneme -- \
  /root/whisper.cpp/build/bin/whisper-cli \
  -m /mneme/whisper/ggml-base-q5_1.bin \
  -f /mneme/test-assets/jfk.wav -l en -nt

# LLM test (after llama.cpp build finishes)
proot-distro login ubuntu --bind $HOME/mneme-test:/mneme -- \
  /root/llama.cpp/build/bin/llama-cli \
  -m /mneme/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf \
  -p "Extract JSON: Chapter 11 due August 25" -n 80 -c 2048 --temp 0.2 -no-cnv
```

Or use helper: `./scripts/laptop-mneme-phone.sh setup|test|log|ssh`

### Rules that prevent problems

1. **Never build/run from `/sdcard`** — use `~/mneme-test/` inside Termux
2. **Use whisper.cpp**, not faster-whisper, for the Android app
3. Build with `-DGGML_OPENMP=OFF` on Termux/Ubuntu
4. **One big model at a time** — unload Qwen before loading anything else heavy
5. Termux is **dev lab only** — production Retenqo app uses JNI directly

---

## Folder layout

```
/sdcard/Retenqo/                          ← adb push here
~/mneme-test/                           ← copy/run here (Termux internal)
├── llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf
├── whisper/ggml-base-q5_1.bin
├── embed/embeddinggemma-300M_seq512_mixed-precision.tflite
├── test-assets/jfk.wav
└── test-log.txt
```

---

## Production path (Retenqo Kotlin app)

| Component | Ship with |
|-----------|-----------|
| STT | [whisper.android](https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android) + `ggml-base-q5_1.bin` |
| LLM | llama.cpp JNI + Qwen3.5-2B GGUF |
| Embeddings | Qwen3-Embedding-0.6B GGUF |
| iQOO demo | Gemma 4 + LiteRT-LM |

---

## Quick checklist

- [x] Models downloaded on laptop
- [x] Models pushed to phone (`~/mneme-test`)
- [x] whisper.cpp built in Ubuntu on phone
- [x] Whisper **base-q5_1** transcribes correctly
- [ ] llama.cpp build complete → Qwen3.5-2B inference test
- [ ] Download + load Qwen3-Embedding-0.6B Q8 GGUF
- [ ] Wire embed into NoteRepository (replace HashEmbedder)
- [ ] Termux: Allow all files (for `/sdcard/Retenqo` sync)
- [ ] Start Kotlin Retenqo app scaffold
