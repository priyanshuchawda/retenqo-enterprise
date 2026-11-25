# Offline models

## Recommended 6 GB profile

Retenqo downloads the following model only after the user taps **Download 563 MB** in Settings:

- Model: Qwen3.5-0.8B, GGUF `Q4_0`
- File: `Qwen3.5-0.8B-Q4_0.gguf`
- Size: 563,036,064 bytes
- Source: [ggml-org/Qwen3.5-0.8B-GGUF](https://huggingface.co/ggml-org/Qwen3.5-0.8B-GGUF/blob/main/Qwen3.5-0.8B-Q4_0.gguf)
- SHA-256: `57d1997790d1744fba5b40a7317df71ea5e2acee28c47e78f0cce39c0703f8cf`
- Original model license: [Apache License 2.0](https://huggingface.co/Qwen/Qwen3.5-0.8B/blob/main/LICENSE)

The app verifies the pinned size, SHA-256, and GGUF format before replacing an installed model. The partial file is removed if the user cancels or a check fails. The model is stored in app-scoped storage. Network access is used only for an explicit model download; the app does not send prompts or memories to the model publisher.

Model download needs an internet connection and roughly 691 MB free space to preserve 128 MB of storage headroom. Wi-Fi is recommended. After download, local generation does not need a network connection.

## Device verification

The selected file is a small compatibility starting point, not a measured performance guarantee. Generation speed, peak memory, thermal behavior, and quality must be measured on each target phone. Earlier local generation was observed on a Galaxy M32 (Android 13, about 5.8 GB physical RAM). See [Testing](TESTING.md) for device scope.

## Speech and historical model research

The Whisper bridge expects a separate `ggml-base-q5_1.bin` model in app-scoped storage or bundled assets. Fresh-install availability and transcription need verification. Earlier 2B/4B and embedding experiments are indexed in [research](RESEARCH_INDEX.md); they are not the current recommended configuration.
