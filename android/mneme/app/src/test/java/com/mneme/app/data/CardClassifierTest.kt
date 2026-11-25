package com.mneme.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CardClassifierTest {
    @Test
    fun dueWinsWhenDueAtSet() {
        assertEquals(
            CardType.DUE,
            CardClassifier.classify("Pay rent", "whenever", dueAt = 1L),
        )
    }

    @Test
    fun promiseFromIllSend() {
        assertEquals(
            CardType.PROMISE,
            CardClassifier.classify("WA", "I'll send the PDF tonight", dueAt = null),
        )
    }

    @Test
    fun waitingFromPhrase() {
        assertEquals(
            CardType.WAITING,
            CardClassifier.classify("Invoice", "Waiting on Priya for the quote", dueAt = null),
        )
    }

    @Test
    fun askForYouFromCanYou() {
        assertEquals(
            CardType.ASK_FOR_YOU,
            CardClassifier.classify("Mom", "Can you confirm the address?", dueAt = null),
        )
    }

    @Test
    fun factDefault() {
        assertEquals(
            CardType.FACT,
            CardClassifier.classify("Uber", "Trip cost was 243", dueAt = null),
        )
    }
}
