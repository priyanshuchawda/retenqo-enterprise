package com.mneme.test

import android.os.Environment
import java.io.File

object ModelPaths {
    private val mnemeRoot: File
        get() {
            val candidates = listOf(
                File("/sdcard/Mneme"),
                File(Environment.getExternalStorageDirectory(), "Mneme"),
            )
            return candidates.firstOrNull { it.isDirectory } ?: candidates.first()
        }

    val phone1Root: File get() = File(mnemeRoot, "models/phone1")

    val llmGguf: File get() = File(phone1Root, "llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf")
    val whisperBin: File get() = File(phone1Root, "whisper/ggml-base-q5_1.bin")
    val embedTflite: File get() =
        File(phone1Root, "embed/litert/embeddinggemma-300M_seq512_mixed-precision.tflite")
    val fallbackTask: File get() =
        File(phone1Root, "fallback/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task")
    val jfkWav: File get() = File(mnemeRoot, "test-assets/jfk.wav")

    fun describe(): String = buildString {
        appendLine("Mneme root: ${mnemeRoot.absolutePath}")
        appendLine("LLM: ${llmGguf.absolutePath} (${llmGguf.length()} bytes)")
        appendLine("Whisper: ${whisperBin.absolutePath} (${whisperBin.length()} bytes)")
        appendLine("Embed: ${embedTflite.absolutePath} (${embedTflite.length()} bytes)")
        appendLine("Fallback: ${fallbackTask.absolutePath} (${fallbackTask.length()} bytes)")
        appendLine("JFK wav: ${jfkWav.absolutePath} (${jfkWav.length()} bytes)")
    }
}
