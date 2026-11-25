package com.mneme.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.charset.StandardCharsets

class BackupCryptoTest {
    private val password = "correct horse battery staple".toCharArray()
    private val plaintext = "captured message and private commitment history".toByteArray(StandardCharsets.UTF_8)

    @Test
    fun encryptedBackupRoundTripsAndDoesNotContainPlaintext() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        assertFalse(encrypted.ciphertext.toString(StandardCharsets.ISO_8859_1).contains("captured message"))
        assertArrayEquals(plaintext, BackupCrypto.decrypt(encrypted, password))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(encrypted, "not the right password".toCharArray())
        }
    }

    @Test
    fun modifiedCiphertextIsRejected() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        encrypted.ciphertext[0] = (encrypted.ciphertext[0].toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(encrypted, password)
        }
    }

    @Test
    fun exportRequiresAMinimumLengthPassword() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.encrypt(plaintext, "too-short".toCharArray())
        }
    }
}
