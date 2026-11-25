package com.mneme.app.voice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mneme.app.MainActivity
import com.mneme.app.R
import com.mneme.app.remind.DueDigest
import com.mneme.app.remind.DueReminderScheduler
import java.util.Calendar

sealed class VoiceIntent {
    data class Query(val text: String) : VoiceIntent()
    data class Remember(val text: String) : VoiceIntent()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : VoiceIntent()
    data object WhatsDue : VoiceIntent()
    data class Route(val destination: String) : VoiceIntent()
    data object AccessibilityHelp : VoiceIntent()
    data object Capabilities : VoiceIntent()
    data object CheckEmail : VoiceIntent()
    data object ReadNotifications : VoiceIntent()
    data class AddCalendar(val title: String) : VoiceIntent()
    data class CreateTask(val text: String) : VoiceIntent()
}

object VoiceIntentRouter {
    private val alarmRe = Regex(
        """(?i)\b(?:set\s+(?:an?\s+)?alarm|wake\s+me|remind\s+me)\b.*?\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""",
    )
    private val rememberRe = Regex("""(?i)^\s*(?:remember|save|note)\b[:\s]+(.+)$""")
    private val routeRe = Regex("""(?i)^\s*(?:navigate|route|directions?|take me|how do i get)\s+(?:to\s+)?(.+)$""")
    private val calendarRe = Regex("""(?i)^\s*(?:add|put|create|schedule)\s+(.+?)\s+(?:to|in|on)\s+(?:my\s+)?calendar\s*$""")
    private val taskRe = Regex("""(?i)^\s*(?:make|create|add)\s+(?:a\s+)?task\s*[:\-]?\s*(.+)$""")

    fun route(transcript: String): VoiceIntent {
        val t = transcript.trim()
        if (t.isBlank()) return VoiceIntent.Query("")

        if (DueDigest.isDueQuery(t)) return VoiceIntent.WhatsDue
        if (Regex("""(?i)\bwhat can you do|what do you do|your capabilities|help me with my phone\b""").containsMatchIn(t)) {
            return VoiceIntent.Capabilities
        }
        if (Regex("""(?i)\b(check|read|open|show)\b.*\b(email|mail|gmail)\b""").containsMatchIn(t)) {
            return VoiceIntent.CheckEmail
        }
        if (Regex("""(?i)\b(read|show|check)\b.*\b(notification|notifications|whatsapp messages?)\b""").containsMatchIn(t)) {
            return VoiceIntent.ReadNotifications
        }
        calendarRe.find(t)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            return VoiceIntent.AddCalendar(it)
        }
        taskRe.find(t)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            return VoiceIntent.CreateTask(it)
        }
        if (Regex("""(?i)\b(disabled|wheelchair|blind|low vision|accessibility|help me get around)\b""").containsMatchIn(t)) {
            return VoiceIntent.AccessibilityHelp
        }
        routeRe.find(t)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            return VoiceIntent.Route(it)
        }

        alarmRe.find(t)?.let { m ->
            var hour = m.groupValues[1].toIntOrNull() ?: return@let
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) {
                return VoiceIntent.SetAlarm(hour, minute, label = t.take(80))
            }
        }

        rememberRe.find(t)?.let { m ->
            val body = m.groupValues[1].trim()
            if (body.isNotBlank()) return VoiceIntent.Remember(body)
        }

        return VoiceIntent.Query(t)
    }
}

class AlarmScheduler(private val context: Context) {
    /**
     * Prefer system AlarmClock UI intent (works without SCHEDULE_EXACT_ALARM on many OEMs).
     * Falls back to AlarmManager + notification broadcast.
     */
    fun schedule(hour: Int, minute: Int, label: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm set for %02d:%02d".format(hour, minute)
        } catch (e: Exception) {
            Log.w(TAG, "alarm clock integration failed (${e.javaClass.simpleName})")
            scheduleExact(hour, minute, label)
        }
    }

    private fun scheduleExact(hour: Int, minute: Int, label: String): String {
        DueReminderScheduler.ensureChannel(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (hour * 60 + minute),
            Intent(context, MnemeAlarmReceiver::class.java).putExtra("label", label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (!am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
        return "Reminder armed for %02d:%02d".format(hour, minute)
    }

    companion object {
        private const val TAG = "MnemeAlarm"
    }
}

class MnemeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Retenqo reminder"
        Log.i("MnemeAlarm", "reminder alarm fired")
        DueReminderScheduler.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, DueReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Retenqo reminder")
            .setContentText("Open Retenqo to view this reminder")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(3_001, notif)
        } catch (e: SecurityException) {
            Log.w("MnemeAlarm", "notification permission unavailable")
        }
    }
}
