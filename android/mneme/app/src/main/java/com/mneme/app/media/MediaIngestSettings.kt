package com.mneme.app.media

import android.content.Context
import androidx.core.content.edit

/** Explicit user opt-in for automatic WhatsApp media access and OCR. */
object MediaIngestSettings {
    private const val PREFS = "retenqo_media_ingest"
    private const val KEY_PERIODIC_ENABLED = "periodic_enabled"

    fun isPeriodicEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERIODIC_ENABLED, false)

    fun setPeriodicEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PERIODIC_ENABLED, enabled)
        }
    }
}
