package com.mneme.app.pulse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.NoteExtractor
import com.mneme.app.ai.PulsePipelineHints
import com.mneme.app.data.CardType
import com.mneme.app.data.IngestSources
import com.mneme.app.data.NoteRepository
import com.mneme.app.remind.DueReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PulseResult(
    val noteId: Long?,
    val preview: String,
    val skippedDuplicate: Boolean,
)

/**
 * Pulse: screenshot → OCR → rules-first extraction (optional local model) → Room note → recycle pixels.
 */
class PulsePipeline(
    private val context: Context,
    private val notes: NoteRepository,
    private val llm: LlmBridge? = null,
) {
    private val mutex = Mutex()
    private var lastPulseAt = 0L

    suspend fun runFromBitmap(bitmap: Bitmap, sourceLabel: String = IngestSources.PULSE): PulseResult =
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastPulseAt < 3_000L) {
                return PulseResult(null, "Pulse ignored (debounce)", skippedDuplicate = false)
            }
            lastPulseAt = now

            try {
                val text = recognize(bitmap).trim()
                return@withLock ingestTextLocked(text, sourceLabel)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    suspend fun runFromFile(file: File, sourceLabel: String = IngestSources.PULSE): PulseResult =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext PulseResult(null, "Could not read screenshot", false)
            try {
                runFromBitmap(bitmap, sourceLabel)
            } finally {
                if (file.exists()) file.delete()
            }
        }

    /**
     * Shared path for Pulse + WhatsApp media ingest (no screenshot debounce).
     */
    suspend fun ingestText(text: String, sourceLabel: String, sourceEventId: String? = null): PulseResult = mutex.withLock {
        ingestTextLocked(text, sourceLabel, sourceEventId)
    }

    private suspend fun ingestTextLocked(text: String, sourceLabel: String, sourceEventId: String? = null): PulseResult {
        val trimmed = text.trim()
        if (sourceEventId != null && notes.hasSeenSourceEvent(sourceEventId)) {
            return PulseResult(null, "Notification event already processed", skippedDuplicate = true)
        }
        // New phone context can suggest that an existing commitment was completed.
        // This only creates a review item; the user decides whether to close the task.
        notes.suggestCompletion(trimmed, sourceLabel, llm)
        if (PulsePipelineHints.shouldSkipIngest(trimmed)) {
            return PulseResult(null, "Skipped junk / UI chrome", skippedDuplicate = false)
        }
        if (!shouldSave(trimmed)) {
            return PulseResult(null, "Nothing important on screen", skippedDuplicate = false)
        }

        val extracted = NoteExtractor.extract(llm, trimmed)

        if (PulsePipelineHints.shouldSkipIngest("${extracted.title}\n${extracted.body}")) {
            return PulseResult(null, "Skipped junk / UI chrome", skippedDuplicate = false)
        }
        if (!extracted.important && extracted.kind == "chat") {
            return PulseResult(null, "Skipped casual chat", skippedDuplicate = false)
        }
        if (PulsePipelineHints.looksLikeClockOnlyTitle(extracted.title)) {
            return PulseResult(null, "Skipped junk title", skippedDuplicate = false)
        }

        val extractedType = CardType.fromStorage(extracted.cardType)
        val requiresReview = extractedType in ACTIONABLE_TYPES
        val id = notes.saveIfNew(
            title = extracted.title,
            body = extracted.body,
            source = sourceLabel,
            dueAt = extracted.dueAt,
            embedText = extracted.embedText,
            who = extracted.who,
            cardType = extractedType,
            confidence = extracted.confidence,
            requiresReview = requiresReview,
            sourceEventId = sourceEventId,
        )
        return if (id == null) {
            PulseResult(null, "Already saved recently", skippedDuplicate = true)
        } else {
            if (extracted.dueAt != null && !requiresReview) {
                DueReminderScheduler.schedule(context, id, extracted.title, extracted.dueAt)
            }
            val dueHint = when {
                extracted.dueAt == null -> ""
                requiresReview -> " · date detected"
                else -> " · due set"
            }
            val kindHint = if (extracted.kind != "other") " · ${extracted.kind}" else ""
            val llmHint = if (extracted.usedLlm) " · llm" else " · rules"
            val reviewHint = if (requiresReview) " · review before task/reminder" else ""
            PulseResult(id, "Saved: ${extracted.title}$kindHint$dueHint$reviewHint$llmHint", skippedDuplicate = false)
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    companion object {
        private val ACTIONABLE_TYPES = setOf(CardType.DUE, CardType.PROMISE, CardType.WAITING, CardType.ASK_FOR_YOU)

        fun shouldSave(text: String): Boolean = PulsePipelineHints.looksImportant(text)
    }
}
