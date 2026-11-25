package com.mneme.app.notif

import android.content.Context
import androidx.core.content.edit

/**
 * Tiny durable queue for notification texts (no Room schema churn).
 * Listener only appends; [NotifIngestWorker] drains.
 */
object NotifIngestQueue {
    private const val PREFS = "mneme_notif_queue"
    private const val KEY = "pending"
    private const val IN_FLIGHT_KEY = "in_flight"
    private const val MAX = 500

    @Synchronized
    fun offer(context: Context, source: String, text: String, sourceEventId: String? = null) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = NotifQueueLogic.offer(
            prefs.getString(KEY, "[]") ?: "[]",
            source,
            text,
            MAX,
            sourceEventId,
        )
        check(prefs.edit().putString(KEY, next).commit()) { "Unable to persist notification queue" }
    }

    /** Claims work durably; an interrupted process's previous claim is returned to pending. */
    @Synchronized
    fun claim(context: Context, limit: Int): List<NotifQueueLogic.Entry> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val claim = NotifQueueLogic.claim(
            prefs.getString(KEY, "[]") ?: "[]",
            prefs.getString(IN_FLIGHT_KEY, "[]") ?: "[]",
            limit,
        )
        check(
            prefs.edit()
                .putString(KEY, claim.pending)
                .putString(IN_FLIGHT_KEY, claim.inFlight)
                .commit(),
        ) { "Unable to claim notification queue" }
        return claim.batch.map { NotifQueueLogic.decodeEntry(it) }
    }

    @Synchronized
    fun acknowledge(context: Context, completed: Set<NotifQueueLogic.Entry>) {
        if (completed.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val inFlight = NotifQueueLogic.entries(prefs.getString(IN_FLIGHT_KEY, "[]") ?: "[]")
        val remaining = inFlight.filterNot { encoded ->
            val decoded = NotifQueueLogic.decodeEntry(encoded)
            completed.any { it == decoded }
        }
        check(prefs.edit().putString(IN_FLIGHT_KEY, NotifQueueLogic.encodeEntries(remaining)).commit()) {
            "Unable to acknowledge notification queue"
        }
    }

    @Synchronized
    fun restore(context: Context, batch: List<NotifQueueLogic.Entry>) {
        if (batch.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val inFlight = prefs.getString(IN_FLIGHT_KEY, "[]") ?: "[]"
        var encoded = prefs.getString(KEY, "[]") ?: "[]"
        batch.asReversed().forEach { item ->
            encoded = NotifQueueLogic.offer(encoded, item.source, item.text, MAX, item.sourceEventId)
        }
        check(
            prefs.edit()
                .putString(KEY, merge(inFlight, encoded))
                .putString(IN_FLIGHT_KEY, "[]")
                .commit(),
        ) { "Unable to restore notification queue" }
    }

    /** Remove queued items whose user consent has been revoked before processing starts. */
    @Synchronized
    fun removeDisabledSources(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = prefs.getString(KEY, "[]") ?: "[]"
        val inFlight = prefs.getString(IN_FLIGHT_KEY, "[]") ?: "[]"
        val enabled = NotificationCaptureSettings.enabledSources(context)
        fun retainEnabled(raw: String): List<String> = NotifQueueLogic.entries(raw).filter { encoded ->
            val item = NotifQueueLogic.decodeEntry(encoded)
            val source = NotificationCaptureSettings.sources.firstOrNull { it.label == item.source }
            source != null && source.id in enabled
        }
        val keptPending = retainEnabled(pending)
        val keptInFlight = retainEnabled(inFlight)
        val removed = NotifQueueLogic.size(pending) + NotifQueueLogic.size(inFlight) - keptPending.size - keptInFlight.size
        if (removed > 0) {
            check(
                prefs.edit()
                    .putString(KEY, NotifQueueLogic.encodeEntries(keptPending))
                    .putString(IN_FLIGHT_KEY, NotifQueueLogic.encodeEntries(keptInFlight))
                    .commit(),
            ) { "Unable to apply notification consent changes" }
        }
        return removed
    }

    @Synchronized
    fun size(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NotifQueueLogic.size(prefs.getString(KEY, "[]") ?: "[]") +
            NotifQueueLogic.size(prefs.getString(IN_FLIGHT_KEY, "[]") ?: "[]")
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY)
            remove(IN_FLIGHT_KEY)
        }
    }

    private fun merge(first: String, second: String): String {
        val combined = (NotifQueueLogic.entries(first) + NotifQueueLogic.entries(second)).distinct()
        return NotifQueueLogic.encodeEntries(combined.takeLast(MAX))
    }
}
