package com.mneme.app.data

import android.database.sqlite.SQLiteDatabase as AndroidSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = UUID.randomUUID().toString().replace("-", "")

    @Test
    fun keystoreWrappedPassphraseSurvivesStoreRecreation() {
        val keyFile = File(context.noBackupFilesDir, "database-key-$id.bin")
        val alias = "retenqo.test.$id"
        try {
            val first = DatabaseKeyStore(context, keyFile, alias)
                .getOrCreatePassphrase(allowCreate = true)
            val storedBlob = keyFile.readBytes()
            val second = DatabaseKeyStore(context, keyFile, alias)
                .getOrCreatePassphrase(allowCreate = false)

            assertArrayEquals(first, second)
            assertFalse(storedBlob.toString(StandardCharsets.ISO_8859_1).contains(String(first, StandardCharsets.UTF_8)))
        } finally {
            keyFile.delete()
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(alias)) deleteEntry(alias)
            }
        }
    }

    @Test
    fun plaintextDatabaseIsEncryptedWithoutLosingRows() {
        System.loadLibrary("sqlcipher")
        val databaseFile = File(context.cacheDir, "legacy-$id.db")
        val passphrase = "test-only-$id".toByteArray(StandardCharsets.UTF_8)
        try {
            AndroidSQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
                database.execSQL("CREATE TABLE private_rows (body TEXT NOT NULL)")
                database.execSQL("INSERT INTO private_rows(body) VALUES (?)", arrayOf("preserve this row"))
            }

            PlaintextDatabaseMigrator.encryptIfNeeded(databaseFile, passphrase)

            assertFalse(PlaintextDatabaseMigrator.hasPlaintextSqliteHeader(databaseFile))
            SQLCipherDatabase.openDatabase(
                databaseFile.absolutePath,
                String(passphrase, StandardCharsets.UTF_8),
                null,
                SQLCipherDatabase.OPEN_READWRITE,
                null,
            ).use { encrypted ->
                encrypted.rawQuery("SELECT body FROM private_rows", emptyArray()).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertTrue(cursor.getString(0) == "preserve this row")
                }
            }
            val wrongKeyRejected = runCatching {
                SQLCipherDatabase.openDatabase(
                    databaseFile.absolutePath,
                    "wrong-key-$id",
                    null,
                    SQLCipherDatabase.OPEN_READWRITE,
                    null,
                ).use { encrypted ->
                    encrypted.rawQuery("SELECT body FROM private_rows", emptyArray()).use { it.count }
                }
            }.isFailure
            assertTrue(wrongKeyRejected)
            val backup = PlaintextDatabaseMigrator.plaintextBackup(databaseFile)
            assertTrue(backup.isFile)
            assertTrue(PlaintextDatabaseMigrator.hasPlaintextSqliteHeader(backup))
        } finally {
            passphrase.fill(0)
            databaseFile.delete()
            File("${databaseFile.path}-wal").delete()
            File("${databaseFile.path}-shm").delete()
            File("${databaseFile.path}.plaintext-backup").delete()
            File("${databaseFile.path}.encrypted-pending").delete()
        }
    }

    @Test
    fun missingWrappedKeyIsNotSilentlyReplacedWhenDatabaseExists() {
        val databaseFile = File(context.cacheDir, "locked-$id.db")
        val keyFile = File(context.noBackupFilesDir, "missing-key-$id.bin")
        val alias = "retenqo.test.missing.$id"
        try {
            AndroidSQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
                database.execSQL("CREATE TABLE private_rows (body TEXT NOT NULL)")
                database.execSQL("INSERT INTO private_rows(body) VALUES ('keep me')")
            }

            val failure = runCatching {
                DatabaseKeyStore(context, keyFile, alias).getOrCreatePassphrase(allowCreate = false)
            }.exceptionOrNull()

            assertTrue(failure is DatabaseKeyUnavailableException)
            assertTrue(PlaintextDatabaseMigrator.hasPlaintextSqliteHeader(databaseFile))
        } finally {
            databaseFile.delete()
            keyFile.delete()
        }
    }
}
