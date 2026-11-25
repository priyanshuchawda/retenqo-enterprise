package com.mneme.app.data

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable password encryption for user-exported backups. Never persists the password or key. */
internal object BackupCrypto {
    const val ITERATIONS = 600_000
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    data class EncryptedPayload(val salt: ByteArray, val nonce: ByteArray, val ciphertext: ByteArray)

    fun encrypt(plaintext: ByteArray, password: CharArray): EncryptedPayload {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Use a backup password with at least 12 characters" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val keyBytes = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            EncryptedPayload(salt, nonce, cipher.doFinal(plaintext))
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(payload: EncryptedPayload, password: CharArray, iterations: Int = ITERATIONS): ByteArray {
        require(password.isNotEmpty()) { "Enter the password used when this backup was exported" }
        require(payload.salt.size == SALT_BYTES) { "Backup salt is invalid" }
        require(payload.nonce.size == NONCE_BYTES) { "Backup encryption metadata is invalid" }
        require(payload.ciphertext.size >= TAG_BITS / 8) { "Backup data is incomplete" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported backup password settings" }
        val keyBytes = deriveKey(password, payload.salt, iterations)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, payload.nonce))
            cipher.doFinal(payload.ciphertext)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("Wrong password or damaged backup")
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private const val MIN_PASSWORD_LENGTH = 12
    private const val MIN_ITERATIONS = 150_000
    private const val MAX_ITERATIONS = 1_000_000
}
