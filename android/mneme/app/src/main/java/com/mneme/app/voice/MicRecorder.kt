package com.mneme.app.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Records 16 kHz mono PCM for Whisper. Call [start], then [stopAndGetSamples].
 */
class MicRecorder {
    private var record: AudioRecord? = null
    private val chunks = ArrayList<ShortArray>()
    private var recording = false

    @SuppressLint("MissingPermission")
    fun start() {
        stopInternal()
        chunks.clear()
        val min = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(min, SAMPLE_RATE / 2)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        require(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }
        record = rec
        recording = true
        rec.startRecording()
    }

    suspend fun pump() = withContext(Dispatchers.IO) {
        val rec = record ?: return@withContext
        val buf = ShortArray(SAMPLE_RATE / 10)
        while (coroutineContext.isActive && recording) {
            val n = rec.read(buf, 0, buf.size)
            if (n > 0) {
                chunks.add(buf.copyOf(n))
            } else if (n < 0) {
                Log.w(TAG, "AudioRecord read=$n")
                break
            }
        }
    }

    fun stopAndGetSamples(): FloatArray {
        recording = false
        try {
            record?.stop()
        } catch (_: Exception) {
        }
        try {
            record?.release()
        } catch (_: Exception) {
        }
        record = null
        val total = chunks.sumOf { it.size }
        if (total == 0) return FloatArray(0)
        val out = FloatArray(total)
        var i = 0
        for (c in chunks) {
            for (s in c) {
                out[i++] = (s / 32767.0f).coerceIn(-1f, 1f)
            }
        }
        chunks.clear()
        return out
    }

    private fun stopInternal() {
        recording = false
        try {
            record?.stop()
        } catch (_: Exception) {
        }
        try {
            record?.release()
        } catch (_: Exception) {
        }
        record = null
    }

    companion object {
        private const val TAG = "MnemeMic"
        const val SAMPLE_RATE = 16_000
    }
}
