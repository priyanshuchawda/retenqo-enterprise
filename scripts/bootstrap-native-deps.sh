#!/usr/bin/env bash
# Provision the native sources used by Whisper and llama.cpp (kept outside Git).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/tools/whisper.cpp"
REPO="https://github.com/ggml-org/whisper.cpp.git"
REV="96ec4d9823e9d0211944172da35f9f29134af990"
LLAMA_DEST="$ROOT/tools/llama.cpp"
LLAMA_REPO="https://github.com/ggml-org/llama.cpp.git"
LLAMA_REV="51831d47d0a47b8a7d07c9ff262ebe4b0a211790"

if [[ ! -f "$DEST/CMakeLists.txt" || ! -f "$DEST/src/whisper.cpp" || ! -d "$DEST/ggml" || "$(git -C "$DEST" rev-parse HEAD 2>/dev/null || true)" != "$REV" ]]; then
  if [[ -e "$DEST" && -n "$(find "$DEST" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
    echo "Found an incomplete Whisper checkout at $DEST; move it aside and retry." >&2
    exit 1
  fi
  mkdir -p "$ROOT/tools"
  git init "$DEST"
  git -C "$DEST" remote add origin "$REPO"
  git -C "$DEST" fetch --depth 1 origin "$REV"
  git -C "$DEST" checkout --detach FETCH_HEAD
fi

if [[ ! -f "$LLAMA_DEST/examples/llama.android/lib/src/main/cpp/ai_chat.cpp" || "$(git -C "$LLAMA_DEST" rev-parse HEAD 2>/dev/null || true)" != "$LLAMA_REV" ]]; then
  if [[ -e "$LLAMA_DEST" && -n "$(find "$LLAMA_DEST" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
    echo "Found an incomplete llama.cpp checkout at $LLAMA_DEST; move it aside and retry." >&2
    exit 1
  fi
  git init "$LLAMA_DEST"
  git -C "$LLAMA_DEST" remote add origin "$LLAMA_REPO"
  git -C "$LLAMA_DEST" fetch --depth 1 origin "$LLAMA_REV"
  git -C "$LLAMA_DEST" checkout --detach FETCH_HEAD
  git -C "$LLAMA_DEST" sparse-checkout set examples/llama.android cmake common ggml include src vendor
fi

# Keep the upstream Android wrapper within the phone's CPU/RAM budget and use
# the CMake/NDK versions installed by this project's build environment.
LLAMA_LIB="$LLAMA_DEST/examples/llama.android/lib"
if rg -q '29\.0\.13113456|29\.0\.14206865|3\.31\.6' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i 's/29\.0\.13113456/28.2.13676358/; s/29\.0\.14206865/28.2.13676358/; s/3\.31\.6/4.1.2/' "$LLAMA_LIB/build.gradle.kts"
fi
if rg -q 'jvmToolchain\(17\)' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i 's/jvmToolchain(17)/jvmToolchain(21)/' "$LLAMA_LIB/build.gradle.kts"
fi
if ! rg -q 'JvmTarget\.JVM_17' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i '/jvmToolchain(21)/a\        compilerOptions {\n            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)\n        }' \
    "$LLAMA_LIB/build.gradle.kts"
fi
if rg -q 'listOf\("arm64-v8a", "x86_64"\)' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i 's/listOf("arm64-v8a", "x86_64")/listOf("arm64-v8a")/' "$LLAMA_LIB/build.gradle.kts"
fi
if rg -q 'GGML_CPU_KLEIDIAI ON|GGML_OPENMP ON' "$LLAMA_LIB/src/main/cpp/CMakeLists.txt"; then
  sed -i 's/GGML_CPU_KLEIDIAI ON/GGML_CPU_KLEIDIAI OFF/; s/GGML_OPENMP ON/GGML_OPENMP OFF/' \
    "$LLAMA_LIB/src/main/cpp/CMakeLists.txt"
fi
if rg -q 'GGML_OPENMP=ON' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i 's/GGML_OPENMP=ON/GGML_OPENMP=OFF/' "$LLAMA_LIB/build.gradle.kts"
fi
# Android packages native libraries inside the APK, so they are not reliably
# enumerable as files in ApplicationInfo.nativeLibraryDir. Link one CPU backend
# as a normal ggml dependency and register it at build time instead of using
# ggml's dlopen plugin discovery. This avoids scanning for backend plugin files.
if rg -q 'GGML_BACKEND_DL=ON|GGML_CPU_ALL_VARIANTS=ON' "$LLAMA_LIB/build.gradle.kts"; then
  sed -i '/GGML_BACKEND_DL=ON/d; /GGML_CPU_ALL_VARIANTS=ON/d' "$LLAMA_LIB/build.gradle.kts"
fi
if rg -q 'DEFAULT_CONTEXT_SIZE    = 8192|BATCH_SIZE              = 512' "$LLAMA_LIB/src/main/cpp/ai_chat.cpp"; then
  sed -i 's/DEFAULT_CONTEXT_SIZE    = 8192/DEFAULT_CONTEXT_SIZE    = 2048/; s/BATCH_SIZE              = 512/BATCH_SIZE              = 128/' \
    "$LLAMA_LIB/src/main/cpp/ai_chat.cpp"
fi
if rg -q 'Formatted and added %s message:' "$LLAMA_LIB/src/main/cpp/ai_chat.cpp"; then
  sed -i 's/    LOGi("%s: Formatted and added %s message:.*/    LOGi("%s: Formatted and added %s message.", __func__, role.c_str());/' \
    "$LLAMA_LIB/src/main/cpp/ai_chat.cpp"
fi
python3 "$ROOT/scripts/patch-llama-runtime.py" "$LLAMA_LIB"
cp "$ROOT/android/mneme/llama-embed-overlay/cpp/embedding_jni.cpp" "$LLAMA_LIB/src/main/cpp/embedding_jni.cpp"
cp "$ROOT/android/mneme/llama-embed-overlay/java/com/arm/aichat/EmbeddingEngine.kt" "$LLAMA_LIB/src/main/java/com/arm/aichat/EmbeddingEngine.kt"
cp "$ROOT/android/mneme/llama-embed-overlay/cpp/CMakeLists.txt.patched" "$LLAMA_LIB/src/main/cpp/CMakeLists.txt"
