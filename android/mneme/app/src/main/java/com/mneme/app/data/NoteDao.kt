package com.mneme.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        ORDER BY
          CASE reviewState WHEN 'pending' THEN 0 ELSE 1 END,
          CASE cardStatus
            WHEN 'open' THEN 0
            WHEN 'snoozed' THEN 1
            WHEN 'done' THEN 2
            ELSE 3
          END,
          createdAt DESC
        """
    )
    fun observeAll(): Flow<List<NoteEntity>>

    @Query(
        """SELECT COUNT(*) FROM notes
           WHERE reviewState = 'confirmed'
             AND cardStatus IN ('open', 'snoozed')
             AND (cardStatus = 'open' OR (snoozedUntil IS NOT NULL AND snoozedUntil <= :nowMs))""",
    )
    suspend fun activeWidgetCount(nowMs: Long): Int

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE contentHash = :contentHash ORDER BY createdAt ASC LIMIT 1")
    suspend fun getByContentHash(contentHash: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE sourceEventId = :sourceEventId LIMIT 1")
    suspend fun getBySourceEventId(sourceEventId: String): NoteEntity?

    @Query(
        """
        SELECT * FROM notes
        WHERE body LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%' OR IFNULL(who, '') LIKE '%' || :q || '%'
        ORDER BY createdAt DESC
        LIMIT 50
        """
    )
    suspend fun search(q: String): List<NoteEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notes
        WHERE contentHash = :hash AND createdAt > :since
        """
    )
    suspend fun countRecentHash(hash: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE sourceEventId = :sourceEventId")
    suspend fun countSourceEventId(sourceEventId: String): Int

    @Query(
        """
        SELECT * FROM notes
        WHERE dueAt IS NOT NULL
          AND dueAt >= :startMs AND dueAt <= :endMs
          AND cardStatus IN ('open', 'snoozed')
          AND reviewState = 'confirmed'
          AND (cardStatus = 'open' OR snoozedUntil IS NULL OR snoozedUntil <= :nowMs)
        ORDER BY dueAt ASC
        """
    )
    suspend fun dueBetween(startMs: Long, endMs: Long, nowMs: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: NoteEntity): Long

    @Transaction
    suspend fun insertNoteWithHistory(note: NoteEntity, event: CommitmentHistoryEvent): Long {
        val id = insert(note)
        if (id > 0L) insertHistoryEvent(event.copy(noteId = id))
        return id
    }

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        UPDATE notes SET
          cardStatus = :status,
          statusChangedAt = :changedAt,
          snoozedUntil = :snoozedUntil
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: Long,
        status: String,
        changedAt: Long,
        snoozedUntil: Long?,
    )

    @Query(
        """
        UPDATE notes SET
          title = :title,
          body = :body,
          who = :who,
          dueAt = :dueAt,
          cardType = :cardType,
          contentHash = :contentHash,
          embedding = :embedding
        WHERE id = :id
        """
    )
    suspend fun updateCardFields(
        id: Long,
        title: String,
        body: String,
        who: String?,
        dueAt: Long?,
        cardType: String,
        contentHash: String,
        embedding: ByteArray?,
    )

    @Query("UPDATE notes SET reviewState = :reviewState WHERE id = :id AND reviewState = 'pending'")
    suspend fun updateReviewState(id: Long, reviewState: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistoryEvent(event: CommitmentHistoryEvent): Long

    @Query("SELECT * FROM commitment_history WHERE noteId = :noteId ORDER BY createdAt DESC, id DESC")
    fun observeHistory(noteId: Long): Flow<List<CommitmentHistoryEvent>>

    @Query("SELECT * FROM commitment_history WHERE noteId = :noteId ORDER BY createdAt DESC, id DESC LIMIT 100")
    suspend fun recentHistory(noteId: Long): List<CommitmentHistoryEvent>

    @Query("SELECT * FROM commitment_history ORDER BY id ASC")
    suspend fun allHistory(): List<CommitmentHistoryEvent>

    @Query(
        """SELECT COUNT(*) FROM commitment_history
           WHERE noteId = :noteId AND eventType = :eventType AND summary = :summary
             AND source = :source AND evidenceText IS :evidenceText AND createdAt = :createdAt""",
    )
    suspend fun countMatchingHistoryEvent(
        noteId: Long,
        eventType: String,
        summary: String,
        source: String,
        evidenceText: String?,
        createdAt: Long,
    ): Int

    @Transaction
    suspend fun updateStatusWithHistory(
        id: Long,
        status: String,
        changedAt: Long,
        snoozedUntil: Long?,
        event: CommitmentHistoryEvent,
    ) {
        updateStatus(id, status, changedAt, snoozedUntil)
        insertHistoryEvent(event)
    }

    @Transaction
    suspend fun updateReviewWithHistory(id: Long, reviewState: String, event: CommitmentHistoryEvent): Int {
        val changed = updateReviewState(id, reviewState)
        if (changed > 0) insertHistoryEvent(event)
        return changed
    }

    @Transaction
    suspend fun updateCardFieldsWithHistory(
        id: Long,
        title: String,
        body: String,
        who: String?,
        dueAt: Long?,
        cardType: String,
        contentHash: String,
        embedding: ByteArray?,
        event: CommitmentHistoryEvent,
    ) {
        updateCardFields(id, title, body, who, dueAt, cardType, contentHash, embedding)
        insertHistoryEvent(event)
    }
}
