package com.mneme.app.remind

import com.mneme.app.data.NoteEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure "what's due" digest — no Android / LLM.
 */
object DueDigest {
    private val dueQuery = Regex(
        """(?i)\b(what'?s\s+due|whats\s+due|due\s+today|due\s+this\s+week|upcoming\s+deadlines?|my\s+reminders?|deadlines?\s+today)\b""",
    )
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    fun isDueQuery(question: String): Boolean =
        dueQuery.containsMatchIn(question.trim())

    /** [startMs, endMs] covering overdue week → next 14 days (inclusive calendar). */
    fun queryWindow(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(14).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun format(
        notes: List<NoteEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val now = System.currentTimeMillis()
        val active = notes.filter { it.dueAt != null && it.isActive(now) }
        if (active.isEmpty()) {
            return "Nothing with a due date in the next two weeks."
        }
        val overdue = mutableListOf<String>()
        val todayList = mutableListOf<String>()
        val upcoming = mutableListOf<String>()
        for (n in active.sortedBy { it.dueAt ?: Long.MAX_VALUE }) {
            val due = n.dueAt ?: continue
            val day = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
            val who = n.who?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
            val line = "• ${n.title}$who (${dayFmt.format(day)})"
            when {
                day.isBefore(today) -> overdue.add(line)
                day == today -> todayList.add(line)
                else -> upcoming.add(line)
            }
        }
        return buildString {
            if (overdue.isNotEmpty()) {
                append("Overdue:\n")
                overdue.forEach { append(it).append('\n') }
            }
            if (todayList.isNotEmpty()) {
                append("Due today:\n")
                todayList.forEach { append(it).append('\n') }
            }
            if (upcoming.isNotEmpty()) {
                append("Upcoming:\n")
                upcoming.forEach { append(it).append('\n') }
            }
        }.trim().ifBlank { "Nothing with a due date in the next two weeks." }
    }

    /**
     * When to fire a morning-of reminder for [dueAt].
     * null = skip (too far in the past).
     */
    fun reminderTriggerMillis(
        dueAt: Long,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val dueDay = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (dueDay.isBefore(today)) return null
        val nine = dueDay.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        if (nine > now) return nine
        // Same day, already past 9:00 — nudge soon
        return now + 60_000L
    }
}
