package com.mneme.app.ai

import com.mneme.app.data.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RulesExtractTest {
    private val today = LocalDate.of(2026, 8, 26)

    @Test
    fun payByWithDateIsConfidentDue() {
        val hit = RulesExtract.extract(
            "From Priya: please pay by Aug 28 the remaining ₹2,500",
            today,
        )
        assertEquals(CardType.DUE, hit.cardType)
        assertNotNull(hit.dueAt)
        assertEquals("₹2,500", hit.amountText)
        assertEquals("Priya", hit.who)
        assertFalse(hit.needsLlm)
        assertTrue(hit.confidence >= 0.7f)
    }

    @Test
    fun illSendIsPromiseWithoutLlm() {
        val hit = RulesExtract.extract("From Sam: I'll send the PDF tonight", today)
        assertEquals(CardType.PROMISE, hit.cardType)
        assertFalse(hit.needsLlm)
    }

    @Test
    fun vagueLongTextNeedsLlm() {
        val long = "Something about the project discussion and various updates from the team meeting notes without clear money or deadlines. ".repeat(4)
        val hit = RulesExtract.extract(long, today)
        assertTrue(hit.needsLlm)
        assertTrue(hit.confidence < 0.7f)
    }

    @Test
    fun noteExtractorFallbackUsesRules() {
        val n = NoteExtractor.fallback("18:54\nPart Submission Notice due Aug 28")
        assertTrue(n.title.contains("Part") || n.title.contains("Submission"))
        assertNotNull(n.dueAt)
        assertEquals(CardType.DUE.storage, n.cardType)
        assertFalse(n.usedLlm)
    }

    @Test
    fun mergePrefersRulesDue() {
        val rules = RulesExtract.extract("Pay rent by 2026-09-01 ₹8000", today)
        val llm = ExtractedNote(
            title = "Rent",
            body = "pay rent",
            dueAt = null,
            usedLlm = true,
        )
        val merged = NoteExtractor.mergeWithRules(llm, rules)
        assertNotNull(merged.dueAt)
        assertTrue(merged.body.contains("₹") || merged.body.contains("8000"))
        assertTrue(merged.usedLlm)
    }
}
