package com.mneme.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

data class BackupRestoreSummary(
    val notesAdded: Int,
    val historyEventsAdded: Int,
    val suggestionsAdded: Int,
)

/** User-directed JSON export and additive restore for local cards and their evidence history. */
class MnemeBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = MnemeDatabase.get(appContext)

    suspend fun export(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("exportedAt", System.currentTimeMillis())
        database.withTransaction {
            root.put("notes", JSONArray().also { rows ->
                database.notes().getAll().forEach { rows.put(it.toJson()) }
            })
            root.put("history", JSONArray().also { rows ->
                database.notes().allHistory().forEach { rows.put(it.toJson()) }
            })
            root.put("suggestions", JSONArray().also { rows ->
                database.followThrough().allSuggestions().forEach { rows.put(it.toJson()) }
            })
        }
        val plainBytes = root.toString().toByteArray(StandardCharsets.UTF_8)
        val encrypted = try {
            require(plainBytes.size <= MAX_BACKUP_BYTES) { "Backup is too large to export as one file" }
            BackupCrypto.encrypt(plainBytes, password)
        } finally {
            plainBytes.fill(0)
        }
        val bytes = JSONObject()
            .put("format", ENCRYPTED_FORMAT)
            .put("version", ENCRYPTED_VERSION)
            .put("cipher", CIPHER)
            .put("kdf", KDF)
            .put("iterations", BackupCrypto.ITERATIONS)
            .put("salt", Base64.getEncoder().encodeToString(encrypted.salt))
            .put("nonce", Base64.getEncoder().encodeToString(encrypted.nonce))
            .put("ciphertext", Base64.getEncoder().encodeToString(encrypted.ciphertext))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BACKUP_BYTES) { "Encrypted backup exceeds the 80 MB export limit" }
        val output = appContext.contentResolver.openOutputStream(uri, "wt")
            ?: error("Could not open the selected backup destination")
        output.use { it.write(bytes) }
    }

    /** Restore is additive: existing cards are retained and matched by event ID or content hash. */
    suspend fun restore(uri: Uri, password: CharArray? = null): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected backup file")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_BACKUP_BYTES) { "Backup file exceeds the 80 MB import limit" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        val envelopeText = bytes.toString(StandardCharsets.UTF_8)
        bytes.fill(0)
        val envelope = JSONObject(envelopeText)
        val root = if (envelope.optString("format") == ENCRYPTED_FORMAT) {
            require(envelope.optInt("version") == ENCRYPTED_VERSION) { "Encrypted backup version is not supported" }
            require(envelope.optString("cipher") == CIPHER && envelope.optString("kdf") == KDF) {
                "Encrypted backup uses an unsupported algorithm"
            }
            require(envelope.optInt("iterations") == BackupCrypto.ITERATIONS) {
                "Encrypted backup uses unsupported password settings"
            }
            val suppliedPassword = password ?: error("Enter the password used when this backup was exported")
            val decrypted = BackupCrypto.decrypt(
                BackupCrypto.EncryptedPayload(
                    salt = Base64.getDecoder().decode(envelope.getString("salt")),
                    nonce = Base64.getDecoder().decode(envelope.getString("nonce")),
                    ciphertext = Base64.getDecoder().decode(envelope.getString("ciphertext")),
                ),
                suppliedPassword,
            )
            require(decrypted.size <= MAX_BACKUP_BYTES) { "Backup data exceeds the 80 MB import limit" }
            try {
                JSONObject(decrypted.toString(StandardCharsets.UTF_8))
            } finally {
                decrypted.fill(0)
            }
        } else {
            // Older Retenqo JSON backups remain importable.
            envelope
        }
        require(root.optString("format") == FORMAT) { "This is not a Retenqo backup" }
        require(root.optInt("version") == FORMAT_VERSION) { "This Retenqo backup version is not supported" }
        val noteRows = root.optJSONArray("notes") ?: error("Backup has no cards section")
        val historyRows = root.optJSONArray("history") ?: JSONArray()
        val suggestionRows = root.optJSONArray("suggestions") ?: JSONArray()

        database.withTransaction {
            val noteDao = database.notes()
            val history = database.followThrough()
            val restoredIds = HashMap<Long, Long>(noteRows.length())
            var notesAdded = 0
            var historyAdded = 0
            var suggestionsAdded = 0

            for (index in 0 until noteRows.length()) {
                val row = noteRows.getJSONObject(index)
                val oldId = row.getLong("id")
                val sourceEventId = row.optString("sourceEventId").takeIf(String::isNotBlank)
                val contentHash = row.getString("contentHash")
                val existing = sourceEventId?.let { noteDao.getBySourceEventId(it) }
                    ?: noteDao.getByContentHash(contentHash)
                if (existing != null) {
                    restoredIds[oldId] = existing.id
                    continue
                }

                val note = row.toNoteEntity().copy(id = 0)
                val newId = noteDao.insert(note)
                if (newId > 0L) {
                    restoredIds[oldId] = newId
                    notesAdded++
                } else {
                    val conflict = sourceEventId?.let { noteDao.getBySourceEventId(it) }
                        ?: noteDao.getByContentHash(contentHash)
                    if (conflict != null) restoredIds[oldId] = conflict.id
                }
            }

            for (index in 0 until historyRows.length()) {
                val row = historyRows.getJSONObject(index)
                val noteId = restoredIds[row.getLong("noteId")] ?: continue
                val event = row.toHistoryEvent(noteId)
                if (noteDao.countMatchingHistoryEvent(
                        event.noteId,
                        event.eventType,
                        event.summary,
                        event.source,
                        event.evidenceText,
                        event.createdAt,
                    ) == 0
                ) {
                    noteDao.insertHistoryEvent(event)
                    historyAdded++
                }
            }

            for (index in 0 until suggestionRows.length()) {
                val row = suggestionRows.getJSONObject(index)
                val noteId = restoredIds[row.getLong("commitmentId")] ?: continue
                val suggestion = row.toSuggestion(noteId)
                if (history.insert(suggestion) > 0L) suggestionsAdded++
            }
            BackupRestoreSummary(notesAdded, historyAdded, suggestionsAdded)
        }
    }

    private fun NoteEntity.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("body", body)
        .put("source", source)
        .put("createdAt", createdAt)
        .put("dueAt", dueAt.jsonValue())
        .put("contentHash", contentHash)
        .put("embedding", embedding?.let { Base64.getEncoder().encodeToString(it) }.jsonValue())
        .put("cardType", cardType)
        .put("cardStatus", cardStatus)
        .put("who", who.jsonValue())
        .put("confidence", confidence.toDouble())
        .put("statusChangedAt", statusChangedAt.jsonValue())
        .put("snoozedUntil", snoozedUntil.jsonValue())
        .put("reviewState", reviewState)
        .put("sourceEventId", sourceEventId.jsonValue())

    private fun JSONObject.toNoteEntity(): NoteEntity {
        val type = optString("cardType", CardType.FACT.storage)
        val status = optString("cardStatus", CardStatus.OPEN.storage)
        val reviewState = optString("reviewState", CardReviewState.CONFIRMED.storage)
        val encoded = nullableString("embedding")
        return NoteEntity(
            title = getString("title").take(MAX_FIELD_CHARS),
            body = optString("body").take(MAX_FIELD_CHARS),
            source = optString("source", "Backup import").take(160),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            dueAt = nullableLong("dueAt"),
            contentHash = getString("contentHash").take(128),
            embedding = encoded?.let { Base64.getDecoder().decode(it) },
            cardType = CardType.fromStorage(type).storage,
            cardStatus = CardStatus.fromStorage(status).storage,
            who = nullableString("who")?.take(MAX_FIELD_CHARS),
            confidence = optDouble("confidence", 1.0).toFloat().coerceIn(0f, 1f),
            statusChangedAt = nullableLong("statusChangedAt"),
            snoozedUntil = nullableLong("snoozedUntil"),
            reviewState = CardReviewState.fromStorage(reviewState).storage,
            sourceEventId = nullableString("sourceEventId")?.take(128),
        )
    }

    private fun CommitmentHistoryEvent.toJson() = JSONObject()
        .put("id", id)
        .put("noteId", noteId)
        .put("eventType", eventType)
        .put("summary", summary)
        .put("source", source)
        .put("evidenceText", evidenceText.jsonValue())
        .put("createdAt", createdAt)

    private fun JSONObject.toHistoryEvent(noteId: Long) = CommitmentHistoryEvent(
        noteId = noteId,
        eventType = optString("eventType", CommitmentHistoryEvent.Type.BASELINE).take(80),
        summary = optString("summary").take(MAX_FIELD_CHARS),
        source = optString("source", "Backup import").take(160),
        evidenceText = nullableString("evidenceText")?.take(MAX_FIELD_CHARS),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
    )

    private fun FollowThroughSuggestion.toJson() = JSONObject()
        .put("id", id)
        .put("commitmentId", commitmentId)
        .put("evidenceSource", evidenceSource)
        .put("evidenceText", evidenceText)
        .put("evidenceHash", evidenceHash)
        .put("confidence", confidence.toDouble())
        .put("createdAt", createdAt)
        .put("state", state)

    private fun JSONObject.toSuggestion(commitmentId: Long) = FollowThroughSuggestion(
        commitmentId = commitmentId,
        evidenceSource = optString("evidenceSource", "Backup import").take(160),
        evidenceText = optString("evidenceText").take(MAX_FIELD_CHARS),
        evidenceHash = getString("evidenceHash").take(128),
        confidence = optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        state = when (val value = optString("state")) {
            FollowThroughSuggestion.State.ACCEPTED, FollowThroughSuggestion.State.DISMISSED -> value
            else -> FollowThroughSuggestion.State.PENDING
        },
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun Any?.jsonValue(): Any = this ?: JSONObject.NULL

    companion object {
        private const val FORMAT = "retenqo-local-backup"
        private const val FORMAT_VERSION = 1
        private const val ENCRYPTED_FORMAT = "retenqo-encrypted-backup"
        private const val ENCRYPTED_VERSION = 1
        private const val CIPHER = "AES-256-GCM"
        private const val KDF = "PBKDF2WithHmacSHA256"
        private const val MAX_BACKUP_BYTES = 80 * 1024 * 1024
        private const val MAX_FIELD_CHARS = 20_000
    }
}
