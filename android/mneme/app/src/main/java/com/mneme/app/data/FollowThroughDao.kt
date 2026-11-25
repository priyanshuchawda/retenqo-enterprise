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

    @Query("UPDATE follow_through_suggestions SET state = :state WHERE id = :id AND state = 'pending'")
    suspend fun updateState(id: Long, state: String): Int

    @Query("SELECT * FROM follow_through_suggestions WHERE id = :id AND state = 'pending' LIMIT 1")
    suspend fun pendingById(id: Long): FollowThroughSuggestion?

    @Query("UPDATE notes SET cardStatus = 'done', statusChangedAt = :changedAt, snoozedUntil = NULL WHERE id = :commitmentId")
    suspend fun markCommitmentDone(commitmentId: Long, changedAt: Long): Int

    @Insert
    suspend fun insertHistoryEvent(event: CommitmentHistoryEvent): Long

    @Transaction
    suspend fun acceptAndComplete(id: Long, changedAt: Long): Long? {
        val suggestion = pendingById(id) ?: return null
        if (markCommitmentDone(suggestion.commitmentId, changedAt) == 0) return null
        if (updateState(id, FollowThroughSuggestion.State.ACCEPTED) == 0) return null
        insertHistoryEvent(
            CommitmentHistoryEvent(
                noteId = suggestion.commitmentId,
                eventType = CommitmentHistoryEvent.Type.COMPLETION_ACCEPTED,
                summary = "User confirmed this completion evidence; commitment marked done.",
                source = suggestion.evidenceSource,
                evidenceText = suggestion.evidenceText,
                createdAt = changedAt,
            ),
        )
        return suggestion.commitmentId
    }

    @Transaction
    suspend fun dismissAndRecord(id: Long, dismissedAt: Long): Boolean {
        val suggestion = pendingById(id) ?: return false
        if (updateState(id, FollowThroughSuggestion.State.DISMISSED) == 0) return false
        insertHistoryEvent(
            CommitmentHistoryEvent(
                noteId = suggestion.commitmentId,
                eventType = CommitmentHistoryEvent.Type.COMPLETION_DISMISSED,
                summary = "User dismissed this possible completion; commitment remains open.",
                source = suggestion.evidenceSource,
                evidenceText = suggestion.evidenceText,
                createdAt = dismissedAt,
            ),
        )
        return true
    }
}
