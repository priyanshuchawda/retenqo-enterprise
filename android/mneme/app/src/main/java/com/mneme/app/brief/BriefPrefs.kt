package com.mneme.app.brief

import android.content.Context
import androidx.core.content.edit

/** Persists last app-open time for since-last-open brief. */
object BriefPrefs {
    private const val PREFS = "retenqo_brief"
    private const val KEY_LAST_OPEN = "last_open_at_ms"

    fun lastOpenAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_OPEN, 0L)

    fun markOpened(context: Context, at: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_OPEN, at)
        }
    }
}
