package com.mneme.app.notif

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCaptureSettingsTest {
    @Test
    fun notificationCaptureStartsPausedUntilSourcesAreSelected() {
        assertTrue(NotificationCaptureSettings.defaultEnabledSources().isEmpty())
    }

    @Test
    fun packageCaptureRequiresAnExplicitlyEnabledKnownSource() {
        assertTrue(NotificationCaptureSettings.isPackageEnabled("com.whatsapp", setOf("whatsapp")))
        assertTrue(NotificationCaptureSettings.isPackageEnabled("com.whatsapp.w4b", setOf("whatsapp")))
        assertFalse(NotificationCaptureSettings.isPackageEnabled("com.whatsapp", emptySet()))
        assertFalse(NotificationCaptureSettings.isPackageEnabled("com.unknown.app", setOf("whatsapp")))
        assertFalse(NotificationCaptureSettings.isPackageEnabled(null, setOf("whatsapp")))
    }
}
