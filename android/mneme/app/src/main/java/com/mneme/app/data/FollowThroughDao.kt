package com.mneme.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowThroughDao {
    @Query(
        """SELECT * FROM follow_through_suggestions
           WHERE state = 'pending' ORDER BY createdAt DESC""",
    )
    fun observePending(): Flow<List<FollowThroughSuggestion>>

    @Query("SELECT * FROM follow_through_suggestions ORDER BY id ASC")
    suspend fun allSuggestions(): List<FollowThroughSuggestion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(suggestion: FollowThroughSuggestion): Long

    @Query("SELECT * FROM follow_through_suggestions WHERE id = :id AND state = 'pending' LIMIT 1")
    suspend fun pendingById(id: Long): FollowThroughSuggestion?

    @Query("UPDATE notes SET cardStatus = 'done', statusChangedAt = :changedAt, snoozedUntil = NULL WHERE id = :commitmentId")
    suspend fun markCommitmentDone(commitmentId: Long, changedAt: Long): Int

    @Insert
    suspend fun insertHistoryEvent(event: CommitmentHistoryEvent): Long

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun commitmentById(id: Long): NoteEntity?

    @Query("UPDATE follow_through_suggestions SET state = :state WHERE id = :id AND state = 'pending'")
    suspend fun updatePendingSuggestion(id: Long, state: String): Int

    @Query("SELECT COUNT(*) FROM commitment_history WHERE noteId = :noteId AND eventType = :eventType AND evidenceText = :evidenceText")
    suspend fun countHistory(noteId: Long, eventType: String, evidenceText: String?): Int

    @Transaction
    suspend fun acceptAndComplete(id: Long, changedAt: Long): Long? {
        val suggestion = pendingById(id) ?: return null
        if (markCommitmentDone(suggestion.commitmentId, changedAt) == 0) return null
        if (!setSuggestionStateAndRecord(id, FollowThroughSuggestion.State.ACCEPTED, changedAt)) return null
        return suggestion.commitmentId
    }

    @Transaction
    suspend fun insertSuggestionWithHistory(suggestion: FollowThroughSuggestion): Long {
        val commitment = commitmentById(suggestion.commitmentId) ?: return -1L
        val id = insert(suggestion)
        if (id <= 0L) return id
        insertHistoryEvent(
            CommitmentHistoryEvent(
                noteId = commitment.id,
                eventType = CommitmentHistoryEvent.Type.COMPLETION_SUGGESTED,
                summary = "Retenqo found possible completion evidence; review needed.",
                source = suggestion.evidenceSource,
                evidenceText = suggestion.evidenceText,
                createdAt = suggestion.createdAt,
            ),
        )
        return id
    }

    @Transaction
    suspend fun setSuggestionStateAndRecord(id: Long, state: String, at: Long): Boolean {
        val suggestion = pendingById(id) ?: return false
        if (updatePendingSuggestion(id, state) == 0) return false
        val eventType = if (state == FollowThroughSuggestion.State.ACCEPTED) {
            CommitmentHistoryEvent.Type.COMPLETION_ACCEPTED
        } else {
            CommitmentHistoryEvent.Type.COMPLETION_DISMISSED
        }
        if (countHistory(suggestion.commitmentId, eventType, suggestion.evidenceText) == 0) {
            insertHistoryEvent(
                CommitmentHistoryEvent(
                    noteId = suggestion.commitmentId,
                    eventType = eventType,
                    summary = if (state == FollowThroughSuggestion.State.ACCEPTED)
                        "User confirmed this completion evidence; commitment marked done."
                    else "User dismissed this possible completion; commitment remains open.",
                    source = suggestion.evidenceSource,
                    evidenceText = suggestion.evidenceText,
                    createdAt = at,
                ),
            )
        }
        return true
    }

    @Transaction
    suspend fun dismissAndRecord(id: Long, dismissedAt: Long): Boolean {
        return setSuggestionStateAndRecord(id, FollowThroughSuggestion.State.DISMISSED, dismissedAt)
    }
}
