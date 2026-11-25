package com.mneme.app.ai

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Lightweight on-device hashed vectors for retrieval while a learned local embedder is pending. */
class MnemeEmbedder(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val llm: LlmBridge? = null,
) {
    private val mutex = Mutex()

    suspend fun embed(text: String): FloatArray = mutex.withLock {
        HashEmbedder.embed(text.trim())
    }

    fun toBytes(vec: FloatArray): ByteArray = HashEmbedder.toBytes(vec)
    fun fromBytes(bytes: ByteArray?): FloatArray? = HashEmbedder.fromBytes(bytes)
    fun cosine(a: FloatArray, b: FloatArray): Float = HashEmbedder.cosine(a, b)
}
