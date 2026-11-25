package com.mneme.app.notif

import com.mneme.app.data.IngestSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NotificationConsentQueueTest(
    private val sourceId: String,
    private val packageName: String,
    private val cardSource: String,
) {
    @Test
    fun enabledSourceIsRetainedWithStableIdentity() {
        val encoded = NotifQueueLogic.encodeStable(sourceId, cardSource, "Keep this notification")

        val retained = NotificationCaptureSettings.normalizeQueueEntries(listOf(encoded), setOf(sourceId))

        assertEquals(1, retained.size)
        assertEquals(sourceId, NotifQueueLogic.decodeEntry(retained.single()).sourceId)
        assertEquals(cardSource, NotifQueueLogic.decodeEntry(retained.single()).source)
    }

    @Test
    fun disabledOrRevokedConsentRemovesPendingSource() {
        val encoded = NotifQueueLogic.encodeStable(sourceId, cardSource, "Remove after revocation")

        val retainedAfterDisable = NotificationCaptureSettings.normalizeQueueEntries(listOf(encoded), emptySet())
        val retainedAfterRevocation = NotificationCaptureSettings.normalizeQueueEntries(
            listOf(encoded),
            NotificationCaptureSettings.sources.map { it.id }.toSet() - sourceId,
        )

        assertTrue(retainedAfterDisable.isEmpty())
        assertTrue(retainedAfterRevocation.isEmpty())
    }

    @Test
    fun legacyQueueEntryMigratesToStableSourceId() {
        val legacy = NotifQueueLogic.encode(cardSource, "Migrate this legacy notification")

        val migrated = NotificationCaptureSettings.normalizeQueueEntries(
            listOf(legacy),
            setOf(sourceId),
        )

        assertEquals(sourceId, NotifQueueLogic.decodeEntry(migrated.single()).sourceId)
        assertEquals(cardSource, NotifQueueLogic.decodeEntry(migrated.single()).source)
    }

    @Test
    fun unknownSourceIsDroppedFailClosed() {
        val unknown = NotifQueueLogic.encode("Unrecognized source", "Do not process this item")

        assertTrue(NotificationCaptureSettings.normalizeQueueEntries(listOf(unknown), setOf(sourceId)).isEmpty())
    }

    @Test
    fun packageMapsToTheSameStableConsentId() {
        assertEquals(sourceId, NotificationCaptureSettings.sourceIdForPackage(packageName))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun sources(): Collection<Array<Any>> = listOf(
            arrayOf("whatsapp", "com.whatsapp", IngestSources.WA_NOTIF),
            arrayOf("gmail", "com.google.android.gm", IngestSources.GMAIL_NOTIF),
            arrayOf("messages", "com.google.android.apps.messaging", IngestSources.SMS_NOTIF),
            arrayOf("telegram", "org.telegram.messenger", IngestSources.TELEGRAM_NOTIF),
            arrayOf("outlook", "com.microsoft.office.outlook", IngestSources.OUTLOOK_NOTIF),
            arrayOf("calendar", "com.google.android.calendar", IngestSources.CALENDAR_NOTIF),
            arrayOf("slack", "com.Slack", IngestSources.SLACK_NOTIF),
        )
    }
}
