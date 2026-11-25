package com.mneme.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCryptoDeviceTest {
    @Test
    fun androidCryptoProviderEncryptsAndAuthenticatesBackupData() {
        val password = "android-device-test-passphrase".toCharArray()
        val plain = "private Retenqo backup payload".toByteArray()
        val sealed = BackupCrypto.encrypt(plain, password)

        assertArrayEquals(plain, BackupCrypto.decrypt(sealed, password))
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(sealed, "incorrect-device-test-password".toCharArray())
        }
    }
}
