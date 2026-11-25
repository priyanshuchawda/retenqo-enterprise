package com.mneme.app.voice

import android.content.Context
import android.util.Log
import com.mneme.app.ai.ModelPaths
import com.whispercpp.java.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WhisperBridge(private val context: Context) {
    private val mutex = Mutex()
    private var ctx: WhisperContext? = null

    suspend fun unload() = mutex.withLock {
        try {
            ctx?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Whisper unload failed (${e.javaClass.simpleName})")
        }
        ctx = null
    }

    suspend fun transcribe(samples: FloatArray): String = mutex.withLock {
        if (ctx == null) {
            val model = withContext(Dispatchers.IO) {
                ModelPaths.ensureWhisperAccessible(context.applicationContext)
            }
            Log.i(TAG, "Loading local Whisper model")
            ctx = WhisperContext.createContextFromFile(model.absolutePath)
        }
        val c = ctx ?: error("Whisper not loaded")
        withContext(Dispatchers.Default) {
            c.transcribeData(samples).trim()
        }
    }

    companion object {
        private const val TAG = "MnemeWhisper"
    }
}
