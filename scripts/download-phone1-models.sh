#!/usr/bin/env bash
# Download Mneme P0 models for Phone 1 (Redmi 13 5G, 6 GB).
# Author: Priyanshu Chawda
#
# Usage: ./scripts/download-phone1-models.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODELS="${ROOT}/models/phone1"

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }

require() {
  command -v hf >/dev/null 2>&1 || command -v huggingface-cli >/dev/null 2>&1 || {
    echo "Install: pip install huggingface_hub" >&2
    exit 1
  }
}

HF="${HF_CMD:-hf}"
if ! command -v hf >/dev/null 2>&1; then
  HF="huggingface-cli"
fi

download() {
  local repo="$1"
  local dest="$2"
  shift 2
  log "↓ ${repo} → ${dest}"
  mkdir -p "${dest}"
  "${HF}" download "${repo}" "$@" --local-dir "${dest}"
}

main() {
  require
  cd "${ROOT}"

  log "Target: ${MODELS}"
  log "Phone 1 P0 — Qwen3.5-2B + Qwen3-Embedding-0.6B + Whisper"

  # Main LLM — llama.cpp JNI on phone
  download TheStageAI/Qwen3.5-2B-GGUF "${MODELS}/llm" \
    Qwen3.5-2B-M-TS-Q4_K_M.gguf

  # Voice STT — whisper.cpp
  download ggerganov/whisper.cpp "${MODELS}/whisper" \
    ggml-base-q5_1.bin

  # RAG embeddings — default for Phone 1 + 2
  download Qwen/Qwen3-Embedding-0.6B-GGUF "${MODELS}/embed/qwen3" \
    Qwen3-Embedding-0.6B-Q8_0.gguf

  # Optional fallback embedder (smaller RAM) — skip if disk tight
  if [[ "${DOWNLOAD_EMBEDDINGGEMMA:-0}" == "1" ]]; then
    download litert-community/embeddinggemma-300m "${MODELS}/embed/litert" \
      embeddinggemma-300M_seq512_mixed-precision.tflite \
      sentencepiece.model
  else
    log "Skip EmbeddingGemma (set DOWNLOAD_EMBEDDINGGEMMA=1 to fetch fallback)"
  fi

  log "Done. Layout:"
  find "${MODELS}" -type f -printf '  %s bytes  %p\n' | sort -k3

  du -sh "${MODELS}"/* 2>/dev/null || true
  log "Push to phone: adb -s 1431df87 push ${MODELS}/ /sdcard/Mneme/models/phone1/"
}

main "$@"
