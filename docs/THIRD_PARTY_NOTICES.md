# Third-party notices

## Native inference sources

The Android build script checks out llama.cpp from `ggml-org/llama.cpp` at
revision `51831d47d0a47b8a7d07c9ff262ebe4b0a211790`. The upstream repository
is distributed under the MIT License; its license text is available in the
checkout at `tools/llama.cpp/LICENSE`.

The Android build script checks out whisper.cpp from `ggerganov/whisper.cpp` at
revision `96ec4d9823e9d0211944172da35f9f29134af990`. The upstream repository
is distributed under the MIT License; its license text is available in the
checkout at `tools/whisper.cpp/LICENSE`.

These source trees are provisioned by `scripts/bootstrap-native-deps.sh` and
are not committed to this repository. Retenqo adds a small Android runtime API
patch and privacy-oriented log changes through `scripts/patch-llama-runtime.py`.

## User-imported models

GGUF model weights are not bundled with Retenqo. Model licenses and usage
conditions differ by publisher and model, so users must review the license for
the exact model they import. Retenqo currently records the imported file's
SHA-256 digest for local identification; it does not establish or certify the
model's license or provenance.

The in-app recommended download is `Qwen3.5-0.8B-Q4_0.gguf` from the
`ggml-org/Qwen3.5-0.8B-GGUF` Hugging Face repository (563,036,064 bytes,
SHA-256 `57d1997790d1744fba5b40a7317df71ea5e2acee28c47e78f0cce39c0703f8cf`).
The underlying Qwen3.5-0.8B model is published under Apache License 2.0.
See [OFFLINE_MODELS.md](OFFLINE_MODELS.md) for source and installation notes.

## Other libraries

The Android project also uses dependencies declared in Gradle build files.
Their license notices remain those supplied by their respective publishers;
review the resolved dependency metadata when preparing a distributable release.
