package com.mneme.app.notif

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mneme.app.data.IngestSources

/**
 * Important-app notification watcher (WA, Gmail, SMS, Telegram, …).
 * Extracts text → queue → WorkManager (LLM only when power gates allow).
 */
class MnemeNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        ingest(sbn)
    }

    private fun ingest(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.isOngoing) return
        if (!NotificationCaptureSettings.isPackageEnabled(applicationContext, sbn.packageName)) return
        if (!NotifTextTriage.isAllowed(sbn.packageName)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: messagingLastLine(extras)

        val candidate = NotifTextTriage.candidate(sbn.packageName, title, text) ?: return
        val sourceId = NotificationCaptureSettings.sourceIdForPackage(sbn.packageName) ?: return
        val source = IngestSources.fromNotifLabel(candidate.sourceLabel)
        Log.i(TAG, "queue [$source] (${candidate.text.length} chars)")
        val eventId = IngestSources.notificationEventFingerprint(sbn.packageName, sbn.key)
        NotifIngestQueue.offer(applicationContext, sourceId, source, candidate.text, eventId)
        NotifIngestWorker.enqueueSoon(applicationContext)
    }

    private fun messagingLastLine(extras: android.os.Bundle): String? {
        return try {
            @Suppress("DEPRECATION")
            val msgs = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
            if (msgs.isEmpty()) return null
            val last = msgs.last() as? android.os.Bundle ?: return null
            last.getCharSequence("text")?.toString()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MnemeNotifListener"
        @Volatile private var instance: MnemeNotificationListener? = null

        fun syncActiveNotifications() {
            instance?.getActiveNotifications()?.forEach { instance?.ingest(it) }
            instance?.let { NotifIngestWorker.enqueueSoon(it.applicationContext, delaySec = 2) }
        }

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, MnemeNotificationListener::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return flat.split(':').any {
                ComponentName.unflattenFromString(it)?.flattenToString() == cn.flattenToString()
            }
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
