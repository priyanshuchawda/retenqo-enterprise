package com.mneme.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IngestSourcesTest {
    @Test
    fun notifLabelsNormalize() {
        assertEquals(IngestSources.WA_NOTIF, IngestSources.fromNotifLabel("WA"))
        assertEquals(IngestSources.GMAIL_NOTIF, IngestSources.fromNotifLabel("Gmail"))
        assertEquals("Teams notif", IngestSources.fromNotifLabel("Teams"))
    }

    @Test
    fun fingerprintIgnoresWhitespaceAndCase() {
        val a = IngestSources.contentFingerprint("Pay Rahul", "₹500 by Friday")
        val b = IngestSources.contentFingerprint("pay  rahul", "  ₹500   by Friday  ")
        assertEquals(a, b)
    }

    @Test
    fun fingerprintDiffersOnContent() {
        val a = IngestSources.contentFingerprint("Pay Rahul", "₹500")
        val b = IngestSources.contentFingerprint("Pay Rahul", "₹600")
        assertNotEquals(a, b)
    }

    @Test
    fun waMediaLabel() {
        assertEquals(
            "WA media · IMG_001.jpg",
            IngestSources.waMedia("IMG_001.jpg"),
        )
    }
}
