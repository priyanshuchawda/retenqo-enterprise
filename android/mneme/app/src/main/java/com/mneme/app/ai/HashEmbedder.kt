package com.mneme.app.ai

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Local semantic vectors without loading a heavy embedding model.
 * Hashed char/word n-grams → fixed dim, L2-normalized. Used when Qwen embed GGUF is absent;
 * [MnemeEmbedder] prefers Qwen3-Embedding-0.6B when available.
 */
object HashEmbedder {
    const val DIM = 256

    fun embed(text: String): FloatArray {
        val out = FloatArray(DIM)
        val normalized = text.lowercase().replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return out

        fun add(token: String, weight: Float) {
            val h = token.hashCode()
            val idx = (h and 0x7fffffff) % DIM
            val sign = if ((h ushr 31) == 0) 1f else -1f
            out[idx] += sign * weight
        }

        normalized.split(' ').filter { it.isNotBlank() }.forEach { w ->
            add(w, 1f)
            if (w.length >= 3) {
                for (i in 0..w.length - 3) add(w.substring(i, i + 3), 0.35f)
            }
        }
        for (i in 0..normalized.length - 4) {
            add(normalized.substring(i, i + 4), 0.15f)
        }

        var norm = 0.0
        for (v in out) norm += v * v
        norm = sqrt(norm)
        if (norm > 1e-8) {
            for (i in out.indices) out[i] = (out[i] / norm).toFloat()
        }
        return out
    }

    fun toBytes(vec: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(vec.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        vec.forEach { buf.putFloat(it) }
        return buf.array()
    }

    fun fromBytes(bytes: ByteArray?): FloatArray? {
        if (bytes == null || bytes.size < 4) return null
        val n = bytes.size / 4
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(n) { buf.float }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var dot = 0f
        for (i in 0 until n) dot += a[i] * b[i]
        return dot
    }
}
