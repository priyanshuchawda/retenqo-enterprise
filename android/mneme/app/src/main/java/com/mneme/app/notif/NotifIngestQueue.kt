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
        offerLegacy(context, source, text, sourceEventId)
    }

    @Synchronized
    fun offer(context: Context, sourceId: String, source: String, text: String, sourceEventId: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = NotifQueueLogic.entries(prefs.getString(KEY, "[]") ?: "[]")
        val nextEntry = NotifQueueLogic.encodeStable(sourceId, source, text, sourceEventId)
        val duplicate = current.any { encoded ->
            NotifQueueLogic.decodeEntry(encoded).text.trim().replace(Regex("\\s+"), " ")
                .equals(text.trim().replace(Regex("\\s+"), " "), ignoreCase = true)
        }
        val next = if (duplicate) NotifQueueLogic.encodeEntries(current) else {
            NotifQueueLogic.encodeEntries((current + nextEntry).takeLast(MAX))
        }
        check(prefs.edit().putString(KEY, next).commit()) { "Unable to persist notification queue" }
    }

    private fun offerLegacy(context: Context, source: String, text: String, sourceEventId: String?) {
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
            val entry = item.sourceId?.let {
                NotifQueueLogic.encodeStable(it, item.source, item.text, item.sourceEventId)
            } ?: NotifQueueLogic.encode(item.source, item.text, item.sourceEventId)
            val entries = NotifQueueLogic.entries(encoded)
            if (entries.none { NotifQueueLogic.decodeEntry(it).text == item.text }) {
                encoded = NotifQueueLogic.encodeEntries((entries + entry).takeLast(MAX))
            }
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
        fun retainEnabled(raw: String): List<String> = NotificationCaptureSettings.normalizeQueueEntries(
            NotifQueueLogic.entries(raw),
            enabled,
        )
        val keptPending = retainEnabled(pending)
        val keptInFlight = retainEnabled(inFlight)
        val removed = NotifQueueLogic.size(pending) + NotifQueueLogic.size(inFlight) - keptPending.size - keptInFlight.size
        val normalizedPending = NotifQueueLogic.encodeEntries(keptPending)
        val normalizedInFlight = NotifQueueLogic.encodeEntries(keptInFlight)
        if (removed > 0 || normalizedPending != pending || normalizedInFlight != inFlight) {
            check(
                prefs.edit()
                    .putString(KEY, normalizedPending)
                    .putString(IN_FLIGHT_KEY, normalizedInFlight)
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
