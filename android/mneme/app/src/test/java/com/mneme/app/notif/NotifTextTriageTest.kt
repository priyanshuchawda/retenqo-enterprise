package com.mneme.app.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifTextTriageTest {
    @Test
    fun allowlistPackages() {
        assertTrue(NotifTextTriage.isAllowed("com.whatsapp"))
        assertTrue(NotifTextTriage.isAllowed("com.google.android.gm"))
        assertTrue(NotifTextTriage.isAllowed("com.google.android.apps.messaging"))
        assertTrue(NotifTextTriage.isAllowed("org.telegram.messenger"))
        assertEquals("Gmail", NotifTextTriage.sourceLabel("com.google.android.gm"))
        assertTrue(NotifTextTriage.isAllowed("com.instagram.android"))
        assertFalse(NotifTextTriage.isAllowed(null))
        assertTrue(NotifTextTriage.isWhatsApp("com.whatsapp.w4b"))
    }

    @Test
    fun dropsSystemNoiseAndOk() {
        assertNull(NotifTextTriage.candidate("WhatsApp", "Checking for new messages"))
        assertNull(NotifTextTriage.candidate("Sam", "ok"))
        assertNull(NotifTextTriage.candidate("Sam", "📷"))
        assertNull(NotifTextTriage.candidate("Bank", "Your OTP is 123456"))
    }

    @Test
    fun filtersSensitiveOneTimeCodesButKeepsOrdinaryMessages() {
        assertNull(NotifTextTriage.candidate("Bank", "Your OTP: 123456"))
        assertNull(NotifTextTriage.candidate("Service", "Verification code 884201"))
        assertNull(NotifTextTriage.candidate("Login", "Your login pin is 4219"))
        assertNotNull(NotifTextTriage.candidate("Sam", "Can you share the 6 digit number for our meeting?"))
    }

    @Test
    fun keepsDeadlineMessageWithSource() {
        val c = NotifTextTriage.candidate(
            "com.google.android.gm",
            "Prof Rao",
            "Reminder: submit the assignment by tomorrow for the lab exam",
        )
        assertNotNull(c)
        assertEquals("Gmail", c!!.sourceLabel)
        assertTrue(c.text.contains("Prof Rao"))
    }
}

class NotifQueueLogicTest {
    @Test
    fun interruptedClaimIsRecoveredBeforeNewWorkIsClaimed() {
        val interrupted = NotifQueueLogic.encodeEntries(
            listOf(
                NotifQueueLogic.encode("WA", "first"),
                NotifQueueLogic.encode("Gmail", "second"),
            ),
        )
        val pending = NotifQueueLogic.encodeEntries(listOf(NotifQueueLogic.encode("Slack", "third")))

        val claim = NotifQueueLogic.claim(pending, interrupted, limit = 2)

        assertEquals(listOf("WA" to "first", "Gmail" to "second"), claim.batch.map(NotifQueueLogic::decode))
        assertEquals(1, NotifQueueLogic.size(claim.pending))
        assertEquals(2, NotifQueueLogic.size(claim.inFlight))
    }

    @Test
    fun offerDrainRoundTripWithSource() {
        var json = "[]"
        json = NotifQueueLogic.offer(json, "WA", "a")
        json = NotifQueueLogic.offer(json, "Gmail", "b")
        json = NotifQueueLogic.offer(json, "Gmail", "b") // dedupe tail
        assertEquals(2, NotifQueueLogic.size(json))
        val (batch, rest) = NotifQueueLogic.drain(json, 1)
        assertEquals(1, batch.size)
        assertEquals("WA" to "a", NotifQueueLogic.decode(batch[0]))
        assertEquals(1, NotifQueueLogic.size(rest))
        val (batch2, rest2) = NotifQueueLogic.drain(rest, 5)
        assertEquals("Gmail" to "b", NotifQueueLogic.decode(batch2[0]))
        assertEquals(0, NotifQueueLogic.size(rest2))
    }

    @Test
    fun capsMax() {
        var json = "[]"
        for (i in 1..20) json = NotifQueueLogic.offer(json, "WA", "m$i", max = 3)
        assertEquals(3, NotifQueueLogic.size(json))
        val (batch, _) = NotifQueueLogic.drain(json, 10)
        assertEquals(
            listOf("WA" to "m18", "WA" to "m19", "WA" to "m20"),
            batch.map { NotifQueueLogic.decode(it) },
        )
    }

    @Test
    fun dropsDuplicateBodyAcrossSources() {
        val once = NotifQueueLogic.offer("[]", "WA notif", "Pay ₹500 by Friday")
        val twice = NotifQueueLogic.offer(once, "Pulse", "pay  ₹500  by friday")
        assertEquals(1, NotifQueueLogic.size(twice))
        assertTrue(twice.contains("WA notif"))
        assertFalse(twice.contains("Pulse"))
    }

    @Test
    fun allowsDifferentBodies() {
        var q = NotifQueueLogic.offer("[]", "WA notif", "Pay ₹500")
        q = NotifQueueLogic.offer(q, "WA notif", "Meeting at 5")
        assertEquals(2, NotifQueueLogic.size(q))
    }
}
