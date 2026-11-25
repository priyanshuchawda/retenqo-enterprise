package com.mneme.app.ai

import android.content.Context
import java.io.File

/** Resolves models only from Retenqo's app-scoped storage or bundled assets. */
object ModelPaths {
    fun llmGguf(context: Context): File = File(modelsDirectory(context), "llm/active.gguf")

    fun ensureLlmAccessible(context: Context): File = llmGguf(context).also { file ->
        require(file.isFile && file.canRead() && file.length() > MIN_LLM_BYTES) {
            "Import a supported GGUF model in Settings before starting local generation."
        }
    }

    fun whisperBin(context: Context): File = modelFile(context, "whisper/ggml-base-q5_1.bin")

    fun embedGguf(context: Context): File = modelFile(context, "embed/qwen3/Qwen3-Embedding-0.6B-Q8_0.gguf")

    fun ensureWhisperAccessible(context: Context): File = ensureAccessible(
        context = context,
        relativePath = "whisper/ggml-base-q5_1.bin",
        minBytes = MIN_AUX_MODEL_BYTES,
        assetName = "ggml-base-q5_1.bin",
    )

    fun ensureEmbedAccessible(context: Context): File = ensureAccessible(
        context = context,
        relativePath = "embed/qwen3/Qwen3-Embedding-0.6B-Q8_0.gguf",
        minBytes = MIN_AUX_MODEL_BYTES,
    )

    private fun ensureAccessible(
        context: Context,
        relativePath: String,
        minBytes: Long,
        assetName: String? = null,
    ): File {
        val destination = modelFile(context, relativePath)
        if (destination.isFile && destination.canRead() && destination.length() > minBytes) return destination

        if (assetName != null) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetName).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            require(destination.isFile && destination.length() > minBytes) {
                "Failed to stage bundled model at ${destination.absolutePath}"
            }
            return destination
        }

        error("Model missing. Import it into Retenqo's app storage: ${destination.absolutePath}")
    }

    private fun modelFile(context: Context, relativePath: String): File = File(modelsDirectory(context), relativePath)

    private fun modelsDirectory(context: Context): File =
        context.getExternalFilesDir("models") ?: File(context.filesDir, "models")

    private const val MIN_LLM_BYTES = 64L * 1024L * 1024L
    private const val MIN_AUX_MODEL_BYTES = 1_000_000L
}
