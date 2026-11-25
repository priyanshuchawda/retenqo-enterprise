package com.mneme.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Append-only record of the evidence and user decisions behind a commitment's current state. */
@Entity(
    tableName = "commitment_history",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["noteId", "createdAt"])],
)
data class CommitmentHistoryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val eventType: String,
    val summary: String,
    val source: String,
    val evidenceText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    object Type {
        const val CAPTURED = "captured"
        const val REVIEW_ACCEPTED = "review_accepted"
        const val REVIEW_DISMISSED = "review_dismissed"
        const val EDITED = "edited"
        const val COMPLETED = "completed"
        const val REOPENED = "reopened"
        const val SNOOZED = "snoozed"
        const val COMPLETION_SUGGESTED = "completion_suggested"
        const val COMPLETION_ACCEPTED = "completion_accepted"
        const val COMPLETION_DISMISSED = "completion_dismissed"
        const val BASELINE = "baseline"
    }
}
