package com.mneme.app.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.mneme.app.notif.MnemeNotificationListener

/**
 * Snapshot of setup permissions for Settings / first-run.
 * Pure status reads — UI / Activity owns request launchers.
 */
data class AppSetupStatus(
    val postNotifications: Boolean,
    val notifListener: Boolean,
    val mediaImages: Boolean,
    val microphone: Boolean,
    val exactAlarms: Boolean,
    val accessibilityService: Boolean = false,
) {
    /** Needed for due reminders + ingest feedback. */
    val canNotify: Boolean get() = postNotifications

    /** Needed for WA / Gmail / SMS text ingest. */
    val canReadNotifs: Boolean get() = notifListener

    /** Needed for Scan WA / MediaStore watcher. */
    val canReadMedia: Boolean get() = mediaImages

    val missingCritical: Boolean
        get() = !canNotify || !canReadNotifs

    val readyForDailyUse: Boolean
        get() = canNotify && canReadNotifs && canReadMedia
}

object AppSetup {
    private const val PREFS = "retenqo_setup"
    private const val KEY_SEEN = "setup_screen_seen_v1"

    internal fun hasPhotoLibraryAccess(fullAccess: Boolean, selectedPhotosAccess: Boolean): Boolean =
        fullAccess || selectedPhotosAccess

    fun snapshot(context: Context): AppSetupStatus {
        val post = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        val selectedPhotosAccess = if (Build.VERSION.SDK_INT >= 34) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        val media = hasPhotoLibraryAccess(
            fullAccess = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED,
            selectedPhotosAccess = selectedPhotosAccess,
        )
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val exact = context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        val accessibility = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )?.split(':')?.any { it.contains(context.packageName) } == true
        return AppSetupStatus(
            postNotifications = post,
            notifListener = MnemeNotificationListener.isEnabled(context),
            mediaImages = media,
            microphone = mic,
            exactAlarms = exact,
            accessibilityService = accessibility,
        )
    }

    fun hasSeenSetup(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSetupSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SEEN, true)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
