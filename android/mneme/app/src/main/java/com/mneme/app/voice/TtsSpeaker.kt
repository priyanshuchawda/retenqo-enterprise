package com.mneme.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class TtsSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
            } else {
                Log.w(TAG, "TTS init failed status=$status")
            }
        }
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) return
        val cleaned = text.replace(Regex("\\s+"), " ").trim().take(400)
        if (cleaned.isBlank()) return
        engine.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "mneme-${System.currentTimeMillis()}")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ready = false
    }

    companion object {
        private const val TAG = "MnemeTts"
    }
}
