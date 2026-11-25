# Llama.android embedding overlay (tracked)

`tools/` is gitignored (full llama.cpp clone). These files are the Mneme-owned
Qwen3-Embedding JNI surface; `./scripts/build-mneme.sh` copies them into:

```
tools/llama.cpp/examples/llama.android/lib/src/main/cpp/embedding_jni.cpp
tools/llama.cpp/examples/llama.android/lib/src/main/java/com/arm/aichat/EmbeddingEngine.kt
```

and ensures `CMakeLists.txt` lists `embedding_jni.cpp`.
