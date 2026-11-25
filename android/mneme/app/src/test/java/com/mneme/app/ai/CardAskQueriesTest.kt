package com.mneme.app.ai

import com.mneme.app.data.CardStatus
import com.mneme.app.data.CardType
import com.mneme.app.data.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardAskQueriesTest {
    @Test
    fun detectsIntents() {
        assertEquals(CardAskQueries.Intent.OWE, CardAskQueries.intent("what do I owe?"))
        assertEquals(CardAskQueries.Intent.WAITING, CardAskQueries.intent("who am I waiting on"))
        assertEquals(CardAskQueries.Intent.PROMISES, CardAskQueries.intent("my promises"))
        assertEquals(null, CardAskQueries.intent("when is the lab"))
    }

    @Test
    fun oweListsDueAndMoneyCardsNotDone() {
        val notes = listOf(
            note(1, "Pay Rahul ₹500", CardType.DUE, who = "Rahul"),
            note(2, "Random fact", CardType.FACT),
            note(3, "Old fee", CardType.DUE, status = CardStatus.DONE),
            note(4, "Refund pending", CardType.FACT, body = "owe me refund INR 200"),
        )
        val r = CardAskQueries.answerFromCards(CardAskQueries.Intent.OWE, notes)
        assertEquals(2, r.citations.size)
        assertTrue(r.answer.contains("[1]"))
        assertTrue(r.answer.contains("Pay Rahul"))
        assertFalse(r.citations.any { it.title == "Old fee" })
    }

    @Test
    fun waitingRefuseWhenEmpty() {
        val r = CardAskQueries.answerFromCards(
            CardAskQueries.Intent.WAITING,
            listOf(note(1, "Fact", CardType.FACT)),
        )
        assertEquals(AskAnswerGuard.REFUSE, r.answer)
        assertTrue(r.citations.isEmpty())
    }

    @Test
    fun formatIncludesTypeWhoDue() {
        val ctx = CardAskQueries.formatCardContext(
            listOf(note(1, "Fee", CardType.DUE, who = "Office", due = 1_700_000_000_000L)),
        )
        assertTrue(ctx.contains("type=due"))
        assertTrue(ctx.contains("who=Office"))
        assertTrue(ctx.contains("[1]"))
    }

    @Test
    fun boostTypesForDeadlineQuestion() {
        assertTrue(CardType.DUE in CardAskQueries.boostTypes("assignment deadline Friday"))
    }

    private fun note(
        id: Long,
        title: String,
        type: CardType,
        who: String? = null,
        body: String = title,
        status: CardStatus = CardStatus.OPEN,
        due: Long? = null,
    ) = NoteEntity(
        id = id,
        title = title,
        body = body,
        source = "Test",
        createdAt = 1L,
        dueAt = due,
        contentHash = "h$id",
        cardType = type.storage,
        cardStatus = status.storage,
        who = who,
    )
}
