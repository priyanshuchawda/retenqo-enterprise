package com.mneme.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteExtractorTest {
    @Test
    fun fallbackSkipsClockTitle() {
        val n = NoteExtractor.fallback("18:54\nPart Submission Notice due Aug 28")
        assertTrue(n.title.contains("Part") || n.title.contains("Submission"))
        assertNull(DueDateGuess.parseIsoOrEnglish("18:54"))
    }

    @Test
    fun parseJsonHonorsNullDueAndDoesNotUseClock() {
        val raw = """
            {"title":"Chat","kind":"chat","body":"See you at 9:30","due_date":null,"important":true,"embed_text":"meet at 9:30"}
        """.trimIndent()
        val n = NoteExtractor.parseJson(raw, "See you at 9:30 in the lobby")
        assertNull(n.dueAt)
        assertEquals("Chat", n.title)
    }

    @Test
    fun parseJsonAcceptsIsoDue() {
        val raw = """
            {"title":"Notice","kind":"deadline","body":"Submit lab","due_date":"2026-08-28","important":true,"embed_text":"lab due"}
        """.trimIndent()
        val n = NoteExtractor.parseJson(raw, "Submit lab by 2026-08-28")
        assertTrue(n.dueAt != null)
    }
}
