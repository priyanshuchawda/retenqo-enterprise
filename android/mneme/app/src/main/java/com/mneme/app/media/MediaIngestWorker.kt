package com.mneme.app.media

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MnemeEmbedder
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.data.NoteRepository
import com.mneme.app.pulse.PulsePipeline
import java.util.concurrent.TimeUnit

/**
 * Battery-aware WhatsApp media ingest.
 * Periodic + event-triggered one-shots; heavy LLM only when [MnemePowerGates] allow.
 */
class MediaIngestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val user = inputData.getBoolean(KEY_USER, false)
        if (!user && !MediaIngestSettings.isPeriodicEnabled(applicationContext)) {
            Log.i(TAG, "skip: background media scan is disabled")
            return Result.success()
        }
        val limit = inputData.getInt(KEY_LIMIT, if (user) 4 else 2)
        val gate = MnemePowerGates.canRunHeavyWork(applicationContext, userInitiated = user)
        if (!gate.ok) {
            Log.i(TAG, "skip: ${gate.reason}")
            // Not a failure — try later when constraints improve
            return if (user) Result.failure(workDataOf(KEY_MSG to gate.reason))
            else Result.retry()
        }
        Log.i(TAG, "run: ${gate.reason} limit=$limit")

        val llm = LlmBridge(applicationContext)
        val embedder = MnemeEmbedder(applicationContext, llm)
        val db = MnemeDatabase.get(applicationContext)
        val notes = NoteRepository(db.notes(), embedder, db.followThrough())
        val pulse = PulsePipeline(applicationContext, notes, llm)
        val media = MediaIngestPipeline(applicationContext, pulse)
        return try {
            val result = media.scanWhatsAppRecent(limit = limit)
            Log.i(TAG, "media scan complete scanned=${result.scanned} saved=${result.saved} skipped=${result.skipped}")
            Result.success(workDataOf(KEY_MSG to result.message))
        } catch (e: Exception) {
            Log.w(TAG, "ingest failed (${e.javaClass.simpleName})")
            Result.retry()
        } finally {
            try {
                llm.unload()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "MediaIngestWorker"
        const val UNIQUE_PERIODIC = "mneme_wa_media_periodic"
        const val UNIQUE_ONESHOT = "mneme_wa_media_oneshot"
        const val KEY_USER = "user"
        const val KEY_LIMIT = "limit"
        const val KEY_MSG = "msg"

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        fun schedulePeriodic(context: Context) {
            if (!MediaIngestSettings.isPeriodicEnabled(context)) return
            val req = PeriodicWorkRequestBuilder<MediaIngestWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints())
                .setInputData(workDataOf(KEY_USER to false, KEY_LIMIT to 2))
                .addTag("mneme_media")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
            Log.i(TAG, "periodic 30m scheduled")
        }

        /** Debounced one-shot after MediaStore / folder hint. */
        fun enqueueSoon(context: Context, userInitiated: Boolean = false) {
            val req = OneTimeWorkRequestBuilder<MediaIngestWorker>()
                .setConstraints(if (userInitiated) Constraints.NONE else constraints())
                .setInputData(
                    workDataOf(
                        KEY_USER to userInitiated,
                        KEY_LIMIT to if (userInitiated) 4 else 2,
                    ),
                )
                .addTag("mneme_media")
                .setInitialDelay(if (userInitiated) 0 else 20, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                if (userInitiated) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                req,
            )
        }
    }
}
