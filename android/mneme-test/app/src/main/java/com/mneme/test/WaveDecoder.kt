package com.mneme.test

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WaveDecoder {
    /** Decode 16-bit PCM WAV to float samples normalized to [-1, 1]. */
    fun decode(file: File): FloatArray {
        val bytes = file.readBytes()
        require(bytes.size >= 44) { "WAV too small" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val channels = buffer.getShort(22).toInt()
        buffer.position(44)
        val shortBuffer = buffer.asShortBuffer()
        val shorts = ShortArray(shortBuffer.limit())
        shortBuffer.get(shorts)
        val frameCount = shorts.size / channels
        return FloatArray(frameCount) { index ->
            val sample = if (channels == 1) {
                shorts[index].toInt()
            } else {
                (shorts[2 * index] + shorts[2 * index + 1]) / 2
            }
            (sample / 32767.0f).coerceIn(-1f, 1f)
        }
    }
}
