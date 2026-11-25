package com.mneme.app.remind

import com.mneme.app.data.CardStatus
import com.mneme.app.data.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DueDigestTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 8, 25)

    private fun note(title: String, dueDay: LocalDate) = NoteEntity(
        id = title.hashCode().toLong(),
        title = title,
        body = title,
        source = "test",
        createdAt = 1L,
        dueAt = dueDay.atStartOfDay(zone).toInstant().toEpochMilli(),
        contentHash = title,
        embedding = null,
    )

    @Test
    fun detectsDueQueries() {
        assertTrue(DueDigest.isDueQuery("what's due today"))
        assertTrue(DueDigest.isDueQuery("due this week"))
        assertFalse(DueDigest.isDueQuery("when is chapter 11 due"))
    }

    @Test
    fun formatsOverdueTodayUpcoming() {
        val text = DueDigest.format(
            listOf(
                note("Old", today.minusDays(2)),
                note("Now", today),
                note("Later", today.plusDays(3)),
                note("Done", today).copy(cardStatus = CardStatus.DONE.storage),
            ),
            today = today,
            zone = zone,
        )
        assertTrue(text.contains("Overdue:"))
        assertTrue(text.contains("Due today:"))
        assertTrue(text.contains("Upcoming:"))
        assertTrue(text.contains("Old"))
        assertTrue(text.contains("Now"))
        assertTrue(text.contains("Later"))
        assertFalse(text.contains("Done"))
    }

    @Test
    fun emptyDigest() {
        assertEquals(
            "Nothing with a due date in the next two weeks.",
            DueDigest.format(emptyList(), today, zone),
        )
    }

    @Test
    fun reminderTriggerFutureMorning() {
        val due = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val trigger = DueDigest.reminderTriggerMillis(due, now, zone)
        assertNotNull(trigger)
        val nine = today.plusDays(2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(nine, trigger)
    }

    @Test
    fun reminderSkipsPastDays() {
        val due = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        assertNull(DueDigest.reminderTriggerMillis(due, now, zone))
    }
}
