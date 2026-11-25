package com.mneme.app.remind

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mneme.app.MainActivity
import com.mneme.app.R
import com.mneme.app.data.MnemeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Schedules a light notification on the due morning (9:00) for notes with [dueAt].
 */
object DueReminderScheduler {
    private const val TAG = "DueReminder"
    const val CHANNEL_ID = "mneme_due"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Due reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Retenqo reminders for notes with due dates"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun schedule(context: Context, noteId: Long, title: String, dueAt: Long) {
        val trigger = DueDigest.reminderTriggerMillis(dueAt) ?: run {
            Log.i(TAG, "skip past due note=$noteId")
            return
        }
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(context, noteId, title)
        try {
            if (!am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
            Log.i(TAG, "scheduled note=$noteId at $trigger")
        } catch (e: Exception) {
            Log.w(TAG, "schedule failed (${e.javaClass.simpleName})")
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(context, noteId, ""))
        NotificationManagerCompat.from(context).cancel(2_000 + noteId.toInt())
    }

    /** Re-arm upcoming due notes after reboot (lightweight; no LLM). */
    fun rescheduleUpcoming(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (start, end) = DueDigest.queryWindow()
                val notes = MnemeDatabase.get(context).notes()
                    .dueBetween(start, end, System.currentTimeMillis())
                var n = 0
                for (note in notes) {
                    val due = note.dueAt ?: continue
                    schedule(context, note.id, note.title, due)
                    n++
                }
                Log.i(TAG, "rescheduled $n due reminders")
            } catch (e: Exception) {
                Log.w(TAG, "reminder restore failed (${e.javaClass.simpleName})")
            }
        }
    }

    private fun pending(context: Context, noteId: Long, title: String): PendingIntent {
        val intent = Intent(context, DueReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .putExtra(EXTRA_TITLE, title)
        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class DueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(DueReminderScheduler.EXTRA_TITLE) ?: "Something is due"
        val noteId = intent.getLongExtra(DueReminderScheduler.EXTRA_NOTE_ID, 0L)
        DueReminderScheduler.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, DueReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Due today")
            .setContentText("Open Retenqo to view this reminder")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2_000 + noteId.toInt(), notif)
            Log.i("DueReminder", "reminder notification posted")
        } catch (e: SecurityException) {
            Log.w("DueReminder", "notification permission unavailable")
        }
    }
}
