#!/bin/bash
# Mneme Phone 1 smoke tests — run inside Ubuntu proot with /mneme bound
# Termux: proot-distro login ubuntu --bind $HOME/mneme-test:/mneme -- bash /mneme/run-test.sh
set -u
LOG="/mneme/test-log.txt"
exec > "$LOG" 2>&1
echo "=== Mneme Phone 1 tests $(date -u) ==="
echo "RAM: $(free -h | awk '/Mem:/ {print $2 " total, " $7 " avail"}')"

PASS=0; FAIL=0; WARN=0
pass(){ echo "PASS: $*"; PASS=$((PASS+1)); }
fail(){ echo "FAIL: $*"; FAIL=$((FAIL+1)); }
warn(){ echo "WARN: $*"; WARN=$((WARN+1)); }

build_whisper() {
  [[ -x /root/whisper.cpp/build/bin/whisper-cli ]] && return 0
  apt-get update -qq 2>/dev/null || true
  apt-get install -y -qq cmake build-essential git 2>/dev/null || true
  [[ ! -d /root/whisper.cpp ]] && git clone --depth 1 https://github.com/ggerganov/whisper.cpp /root/whisper.cpp
  cmake -B /root/whisper.cpp/build -DCMAKE_BUILD_TYPE=Release -DGGML_OPENMP=OFF /root/whisper.cpp
  cmake --build /root/whisper.cpp/build --target whisper-cli -j"$(nproc)"
}

build_llama() {
  [[ -x /root/llama.cpp/build/bin/llama-cli ]] && return 0
  [[ ! -d /root/llama.cpp ]] && git clone --depth 1 https://github.com/ggerganov/llama.cpp /root/llama.cpp
  cmake -B /root/llama.cpp/build \
    -DCMAKE_BUILD_TYPE=Release \
    -DLLAMA_CURL=OFF \
    -DGGML_OPENMP=OFF \
    -DLLAMA_BUILD_UI=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DBUILD_SHARED_LIBS=ON \
    /root/llama.cpp
  cmake --build /root/llama.cpp/build --target llama-cli -j"$(nproc)"
}

echo "--- TEST 1: Whisper base STT ---"
build_whisper
if [[ -f /mneme/whisper/ggml-base-q5_1.bin && -f /mneme/test-assets/jfk.wav ]]; then
  START=$(date +%s)
  OUT=$(/root/whisper.cpp/build/bin/whisper-cli -m /mneme/whisper/ggml-base-q5_1.bin -f /mneme/test-assets/jfk.wav -l en -nt 2>&1) || true
  T=$(($(date +%s)-START))
  echo "$OUT" | tail -12
  echo "Whisper time: ${T}s"
  if echo "$OUT" | grep -qiE 'fellow Americans|country can do|your country'; then
    pass "Whisper STT (${T}s)"
  else
    fail "Whisper output unexpected"
  fi
else
  fail "Whisper model or jfk.wav missing"
fi

echo "--- TEST 2: Qwen3.5-2B LLM ---"
build_llama
if [[ -f /mneme/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf ]]; then
  PROMPT='Extract JSON with title, body, source from: Chapter 11 due August 25 on Moodle.'
  START=$(date +%s)
  OUT=$(/root/llama.cpp/build/bin/llama-cli \
    -m /mneme/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf \
    -p "$PROMPT" -n 100 -c 2048 --temp 0.2 -no-cnv 2>&1) || true
  T=$(($(date +%s)-START))
  echo "$OUT" | tail -25
  echo "LLM time: ${T}s"
  if echo "$OUT" | grep -qiE 'chapter|august|moodle|title|body|json'; then
    pass "Qwen3.5-2B extract (${T}s)"
  elif echo "$OUT" | grep -qiE 'killed|OOM|error|failed to load'; then
    fail "Qwen3.5-2B OOM or load error"
  else
    warn "Qwen3.5-2B ran (${T}s) — review output"
  fi
  [[ $T -gt 120 ]] && warn "LLM slow — OK for 6GB phone, use in app with smaller context"
else
  fail "LLM gguf missing"
fi

echo "--- TEST 3: EmbeddingGemma TFLite ---"
if [[ -f /mneme/embed/embeddinggemma-300M_seq512_mixed-precision.tflite ]]; then
  python3 - <<'PY'
import sys
try:
    import tensorflow as tf
    i = tf.lite.Interpreter(model_path='/mneme/embed/embeddinggemma-300M_seq512_mixed-precision.tflite')
    i.allocate_tensors()
    o = i.get_output_details()[0]['shape']
    print('TFLite OK, output shape:', o)
    sys.exit(0)
except ImportError:
    print('NO_TF')
    sys.exit(2)
except Exception as e:
    print('ERR:', e)
    sys.exit(1)
PY
  RC=$?
  [[ $RC -eq 0 ]] && pass "EmbeddingGemma TFLite loads"
  [[ $RC -eq 2 ]] && warn "Run: pip install tensorflow (optional for embed smoke test)"
  [[ $RC -eq 1 ]] && fail "TFLite load failed"
else
  warn "TFLite not in /mneme/embed"
fi

echo "--- TEST 4: LiteRT fallback .task ---"
[[ -f /mneme/fallback/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task ]] && \
  pass "Fallback .task present (LiteRT-LM in Kotlin app)" || \
  warn "Fallback .task not copied to ~/mneme-test"

echo "=== SUMMARY PASS=$PASS FAIL=$FAIL WARN=$WARN ==="
