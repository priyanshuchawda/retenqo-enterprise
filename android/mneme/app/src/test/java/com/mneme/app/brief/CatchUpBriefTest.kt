package com.mneme.app.brief

import com.mneme.app.data.CardStatus
import com.mneme.app.data.CardType
import com.mneme.app.data.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class CatchUpBriefTest {
    private val now = 1_700_000_000_000L

    @Test
    fun emptyWhenNothingRelevant() {
        val brief = CatchUpBriefBuilder.build(emptyList(), lastOpenAt = now - 60_000, now = now)
        assertTrue(brief.isEmpty)
        assertEquals("You're caught up", brief.headline)
    }

    @Test
    fun picksNewDuesAndStaleWaiting() {
        val notes = listOf(
            note(1, "Pay Rahul", CardType.DUE, created = now - 10_000, due = now + TimeUnit.HOURS.toMillis(12)),
            note(2, "Old chat", CardType.FACT, created = now - TimeUnit.DAYS.toMillis(5)),
            note(3, "Waiting marks", CardType.WAITING, created = now - TimeUnit.DAYS.toMillis(5)),
            note(4, "Done fee", CardType.DUE, created = now - 5_000, due = now + 3_600_000, status = CardStatus.DONE),
        )
        val brief = CatchUpBriefBuilder.build(notes, lastOpenAt = now - 60_000, now = now)
        assertEquals(1, brief.newCards.size)
        assertEquals("Pay Rahul", brief.newCards[0].title)
        assertEquals(1, brief.duesSoon.size)
        assertEquals(1, brief.staleWaiting.size)
        assertFalse(brief.isEmpty)
        assertTrue(brief.headline.contains("due"))
        val body = CatchUpBriefBuilder.formatBody(brief)
        assertTrue(body.contains("Pay Rahul"))
        assertTrue(body.contains("Waiting marks"))
    }

    @Test
    fun firstOpenUses24hLookback() {
        val recent = note(1, "Recent", CardType.FACT, created = now - TimeUnit.HOURS.toMillis(2))
        val old = note(2, "Old", CardType.FACT, created = now - TimeUnit.DAYS.toMillis(3))
        val brief = CatchUpBriefBuilder.build(listOf(recent, old), lastOpenAt = 0L, now = now)
        assertEquals(listOf("Recent"), brief.newCards.map { it.title })
    }

    private fun note(
        id: Long,
        title: String,
        type: CardType,
        created: Long,
        due: Long? = null,
        status: CardStatus = CardStatus.OPEN,
    ) = NoteEntity(
        id = id,
        title = title,
        body = title,
        source = "Test",
        createdAt = created,
        dueAt = due,
        contentHash = "h$id",
        cardType = type.storage,
        cardStatus = status.storage,
    )
}
