package com.mneme.app.ai

import com.mneme.app.data.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskAnswerGuardTest {
    private val note = NoteEntity(
        id = 1,
        title = "Lab due",
        body = "Submit by Aug 28",
        source = "Pulse",
        createdAt = 1L,
        dueAt = null,
        contentHash = "x",
        embedding = null,
    )

    @Test
    fun refusesWithoutCitations() {
        val g = AskAnswerGuard.sanitize("Something invented [1]", emptyList())
        assertEquals(AskAnswerGuard.REFUSE, g.answer)
        assertTrue(g.citations.isEmpty())
    }

    @Test
    fun refusesUncitedClaim() {
        val g = AskAnswerGuard.sanitize("The deadline is tomorrow for sure.", listOf(note))
        assertEquals(AskAnswerGuard.REFUSE, g.answer)
        assertTrue(g.citations.isEmpty())
    }

    @Test
    fun keepsGroundedAnswer() {
        val g = AskAnswerGuard.sanitize("Submit by Aug 28 [1].", listOf(note))
        assertTrue(g.answer.contains("[1]"))
        assertEquals(1, g.citations.size)
    }

    @Test
    fun refusesUnknownCitationIds() {
        val g = AskAnswerGuard.sanitize("The deadline is Saturday [999].", listOf(note))
        assertEquals(AskAnswerGuard.REFUSE, g.answer)
        assertTrue(g.citations.isEmpty())
    }

    @Test
    fun returnsOnlyCitedCards() {
        val second = note.copy(id = 2, title = "Another task")
        val g = AskAnswerGuard.sanitize("Submit by Aug 28 [1].", listOf(note, second))
        assertEquals(listOf(note), g.citations)
    }

    @Test
    fun detectsRefusePhrases() {
        assertTrue(AskAnswerGuard.looksLikeRefuse("I don't have that in your memory."))
        assertFalse(AskAnswerGuard.hasCitationMarker("no markers here"))
        assertTrue(AskAnswerGuard.hasCitationMarker("See [2] for details"))
    }
}
