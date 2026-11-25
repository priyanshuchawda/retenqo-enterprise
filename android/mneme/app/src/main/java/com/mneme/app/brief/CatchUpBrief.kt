package com.mneme.app.brief

import com.mneme.app.data.CardType
import com.mneme.app.data.NoteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Since-last-open / morning catch-up — pure logic, no LLM.
 */
data class CatchUpBrief(
    val newCards: List<NoteEntity>,
    val duesSoon: List<NoteEntity>,
    val staleWaiting: List<NoteEntity>,
) {
    val isEmpty: Boolean
        get() = newCards.isEmpty() && duesSoon.isEmpty() && staleWaiting.isEmpty()

    val headline: String
        get() {
            if (isEmpty) return "You're caught up"
            val parts = mutableListOf<String>()
            if (duesSoon.isNotEmpty()) parts.add("${duesSoon.size} due soon")
            if (newCards.isNotEmpty()) parts.add("${newCards.size} new")
            if (staleWaiting.isNotEmpty()) parts.add("${staleWaiting.size} waiting")
            return parts.joinToString(" · ")
        }
}

object CatchUpBriefBuilder {
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val STALE_WAITING_MS = TimeUnit.DAYS.toMillis(3)
    private val DUE_HORIZON_MS = TimeUnit.HOURS.toMillis(48)
    private val FIRST_OPEN_LOOKBACK_MS = TimeUnit.HOURS.toMillis(24)

    /**
     * @param lastOpenAt epoch ms of previous session; 0 = first open (look back 24h for “new”)
     */
    fun build(
        notes: List<NoteEntity>,
        lastOpenAt: Long,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): CatchUpBrief {
        val since = if (lastOpenAt <= 0L) now - FIRST_OPEN_LOOKBACK_MS else lastOpenAt
        val dueEnd = now + DUE_HORIZON_MS
        val active = notes.filter { it.isActive(now) }

        val newCards = active
            .filter { it.createdAt > since }
            .sortedByDescending { it.createdAt }
            .take(12)

        val duesSoon = active
            .filter { d ->
                val due = d.dueAt ?: return@filter false
                due <= dueEnd // overdue + next 48h
            }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
            .take(12)

        val staleWaiting = active
            .filter {
                it.type() == CardType.WAITING &&
                    it.createdAt <= now - STALE_WAITING_MS
            }
            .sortedBy { it.createdAt }
            .take(8)

        return CatchUpBrief(
            newCards = newCards,
            duesSoon = duesSoon,
            staleWaiting = staleWaiting,
        )
    }

    fun formatBody(
        brief: CatchUpBrief,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        if (brief.isEmpty) return "Nothing new. No dues in the next 48 hours."
        return buildString {
            if (brief.duesSoon.isNotEmpty()) {
                append("Due (next 48h / overdue):\n")
                brief.duesSoon.forEach { append(line(it, zone)).append('\n') }
            }
            if (brief.newCards.isNotEmpty()) {
                append("New since last open:\n")
                brief.newCards.forEach { append("• ${it.title}${who(it)}\n") }
            }
            if (brief.staleWaiting.isNotEmpty()) {
                append("Still waiting:\n")
                brief.staleWaiting.forEach { append("• ${it.title}${who(it)}\n") }
            }
        }.trim()
    }

    private fun who(n: NoteEntity): String =
        n.who?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()

    private fun line(n: NoteEntity, zone: ZoneId): String {
        val due = n.dueAt ?: return "• ${n.title}${who(n)}"
        val day = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
        return "• ${n.title}${who(n)} (${dayFmt.format(day)})"
    }
}
