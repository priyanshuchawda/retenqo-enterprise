package com.mneme.app.notif

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MnemeEmbedder
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.data.NoteRepository
import com.mneme.app.media.MnemePowerGates
import com.mneme.app.pulse.PulsePipeline
import java.util.concurrent.TimeUnit

/**
 * Battery-aware notification text → note. Listener never runs the LLM.
 */
class NotifIngestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (NotifIngestQueue.size(applicationContext) == 0) {
            return Result.success()
        }
        val gate = MnemePowerGates.canRunHeavyWork(applicationContext, userInitiated = false)
        if (!gate.ok) {
            Log.i(TAG, "defer: ${gate.reason}")
            return Result.retry()
        }
        val batch = NotifIngestQueue.drain(applicationContext, limit = 3)
        if (batch.isEmpty()) return Result.success()

        val llm = LlmBridge(applicationContext)
        val embedder = MnemeEmbedder(applicationContext, llm)
        val db = MnemeDatabase.get(applicationContext)
        val notes = NoteRepository(db.notes(), embedder, db.followThrough())
        val pulse = PulsePipeline(applicationContext, notes, llm)
        var saved = 0
        var skipped = 0
        var idx = 0
        return try {
            while (idx < batch.size) {
                val item = batch[idx]
                val r = pulse.ingestText(item.text, sourceLabel = item.source, sourceEventId = item.sourceEventId)
                if (r.noteId != null) saved++ else skipped++
                Log.i(TAG, "processed source=${item.source} saved=${r.noteId != null}")
                idx++
            }
            val msg = "Notif cards: saved $saved · skipped $skipped"
            Log.i(TAG, msg)
            if (NotifIngestQueue.size(applicationContext) > 0) {
                enqueueSoon(applicationContext, delaySec = 45)
            }
            Result.success(workDataOf(KEY_MSG to msg))
        } catch (e: Exception) {
            Log.w(TAG, "error: ${e.message}")
            for (i in idx until batch.size) {
                val item = batch[i]
                NotifIngestQueue.offer(applicationContext, item.source, item.text, item.sourceEventId)
            }
            Result.retry()
        } finally {
            try {
                llm.unload()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "NotifIngestWorker"
        const val UNIQUE = "mneme_wa_notif_oneshot"
        const val KEY_MSG = "msg"

        fun constraints(): Constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        fun enqueueSoon(context: Context, delaySec: Long = 25) {
            val req = OneTimeWorkRequestBuilder<NotifIngestWorker>()
                .setConstraints(constraints())
                .setInitialDelay(delaySec, TimeUnit.SECONDS)
                .addTag("mneme_notif")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.KEEP,
                req,
            )
        }
    }
}
