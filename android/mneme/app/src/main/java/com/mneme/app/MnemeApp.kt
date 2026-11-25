package com.mneme.app

import android.app.Application
import android.util.Log
import com.mneme.app.media.MediaIngestWorker
import com.mneme.app.media.MediaStoreTrigger
import com.mneme.app.media.MediaIngestSettings
import com.mneme.app.settings.AppSetup
import com.mneme.app.remind.DueReminderScheduler
import androidx.work.WorkManager

class MnemeApp : Application() {
    lateinit var mediaTrigger: MediaStoreTrigger
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        DueReminderScheduler.ensureChannel(this)
        // Background access to personal media is opt-in. Do not scan at first launch.
        mediaTrigger = MediaStoreTrigger(this)
        refreshMediaIngest()
        DueReminderScheduler.rescheduleUpcoming(this)
        Log.i(TAG, "MnemeApp ready (battery-aware media + due reminders)")
    }

    fun refreshMediaIngest() {
        val enabled = MediaIngestSettings.isPeriodicEnabled(this) && AppSetup.snapshot(this).canReadMedia
        if (enabled) {
            MediaIngestWorker.schedulePeriodic(this)
            mediaTrigger.start()
        } else {
            WorkManager.getInstance(this).cancelUniqueWork(MediaIngestWorker.UNIQUE_PERIODIC)
            WorkManager.getInstance(this).cancelAllWorkByTag("mneme_media")
            mediaTrigger.stop()
        }
    }

    companion object {
        private const val TAG = "MnemeApp"
        lateinit var instance: MnemeApp
            private set
    }
}
