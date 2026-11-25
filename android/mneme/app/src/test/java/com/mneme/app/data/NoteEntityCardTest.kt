package com.mneme.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEntityCardTest {
    @Test
    fun openIsActive() {
        val n = NoteEntity(
            title = "t",
            body = "b",
            source = "s",
            contentHash = "h",
            cardStatus = CardStatus.OPEN.storage,
        )
        assertTrue(n.isActive(1_000L))
    }

    @Test
    fun doneNotActive() {
        val n = NoteEntity(
            title = "t",
            body = "b",
            source = "s",
            contentHash = "h",
            cardStatus = CardStatus.DONE.storage,
        )
        assertFalse(n.isActive())
    }

    @Test
    fun snoozeActiveOnlyAfterUntil() {
        val n = NoteEntity(
            title = "t",
            body = "b",
            source = "s",
            contentHash = "h",
            cardStatus = CardStatus.SNOOZED.storage,
            snoozedUntil = 5_000L,
        )
        assertFalse(n.isActive(4_000L))
        assertTrue(n.isActive(5_000L))
        assertTrue(n.isActive(6_000L))
    }
}
