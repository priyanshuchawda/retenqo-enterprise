package com.mneme.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [NoteEntity::class, FollowThroughSuggestion::class, CommitmentHistoryEvent::class], version = 7, exportSchema = true)
abstract class MnemeDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
    abstract fun followThrough(): FollowThroughDao

    /** SQLCipher retains this array to authenticate each connection opened by its WAL pool. */
    private var retainedCipherPassphrase: ByteArray? = null

    @Synchronized
    private fun retainCipherPassphrase(passphrase: ByteArray) {
        check(retainedCipherPassphrase == null) { "Database passphrase is already initialized." }
        retainedCipherPassphrase = passphrase
    }

    override fun close() {
        try {
            super.close()
        } finally {
            synchronized(this) {
                retainedCipherPassphrase?.fill(0)
                retainedCipherPassphrase = null
            }
        }
    }

    companion object {
        @Volatile private var instance: MnemeDatabase? = null
        private const val DATABASE_NAME = "mneme.db"

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN cardType TEXT NOT NULL DEFAULT 'fact'")
                db.execSQL("ALTER TABLE notes ADD COLUMN cardStatus TEXT NOT NULL DEFAULT 'open'")
                db.execSQL("ALTER TABLE notes ADD COLUMN who TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE notes ADD COLUMN statusChangedAt INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozedUntil INTEGER")
                // Existing rows with a due date become due cards
                db.execSQL("UPDATE notes SET cardType = 'due' WHERE dueAt IS NOT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_cardStatus ON notes(cardStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_cardType ON notes(cardType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_dueAt ON notes(dueAt)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS follow_through_suggestions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        commitmentId INTEGER NOT NULL,
                        evidenceSource TEXT NOT NULL,
                        evidenceText TEXT NOT NULL,
                        evidenceHash TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        state TEXT NOT NULL,
                        FOREIGN KEY(commitmentId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_through_suggestions_commitmentId ON follow_through_suggestions(commitmentId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_follow_through_suggestions_evidenceHash ON follow_through_suggestions(evidenceHash)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_through_suggestions_state_createdAt ON follow_through_suggestions(state, createdAt)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reviewState TEXT NOT NULL DEFAULT 'confirmed'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_reviewState ON notes(reviewState)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS commitment_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noteId INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        source TEXT NOT NULL,
                        evidenceText TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_commitment_history_noteId_createdAt ON commitment_history(noteId, createdAt)")
                // Preserve a truthful baseline for old cards without pretending past decisions were recorded.
                db.execSQL(
                    """INSERT INTO commitment_history(noteId, eventType, summary, source, evidenceText, createdAt)
                       SELECT id, 'baseline', 'History starts here; this card already existed.', source, substr(body, 1, 1200), createdAt
                       FROM notes""".trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN sourceEventId TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notes_sourceEventId ON notes(sourceEventId)")
            }
        }

        fun get(context: Context): MnemeDatabase = instance ?: synchronized(this) {
            instance ?: createAndOpen(context.applicationContext).also { instance = it }
        }

        private fun createAndOpen(context: Context): MnemeDatabase {
            System.loadLibrary("sqlcipher")
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            val allowKeyCreation = !databaseFile.exists() || databaseFile.length() == 0L ||
                PlaintextDatabaseMigrator.hasPlaintextSqliteHeader(databaseFile)
            val passphrase = DatabaseKeyStore(context).getOrCreatePassphrase(allowKeyCreation)
            try {
                PlaintextDatabaseMigrator.encryptIfNeeded(databaseFile, passphrase)
                val roomPassphrase = passphrase.copyOf()
                var database: MnemeDatabase? = null
                try {
                    database = Room.databaseBuilder(context, MnemeDatabase::class.java, DATABASE_NAME)
                        .openHelperFactory(SupportOpenHelperFactory(roomPassphrase, null, true))
                        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                        .addCallback(object : Callback() {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                db.query("PRAGMA secure_delete = ON").use { it.moveToFirst() }
                                removePlaintextBackup(databaseFile)
                            }
                        })
                        .build()
                    database.retainCipherPassphrase(roomPassphrase)
                    database.openHelper.writableDatabase
                    return database
                } catch (error: Exception) {
                    if (database == null) {
                        roomPassphrase.fill(0)
                    } else {
                        runCatching { database.close() }
                    }
                    throw error
                }
            } finally {
                passphrase.fill(0)
            }
        }

        private fun removePlaintextBackup(databaseFile: java.io.File) {
            val backup = PlaintextDatabaseMigrator.plaintextBackup(databaseFile)
            listOf(backup, java.io.File(backup.parentFile, backup.name + "-wal"), java.io.File(backup.parentFile, backup.name + "-shm"))
                .forEach { file ->
                    if (file.exists() && !file.delete()) {
                        throw IllegalStateException("Unable to remove the plaintext database migration backup.")
                    }
                }
        }
    }
}
