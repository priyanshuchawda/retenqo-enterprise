package com.mneme.app.data

import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

/** Converts an existing Room SQLite file before Room opens it; interrupted copies are recoverable. */
internal object PlaintextDatabaseMigrator {
    private const val PLAINTEXT_BACKUP_SUFFIX = ".plaintext-backup"
    private const val ENCRYPTED_PENDING_SUFFIX = ".encrypted-pending"
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII)

    fun hasPlaintextSqliteHeader(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_HEADER.size) return false
        return FileInputStream(file).use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            var read = 0
            while (read < header.size) {
                val count = input.read(header, read, header.size - read)
                if (count < 0) return@use false
                read += count
            }
            header.contentEquals(SQLITE_HEADER)
        }
    }

    fun plaintextBackup(databaseFile: File): File =
        File(databaseFile.parentFile, databaseFile.name + PLAINTEXT_BACKUP_SUFFIX)

    fun encryptIfNeeded(databaseFile: File, passphrase: ByteArray) {
        if (passphrase.isEmpty()) throw DatabaseKeyUnavailableException("The database key is empty.")
        val backup = plaintextBackup(databaseFile)
        val pending = File(databaseFile.parentFile, databaseFile.name + ENCRYPTED_PENDING_SUFFIX)
        recoverInterruptedMove(databaseFile, backup, pending, passphrase)
        if (!hasPlaintextSqliteHeader(databaseFile)) return

        if (pending.exists()) {
            val recoveredEncryptedCopy = runCatching { verifyEncryptedFile(pending, passphrase) }.isSuccess
            if (recoveredEncryptedCopy) {
                if (backup.exists()) {
                    throw IllegalStateException("A plaintext database recovery copy already exists.")
                }
                if (!databaseFile.renameTo(backup)) {
                    throw IllegalStateException("Unable to preserve the original database during encryption recovery.")
                }
                if (!pending.renameTo(databaseFile)) {
                    if (!backup.renameTo(databaseFile)) {
                        throw IllegalStateException("Encrypted database recovery failed; the original file remains preserved.")
                    }
                    throw IllegalStateException("Unable to restore the original database after encryption recovery.")
                }
                return
            }
            if (!pending.delete()) {
                throw IllegalStateException("Unable to remove an interrupted encrypted database copy.")
            }
        }
        exportToEncryptedFile(databaseFile, pending, passphrase)
        verifyEncryptedFile(pending, passphrase)
        deleteSidecar(databaseFile, "-wal")
        deleteSidecar(databaseFile, "-shm")
        if (backup.exists()) {
            throw IllegalStateException("A plaintext database recovery copy already exists.")
        }
        if (!databaseFile.renameTo(backup)) {
            throw IllegalStateException("Unable to preserve the original database during encryption.")
        }
        if (!pending.renameTo(databaseFile)) {
            if (!backup.renameTo(databaseFile)) {
                throw IllegalStateException("Database encryption needs recovery; the original file is preserved at ${backup.name}.")
            }
            throw IllegalStateException("Unable to promote the encrypted database; the original database was restored.")
        }
        deleteSidecar(backup, "-wal")
        deleteSidecar(backup, "-shm")
    }

    fun verifyEncryptedFile(databaseFile: File, passphrase: ByteArray) {
        val key = String(passphrase, StandardCharsets.UTF_8)
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            key,
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
        )
        try {
            database.rawQuery("PRAGMA integrity_check", emptyArray()).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                    throw IllegalStateException("Encrypted database integrity verification failed.")
                }
            }
        } finally {
            database.close()
        }
    }

    private fun exportToEncryptedFile(sourceFile: File, destinationFile: File, passphrase: ByteArray) {
        val key = String(passphrase, StandardCharsets.UTF_8)
        val source = SQLiteDatabase.openDatabase(
            sourceFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        var schemaVersion = 0
        var applicationId = 0
        try {
            source.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray()).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getInt(0) != 0) {
                    throw IllegalStateException("The existing database is busy and cannot be safely encrypted yet.")
                }
            }
            schemaVersion = source.version
            applicationId = source.rawQuery("PRAGMA application_id", emptyArray()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } finally {
            source.close()
        }

        // Open the destination as the keyed main database, then export the plaintext attachment
        // into it. This avoids depending on keyed attached-database metadata pragmas.
        SQLiteDatabase.openOrCreateDatabase(destinationFile, key, null, null).use { encrypted ->
            encrypted.rawExecSQL(
                "ATTACH DATABASE ${sqlLiteral(sourceFile.absolutePath)} AS plaintext KEY ''",
            )
            try {
                encrypted.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            } finally {
                encrypted.rawExecSQL("DETACH DATABASE plaintext")
            }
            encrypted.version = schemaVersion
            encrypted.rawExecSQL("PRAGMA application_id = $applicationId")
        }
    }

    private fun recoverInterruptedMove(
        databaseFile: File,
        backup: File,
        pending: File,
        passphrase: ByteArray,
    ) {
        if (databaseFile.exists() || !backup.exists()) return

        if (pending.exists()) {
            val recoveredEncryptedCopy = runCatching { verifyEncryptedFile(pending, passphrase) }.isSuccess
            if (recoveredEncryptedCopy) {
                if (!pending.renameTo(databaseFile)) {
                    throw IllegalStateException("Unable to finish the interrupted database encryption.")
                }
                return
            }
            if (!pending.delete()) {
                throw IllegalStateException("Unable to remove an incomplete encrypted database copy.")
            }
        }

        if (!backup.renameTo(databaseFile)) {
            throw IllegalStateException("Unable to restore the original database after an interrupted encryption.")
        }
    }

    private fun sqlLiteral(value: String): String = "'${value.replace("'", "''")}'"

    private fun deleteSidecar(databaseFile: File, suffix: String) {
        val sidecar = File(databaseFile.parentFile, databaseFile.name + suffix)
        if (sidecar.exists() && !sidecar.delete()) {
            throw IllegalStateException("Unable to remove a stale database journal file.")
        }
    }
}
