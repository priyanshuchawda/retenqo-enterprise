#!/usr/bin/env bash
# Fedora suite: compare text LLMs on Mneme RAG + Pulse-extract prompts.
#
# Why earlier 3B/4B looked "blank":
# - Qwen2.5-3B / Qwen3-4B-Instruct produced garbage tokens on this llama.cpp build
#   (not incomplete downloads — sizes matched HF).
# - Qwen3.5 chat mode often burns max_tokens inside <think>; short -n → empty answer.
# Fix for Qwen3.5: -no-cnv + manual chat turns ending with closed empty <think></think>
# so the model continues with the visible answer (see Unsloth/Qwen3.5 thinking docs).
#
# Usage: ./scripts/eval-fedora-text-models.sh [model.gguf ...]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/eval-results/fedora-suite"
LLAMA="$ROOT/tools/llama.cpp/build-host/bin/llama-completion"
export LD_LIBRARY_PATH="$ROOT/tools/llama.cpp/build-host/bin:${LD_LIBRARY_PATH:-}"
mkdir -p "$OUT/text"

CONTEXT='[1] Uber receipt
Airport drop ₹620 paid via UPI to driver Ramesh

[2] Bank SMS
HDFC A/c XX4521 debited INR 1,299 for Netflix on 21-Aug-2026

[3] WhatsApp Priya
Meeting shifted to Thursday 4pm at cafe near gate 2. Bring laptop.

[4] Doctor slip
Dr Mehta: follow-up after 10 days. Medicine: pantoprazole 40mg morning.'

OCR_WHATSAPP='Mams301 Yesterday plssubmitch-11 by 25108, and viva nextweek monday You okmam'
OCR_UBER='Uber Trip Receipt Altportdrop Total P8620 Paid via UPL Driver: Ramesh'

# Build a Qwen chat prompt that skips thinking (empty closed think block).
qwen_prompt() {
  local sys="$1" user="$2"
  cat <<EOF
<|im_start|>system
${sys}<|im_end|>
<|im_start|>user
${user}<|im_end|>
<|im_start|>assistant
<think>
</think>

EOF
}

run_completion() {
  local model="$1" prompt_file="$2" n="$3" raw="$4" err="$5"
  "$LLAMA" -m "$model" -f "$prompt_file" -n "$n" --temp 0.1 -t 6 -c 2048 \
    -no-cnv --no-display-prompt \
    2>"$err" >"$raw"
}

clean_out() {
  local raw="$1" out="$2"
  python3 - <<PY
import re
raw=open("$raw").read()
clean=re.sub(r"(?s)<think>.*?</think>","",raw)
clean=re.sub(r"(?s)<think>.*","",clean)
clean=re.sub(r"\[end of text\]","",clean)
clean=re.sub(r"> EOF by user","",clean).strip()
open("$out","w").write(clean)
print(clean if clean else "(EMPTY)")
PY
}

run_model() {
  local model="$1"
  local tag
  tag="$(basename "$model" .gguf | tr ' /' '__' | cut -c1-48)"
  local dir="$OUT/text/$tag"
  mkdir -p "$dir"
  echo "======== MODEL $tag ========"

  ask() {
    local name="$1" q="$2"
    local user
    user=$(cat <<EOF
Answer ONLY using CONTEXT. Cite like [1]. One short sentence. If missing say: I don't have that in your memory.
Question: $q

CONTEXT:
$CONTEXT
EOF
)
    qwen_prompt "Mneme memory assistant. Short answers from notes only." "$user" \
      >"$dir/${name}.prompt"
    run_completion "$model" "$dir/${name}.prompt" 96 \
      "$dir/${name}.raw" "$dir/${name}.err"
    echo -n "  $name → "
    clean_out "$dir/${name}.raw" "$dir/${name}.out"
  }

  extract() {
    local name="$1" ocr="$2"
    local user
    user=$(cat <<EOF
Extract a personal memory note as JSON only (no markdown).
Keys: title (short), body (cleaned facts), due_date (YYYY-MM-DD or null).
Today is 2026-08-23. If date has no year use 2026.
Text:
$ocr
EOF
)
    qwen_prompt "Mneme. JSON only." "$user" >"$dir/${name}.prompt"
    run_completion "$model" "$dir/${name}.prompt" 128 \
      "$dir/${name}.raw" "$dir/${name}.err"
    echo -n "  $name → "
    clean_out "$dir/${name}.raw" "$dir/${name}.out" | head -c 220
    echo
  }

  ask uber "How much was my airport Uber?"
  ask priya "When and where is my meeting with Priya?"
  ask netflix "What did Netflix charge?"
  ask missing "What is my passport number?"
  ask thursday "What do I have on Thursday?"
  extract pulse_wa "$OCR_WHATSAPP"
  extract pulse_uber "$OCR_UBER"
}

if [[ $# -gt 0 ]]; then
  for m in "$@"; do run_model "$m"; done
else
  run_model "$ROOT/models/phone1/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf"
  if [[ -f "$ROOT/models/eval/Qwen3.5-4B-Q4_K_M.gguf" ]]; then
    run_model "$ROOT/models/eval/Qwen3.5-4B-Q4_K_M.gguf"
  else
    echo "Qwen3.5-4B not at models/eval/Qwen3.5-4B-Q4_K_M.gguf — skipping"
  fi
fi
echo "DONE → $OUT/text"
