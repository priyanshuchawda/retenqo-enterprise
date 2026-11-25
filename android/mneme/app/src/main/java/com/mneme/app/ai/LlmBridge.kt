package com.mneme.app.ai

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.mneme.app.ai.ModelPaths.ensureLlmAccessible
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Raised when local generation cannot be made ready. No network fallback is provided. */
class OfflineGenerationUnavailable(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/** llama.cpp-backed local generation entry point. One model and one inference at a time. */
class LlmBridge(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = sharedMutex
    private val engineDelegate = lazy { AiChat.getInferenceEngine(appContext) }
    private val engine: InferenceEngine get() = engineDelegate.value

    val isAvailable: Boolean
        get() = runCatching { ModelPaths.llmGguf(appContext).let { it.isFile && it.length() > 64L * 1024L * 1024L } }
            .getOrDefault(false)

    suspend fun ensureLoaded() = mutex.withLock {
        ensureLoadedLocked()
    }

    suspend fun unload() = mutex.withLock {
        if (!engineDelegate.isInitialized()) {
            sharedLoadedModelPath = null
            return@withLock
        }
        val current = runCatching { engine.state.value }.getOrNull()
        if (current is InferenceEngine.State.ModelReady || current is InferenceEngine.State.Error) {
            engine.cleanUp()
        }
        sharedLoadedModelPath = null
    }

    suspend fun complete(prompt: String, maxTokens: Int = 96): String = mutex.withLock {
        require(prompt.isNotBlank()) { "Prompt cannot be blank" }
        val predictLength = maxTokens.coerceIn(1, 256)
        ensureLoadedLocked()
        try {
            engine.resetConversation(SYSTEM_PROMPT)
            engine.sendUserPrompt(prompt, predictLength).toList().joinToString(separator = "").trim()
        } catch (e: Exception) {
            Log.w(TAG, "Local inference failed (${e.javaClass.simpleName})")
            throw OfflineGenerationUnavailable("The local model could not generate a response.", e)
        }
    }

    private suspend fun ensureLoadedLocked() {
        val model = try {
            ensureLlmAccessible(appContext)
        } catch (e: Exception) {
            throw OfflineGenerationUnavailable(e.message ?: "No local GGUF model is installed.", e)
        }
        val path = model.absolutePath

        val state = try {
            engine.state.first {
                it is InferenceEngine.State.Initialized ||
                    it is InferenceEngine.State.ModelReady ||
                    it is InferenceEngine.State.Error
            }
        } catch (e: Exception) {
            throw OfflineGenerationUnavailable("Could not initialize llama.cpp.", e)
        }

        if (state is InferenceEngine.State.Error) {
            engine.cleanUp()
            sharedLoadedModelPath = null
        } else if (state is InferenceEngine.State.ModelReady && sharedLoadedModelPath == path) {
            return
        } else if (state is InferenceEngine.State.ModelReady) {
            engine.cleanUp()
            sharedLoadedModelPath = null
        }

        try {
            engine.loadModel(path)
            sharedLoadedModelPath = path
        } catch (e: Exception) {
            runCatching { engine.cleanUp() }
            sharedLoadedModelPath = null
            throw OfflineGenerationUnavailable("Could not load the local GGUF model.", e)
        }
    }

    companion object {
        private const val TAG = "RetenqoLocalAI"
        private const val SYSTEM_PROMPT =
            "You are Retenqo, a private on-device assistant. Keep answers concise. " +
                "For personal-memory questions, use only facts explicitly supplied in the user message. " +
                "Never invent facts or citations."
        private val sharedMutex = Mutex()
        private var sharedLoadedModelPath: String? = null
    }
}
