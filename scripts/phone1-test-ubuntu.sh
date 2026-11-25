#!/bin/bash
# Run inside: ubuntu -- bash /sdcard/Mneme/scripts/phone1-test-ubuntu.sh
set -u
LOG="/sdcard/Mneme/test-results-ubuntu.txt"
exec > >(tee "$LOG") 2>&1

echo "=== Mneme Phone 1 tests (Ubuntu in Termux) ==="
date
echo "Host: $(uname -m) $(uname -r)"
echo "RAM: $(free -h | awk '/Mem:/ {print $2 " total, " $7 " avail"}')"
echo ""

PASS=0; FAIL=0; WARN=0
pass(){ echo "PASS: $*"; PASS=$((PASS+1)); }
fail(){ echo "FAIL: $*"; FAIL=$((FAIL+1)); }
warn(){ echo "WARN: $*"; WARN=$((WARN+1)); }

SD="/sdcard/Mneme"
LLM="${SD}/models/phone1/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf"
WHISPER_M="${SD}/models/phone1/whisper/ggml-base-q5_1.bin"
WAV="${SD}/test-assets/jfk.wav"
TFLITE="${SD}/models/phone1/embed/litert/embeddinggemma-300M_seq512_mixed-precision.tflite"
TASK="${SD}/models/phone1/fallback/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"

# --- check files ---
echo "--- Files on device ---"
for f in "$LLM" "$WHISPER_M" "$WAV" "$TFLITE" "$TASK"; do
  if [[ -f "$f" ]]; then echo "OK $(du -h "$f" | cut -f1) $f"; else echo "MISSING $f"; fi
done
echo ""

# --- install llama.cpp if needed ---
echo "--- Setup llama.cpp ---"
LLAMA_BIN=""
if command -v llama-cli >/dev/null 2>&1; then
  LLAMA_BIN=llama-cli
elif [[ -x /root/llama.cpp/build/bin/llama-cli ]]; then
  LLAMA_BIN=/root/llama.cpp/build/bin/llama-cli
else
  if [[ ! -d /root/llama.cpp ]]; then
    echo "Cloning llama.cpp..."
    git clone --depth 1 https://github.com/ggerganov/llama.cpp /root/llama.cpp
  fi
  cmake -B /root/llama.cpp/build -DCMAKE_BUILD_TYPE=Release -DLLAMA_CURL=OFF -DGGML_OPENMP=OFF /root/llama.cpp
  cmake --build /root/llama.cpp/build --target llama-cli -j4
  LLAMA_BIN=/root/llama.cpp/build/bin/llama-cli
fi
echo "llama-cli: $LLAMA_BIN"

# --- TEST 1: Qwen3.5-2B ---
echo ""
echo "--- TEST 1: Qwen3.5-2B (extract note) ---"
if [[ ! -f "$LLM" ]]; then
  fail "LLM missing"
elif [[ -x "$LLAMA_BIN" ]] || command -v "$LLAMA_BIN" >/dev/null; then
  PROMPT='Extract JSON with title, body, source from: "Chapter 11 due August 25 on Moodle."'
  START=$(date +%s)
  OUT=$("$LLAMA_BIN" -m "$LLM" -p "$PROMPT" -n 100 -c 2048 --temp 0.2 -no-cnv 2>&1) || true
  END=$(date +%s)
  echo "$OUT" | tail -25
  echo "Time: $((END-START))s"
  if echo "$OUT" | grep -qiE 'chapter|august|moodle|title|body'; then
    pass "Qwen3.5-2B extract ($((END-START))s)"
  elif echo "$OUT" | grep -qiE 'killed|OOM|error|failed'; then
    fail "Qwen3.5-2B crashed/OOM"
  else
    warn "Qwen3.5-2B ran but output unclear"
  fi
  [[ $((END-START)) -gt 90 ]] && warn "LLM slow — consider Q3_K_S or LiteRT fallback"
else
  fail "llama-cli not built"
fi

# --- TEST 2: Whisper ---
echo ""
echo "--- TEST 2: Whisper base STT ---"
WHISPER_BIN=""
if command -v whisper-cli >/dev/null 2>&1; then
  WHISPER_BIN=whisper-cli
elif [[ -x /root/whisper.cpp/build/bin/whisper-cli ]]; then
  WHISPER_BIN=/root/whisper.cpp/build/bin/whisper-cli
else
  if [[ ! -d /root/whisper.cpp ]]; then
    git clone --depth 1 https://github.com/ggerganov/whisper.cpp /root/whisper.cpp
  fi
  cmake -B /root/whisper.cpp/build -DCMAKE_BUILD_TYPE=Release -DGGML_OPENMP=OFF /root/whisper.cpp
  cmake --build /root/whisper.cpp/build --target whisper-cli -j4
  WHISPER_BIN=/root/whisper.cpp/build/bin/whisper-cli
fi

if [[ -f "$WHISPER_M" && -f "$WAV" && -n "$WHISPER_BIN" ]]; then
  START=$(date +%s)
  OUT=$("$WHISPER_BIN" -m "$WHISPER_M" -f "$WAV" -l en -nt 2>&1) || true
  END=$(date +%s)
  echo "$OUT" | tail -10
  if echo "$OUT" | grep -qiE 'nation|president|dedicated'; then
    pass "Whisper STT ($((END-START))s)"
  else
    fail "Whisper unexpected output"
  fi
else
  fail "Whisper missing files or binary"
fi

# --- TEST 3: EmbeddingGemma TFLite ---
echo ""
echo "--- TEST 3: EmbeddingGemma TFLite ---"
if [[ ! -f "$TFLITE" ]]; then
  fail "TFLite missing"
else
  python3 - <<PY "$TFLITE"
import sys
path = sys.argv[1]
try:
    import tensorflow as tf
    i = tf.lite.Interpreter(model_path=path)
    i.allocate_tensors()
    inp, out = i.get_input_details(), i.get_output_details()
    print("TFLite OK in:", inp[0]['shape'], "out:", out[0]['shape'])
    sys.exit(0)
except ImportError:
    print("INSTALL: pip install tensorflow")
    sys.exit(2)
except Exception as e:
    print("ERR:", e)
    sys.exit(1)
PY
  RC=$?
  [[ $RC -eq 0 ]] && pass "EmbeddingGemma TFLite loads"
  [[ $RC -eq 2 ]] && warn "pip install tensorflow in Ubuntu for embed test"
  [[ $RC -eq 1 ]] && fail "TFLite load failed"
fi

# --- TEST 4: LiteRT fallback ---
echo ""
echo "--- TEST 4: Qwen2.5-1.5B LiteRT fallback ---"
if [[ -f "$TASK" ]]; then
  pass "Fallback .task present ($(du -h "$TASK" | cut -f1)) — needs LiteRT-LM in Android app"
else
  fail "Fallback .task missing"
fi

echo ""
echo "=== SUMMARY: PASS=$PASS FAIL=$FAIL WARN=$WARN ==="
echo "Log: $LOG"
