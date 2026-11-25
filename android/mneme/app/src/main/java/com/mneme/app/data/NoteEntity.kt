package com.mneme.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["contentHash"]),
        Index(value = ["createdAt"]),
        Index(value = ["cardStatus"]),
        Index(value = ["cardType"]),
        Index(value = ["dueAt"]),
        Index(value = ["reviewState"]),
        Index(value = ["sourceEventId"], unique = true),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val contentHash: String,
    /** L2-normalized float32 little-endian blob (Qwen3-Embedding-0.6B preferred; Hash fallback). */
    val embedding: ByteArray? = null,
    /** [CardType.storage] */
    val cardType: String = CardType.FACT.storage,
    /** [CardStatus.storage] */
    val cardStatus: String = CardStatus.OPEN.storage,
    /** Person or party involved, if known. */
    val who: String? = null,
    /** 0f..1f extract confidence. */
    val confidence: Float = 1f,
    val statusChangedAt: Long? = null,
    /** When snoozed, hide from active dues until this time. */
    val snoozedUntil: Long? = null,
    /** New captured commitments stay out of task flows until the user reviews them. */
    val reviewState: String = CardReviewState.CONFIRMED.storage,
    /** Hashed upstream event key when the source provides one (e.g. Android notification key). */
    val sourceEventId: String? = null,
) {
    fun type(): CardType = CardType.fromStorage(cardType)
    fun status(): CardStatus = CardStatus.fromStorage(cardStatus)
    fun review(): CardReviewState = CardReviewState.fromStorage(reviewState)

    /** True when the card should count for dues / brief (not done; snooze expired or open). */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean =
        review() == CardReviewState.CONFIRMED &&
        when (status()) {
            CardStatus.OPEN -> true
            CardStatus.SNOOZED -> snoozedUntil != null && snoozedUntil <= now
            CardStatus.DONE, CardStatus.EXPIRED -> false
        }
}
