package com.mneme.app.notif

import android.content.Context
import androidx.core.content.edit

/** User-controlled notification sources. Package aliases are grouped into one setting per app. */
object NotificationCaptureSettings {
    data class Source(val id: String, val label: String, val packages: Set<String>)

    val sources = listOf(
        Source("whatsapp", "WhatsApp", setOf("com.whatsapp", "com.whatsapp.w4b")),
        Source("gmail", "Gmail", setOf("com.google.android.gm", "com.google.android.apps.gmail")),
        Source("messages", "Messages", setOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging")),
        Source("telegram", "Telegram", setOf("org.telegram.messenger", "org.telegram.messenger.web")),
        Source("outlook", "Outlook", setOf("com.microsoft.office.outlook")),
        Source("calendar", "Calendar", setOf("com.google.android.calendar", "com.android.calendar")),
        Source("slack", "Slack", setOf("com.Slack", "com.slack")),
    )

    private const val PREFS = "retenqo_notification_sources"
    private const val KEY_ENABLED = "enabled"

    internal fun defaultEnabledSources(): Set<String> = emptySet()

    fun enabledSources(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ENABLED, null)
            ?.toSet()
            ?: defaultEnabledSources()

    fun setEnabled(context: Context, sourceId: String, enabled: Boolean) {
        if (sources.none { it.id == sourceId }) return
        val current = enabledSources(context).toMutableSet()
        if (enabled) current += sourceId else current -= sourceId
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putStringSet(KEY_ENABLED, current)
        }
    }

    fun isPackageEnabled(context: Context, packageName: String?): Boolean {
        return isPackageEnabled(packageName, enabledSources(context))
    }

    internal fun isPackageEnabled(packageName: String?, enabledSourceIds: Set<String>): Boolean {
        if (packageName == null) return false
        val source = sources.firstOrNull { packageName in it.packages } ?: return false
        return source.id in enabledSourceIds
    }
}
