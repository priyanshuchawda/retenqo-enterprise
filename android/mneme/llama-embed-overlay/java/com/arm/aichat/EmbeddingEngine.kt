package com.arm.aichat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Qwen3-Embedding (or any llama.cpp embedding GGUF) via JNI.
 * Separate from [InferenceEngine] chat model — unload one before loading the other on 6 GB phones.
 */
class EmbeddingEngine private constructor(
    private val nativeLibDir: String,
) {
    private val mutex = Mutex()
    private var loadedPath: String? = null

    suspend fun load(modelPath: String): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureNative()
            val file = File(modelPath)
            require(file.isFile) { "Embedding model missing: $modelPath" }
            if (loadedPath == modelPath && nativeIsLoaded()) return@withContext true
            nativeUnload()
            val rc = nativeLoad(nativeLibDir, modelPath)
            if (rc != 0) {
                Log.e(TAG, "nativeLoad failed rc=$rc for $modelPath")
                loadedPath = null
                return@withContext false
            }
            loadedPath = modelPath
            true
        }
    }

    suspend fun embed(text: String): FloatArray? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!nativeIsLoaded()) return@withContext null
            nativeEmbed(text)
        }
    }

    suspend fun unload() = mutex.withLock {
        withContext(Dispatchers.IO) {
            nativeUnload()
            loadedPath = null
        }
    }

    fun isLoaded(): Boolean = nativeIsLoaded()

    private fun ensureNative() {
        try {
            System.loadLibrary("ai-chat")
        } catch (e: UnsatisfiedLinkError) {
            // Already loaded by InferenceEngineImpl
            Log.d(TAG, "ai-chat already loaded: ${e.message}")
        }
    }

    private external fun nativeLoad(nativeLibDir: String, modelPath: String): Int
    private external fun nativeEmbed(text: String): FloatArray?
    private external fun nativeUnload()
    private external fun nativeIsLoaded(): Boolean

    companion object {
        private const val TAG = "EmbeddingEngine"

        @Volatile
        private var instance: EmbeddingEngine? = null

        fun get(context: Context): EmbeddingEngine {
            return instance ?: synchronized(this) {
                instance ?: EmbeddingEngine(
                    context.applicationInfo.nativeLibraryDir,
                ).also { instance = it }
            }
        }
    }
}
