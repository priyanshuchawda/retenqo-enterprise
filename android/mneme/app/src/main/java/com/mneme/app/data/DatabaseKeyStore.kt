package com.mneme.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores a random SQLCipher passphrase encrypted by a non-exportable Android Keystore key. */
internal class DatabaseKeyStore(
    context: Context,
    private val keyFile: File = File(context.noBackupFilesDir, KEY_FILE_NAME),
    private val keyAlias: String = KEY_ALIAS,
) {
    @Synchronized
    fun getOrCreatePassphrase(allowCreate: Boolean): ByteArray {
        if (keyFile.exists()) return decryptPassphrase()
        if (!allowCreate) {
            throw DatabaseKeyUnavailableException("The database key is missing; refusing to replace it.")
        }

        val randomKey = ByteArray(PASSPHRASE_BYTES)
        SecureRandom().nextBytes(randomKey)
        val passphrase = java.util.Base64.getUrlEncoder().withoutPadding().encode(randomKey)
        randomKey.fill(0)
        try {
            encryptPassphrase(passphrase)
            return passphrase
        } catch (error: Exception) {
            passphrase.fill(0)
            throw DatabaseKeyUnavailableException("Unable to protect the database key.", error)
        }
    }

    private fun decryptPassphrase(): ByteArray {
        if (keyFile.length() !in MIN_BLOB_BYTES..MAX_BLOB_BYTES) {
            throw DatabaseKeyUnavailableException("The protected database key is invalid.")
        }
        return try {
            val bytes = keyFile.readBytes()
            val headerSize = MAGIC.size + IV_BYTES
            if (!bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
                throw DatabaseKeyUnavailableException("The protected database key has an unknown format.")
            }
            val iv = bytes.copyOfRange(MAGIC.size, headerSize)
            val ciphertext = bytes.copyOfRange(headerSize, bytes.size)
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, getKeystoreKey(createIfMissing = false), GCMParameterSpec(TAG_BITS, iv))
                doFinal(ciphertext)
            }
        } catch (error: DatabaseKeyUnavailableException) {
            throw error
        } catch (error: Exception) {
            throw DatabaseKeyUnavailableException("The database key cannot be unlocked; database data was preserved.", error)
        }
    }

    private fun encryptPassphrase(passphrase: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKeystoreKey(createIfMissing = true))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase)
        val output = ByteArrayOutputStream(MAGIC.size + iv.size + ciphertext.size).apply {
            write(MAGIC)
            write(iv)
            write(ciphertext)
        }.toByteArray()

        val parent = keyFile.parentFile ?: throw DatabaseKeyUnavailableException("Database key path is invalid.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw DatabaseKeyUnavailableException("Unable to create secure database key storage.")
        }
        val temporary = File(parent, "${keyFile.name}.pending")
        FileOutputStream(temporary).use { stream ->
            stream.write(output)
            stream.fd.sync()
        }
        if (!temporary.renameTo(keyFile)) {
            throw DatabaseKeyUnavailableException("Unable to persist the protected database key.")
        }
        output.fill(0)
        ciphertext.fill(0)
    }

    private fun getKeystoreKey(createIfMissing: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            return keyStore.getKey(keyAlias, null) as? SecretKey
                ?: throw DatabaseKeyUnavailableException("The database key protector is unavailable.")
        }
        if (!createIfMissing) {
            throw DatabaseKeyUnavailableException("The database key protector is missing.")
        }

        return try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .setUserAuthenticationRequired(false)
                        .build(),
                )
            }.generateKey()
        } catch (error: GeneralSecurityException) {
            throw DatabaseKeyUnavailableException("Unable to initialize the database key protector.", error)
        }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.mneme.app.database.key.v1"
        private const val KEY_FILE_NAME = "mneme-db-key.bin"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PASSPHRASE_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private val MAGIC = byteArrayOf(0x52, 0x54, 0x4e, 0x51, 0x01)
        private const val MIN_BLOB_BYTES = 5 + IV_BYTES + 16
        private const val MAX_BLOB_BYTES = 256
    }
}

internal class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
