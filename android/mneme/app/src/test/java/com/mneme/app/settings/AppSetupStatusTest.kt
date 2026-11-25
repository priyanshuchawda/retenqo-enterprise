package com.mneme.app.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSetupStatusTest {
    @Test
    fun criticalRequiresNotifyAndListener() {
        val missing = AppSetupStatus(
            postNotifications = false,
            notifListener = true,
            mediaImages = true,
            microphone = false,
            exactAlarms = true,
        )
        assertTrue(missing.missingCritical)
        assertFalse(missing.readyForDailyUse)

        val ok = missing.copy(postNotifications = true)
        assertFalse(ok.missingCritical)
        assertTrue(ok.readyForDailyUse)
    }

    @Test
    fun mediaAccessRequiresPhotoPermission() {
        val s = AppSetupStatus(
            postNotifications = true,
            notifListener = true,
            mediaImages = false,
            microphone = false,
            exactAlarms = false,
        )
        assertFalse(s.canReadMedia)
        assertFalse(s.readyForDailyUse)
    }

    @Test
    fun grantedPhotoAccessAllowsMediaWorkflows() {
        val s = AppSetupStatus(
            postNotifications = true,
            notifListener = true,
            mediaImages = true,
            microphone = false,
            exactAlarms = true,
        )
        assertTrue(s.canReadMedia)
        assertTrue(s.readyForDailyUse)
    }

    @Test
    fun androidSelectedPhotosGrantCountsAsMediaAccess() {
        assertTrue(AppSetup.hasPhotoLibraryAccess(fullAccess = false, selectedPhotosAccess = true))
        assertFalse(AppSetup.hasPhotoLibraryAccess(fullAccess = false, selectedPhotosAccess = false))
    }
}
