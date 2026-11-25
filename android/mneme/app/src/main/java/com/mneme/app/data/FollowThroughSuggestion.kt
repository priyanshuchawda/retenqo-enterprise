package com.mneme.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Evidence that may close an open commitment. User confirmation is always required. */
@Entity(
    tableName = "follow_through_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["commitmentId"]),
        Index(value = ["evidenceHash"], unique = true),
        Index(value = ["state", "createdAt"]),
    ],
)
data class FollowThroughSuggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val evidenceSource: String,
    val evidenceText: String,
    val evidenceHash: String,
    val confidence: Float,
    val createdAt: Long = System.currentTimeMillis(),
    val state: String = State.PENDING,
) {
    object State {
        const val PENDING = "pending"
        const val ACCEPTED = "accepted"
        const val DISMISSED = "dismissed"
    }
}
