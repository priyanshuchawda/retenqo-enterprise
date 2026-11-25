package com.mneme.app.media

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log

/**
 * Lightweight trigger: MediaStore image changes (WhatsApp often indexes downloads here).
 * Does NOT run OCR/LLM — only enqueues WorkManager after debounce.
 */
class MediaStoreTrigger(private val context: Context) {
    private val thread = HandlerThread("mneme-media-watch").also { it.start() }
    private val handler = Handler(thread.looper)
    private var lastEnqueueAt = 0L
    private var registered = false

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            onChange(selfChange)
        }

        override fun onChange(selfChange: Boolean) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastEnqueueAt < DEBOUNCE_MS) return
            lastEnqueueAt = now
            Log.i(TAG, "MediaStore change → enqueue ingest (debounced)")
            MediaIngestWorker.enqueueSoon(context.applicationContext, userInitiated = false)
        }
    }

    fun start() {
        if (registered) return
        try {
            val cr = context.contentResolver
            // Images (WA photos / screenshots) + Files (PDFs / docs often land here)
            cr.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
            cr.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                observer,
            )
            registered = true
            Log.i(TAG, "MediaStore observers registered (images + files)")
        } catch (e: Exception) {
            Log.w(TAG, "observer failed: ${e.message}")
        }
    }

    fun stop() {
        if (!registered) return
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
        registered = false
    }

    companion object {
        private const val TAG = "MediaStoreTrigger"
        private const val DEBOUNCE_MS = 45_000L
    }
}
