package com.mneme.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DueDateGuessTest {
    private val today = LocalDate.of(2026, 8, 25)

    @Test
    fun ignoresBareClockTimes() {
        assertNull(DueDateGuess.guess("Meeting at 13:49 in the lab", today))
        assertNull(DueDateGuess.parseIsoOrEnglish("13:49", today))
        assertNull(DueDateGuess.parseIsoOrEnglish("9:30", today))
        assertNull(DueDateGuess.parseIsoOrEnglish("9:30 AM", today))
    }

    @Test
    fun parsesIsoDate() {
        val ms = DueDateGuess.guess("Submit by 2026-08-28 please", today)
        assertNotNull(ms)
        val d = java.time.Instant.ofEpochMilli(ms!!)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(LocalDate.of(2026, 8, 28), d)
    }

    @Test
    fun parsesMonthDayWithDueKeyword() {
        val ms = DueDateGuess.guess("Homework due Aug 28", today)
        assertNotNull(ms)
        val d = java.time.Instant.ofEpochMilli(ms!!)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(28, d.dayOfMonth)
        assertEquals(8, d.monthValue)
    }

    @Test
    fun ignoresSlashDateWithoutYearOrDueWord() {
        // Ambiguous 8/25 alone should not invent a due
        assertNull(DueDateGuess.guess("See you 8/25 maybe", today))
    }

    @Test
    fun acceptsDueNearSlashDate() {
        val ms = DueDateGuess.guess("Assignment due 28/8", today)
        assertNotNull(ms)
    }
}
