package com.mneme.app.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mneme.app.ai.PulsePipelineHints
import com.mneme.app.data.IngestSources
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MediaScanResult(
    val scanned: Int,
    val saved: Int,
    val skipped: Int,
    val message: String,
)

/**
 * Ingest user-selected files or recent WhatsApp photos from scoped MediaStore access.
 * Ladder: image/PDF → ML Kit OCR → [PulsePipeline.ingestText] (local extraction + embed).
 * VLM fallback is deferred (OCR-first is enough for digital PDFs / clear photos).
 */
class MediaIngestPipeline(
    private val context: Context,
    private val pulse: PulsePipeline,
) {
    private val mutex = Mutex()
    private val prefs by lazy {
        context.getSharedPreferences("mneme_media_ingest", Context.MODE_PRIVATE)
    }

    fun clearHistory() {
        prefs.edit().clear().apply()
    }

    suspend fun scanWhatsAppRecent(limit: Int = 8): MediaScanResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val candidates = WhatsAppMediaPaths.listRecentCandidates(context, limit = limit)
            if (candidates.isEmpty()) {
                return@withContext MediaScanResult(
                    0, 0, 0,
                    "No recent WhatsApp photos found in shared media",
                )
            }
            var saved = 0
            var skipped = 0
            for (candidate in candidates) {
                val key = "done:${candidate.uri}:${candidate.modifiedAtSeconds}:${candidate.sizeBytes}"
                if (prefs.getBoolean(key, false)) {
                    skipped++
                    continue
                }
                val result = try {
                    ingestSelected(candidate.uri, sourceLabel = "WhatsApp photo · ${candidate.displayName}")
                } catch (e: Exception) {
                    Log.w(TAG, "media OCR failed (${e.javaClass.simpleName})")
                    PulseResult(null, "Could not read a WhatsApp photo", false)
                }
                if (result.noteId != null) {
                    saved++
                    prefs.edit().putBoolean(key, true).apply()
                } else {
                    skipped++
                    // Remember weak/unimportant so we don't loop forever
                    if (result.preview.startsWith("Skipped") ||
                        result.preview.startsWith("Nothing") ||
                        result.skippedDuplicate
                    ) {
                        prefs.edit().putBoolean(key, true).apply()
                    }
                }
            }
            val msg = when {
                saved > 0 -> "WhatsApp photos: saved $saved · skipped $skipped"
                else -> "WhatsApp photos: nothing new ($skipped checked)"
            }
            MediaScanResult(candidates.size, saved, skipped, msg)
        }
    }

    suspend fun ingestFile(file: File, sourceLabel: String = IngestSources.waMedia(file.name)): PulseResult {
        val ext = file.extension.lowercase()
        val text = when (ext) {
            "pdf" -> ocrPdf(file)
            in setOf("jpg", "jpeg", "png", "webp", "heic") -> ocrImage(file)
            else -> return PulseResult(null, "Unsupported ${file.extension}", false)
        }
        if (text.length < 12) {
            return PulseResult(null, "Skipped weak OCR: ${file.name}", false)
        }
        if (PulsePipelineHints.shouldSkipIngest(text)) {
            return PulseResult(null, "Skipped junk OCR: ${file.name}", false)
        }
        return pulse.ingestText(text, sourceLabel = sourceLabel)
    }

    /** Ingest only files the user selected through Android's document picker. */
    suspend fun ingestSelected(uri: Uri, sourceLabel: String = "Selected file"): PulseResult = withContext(Dispatchers.IO) {
        val metadata = queryMetadata(uri)
        val safeName = metadata.first
        val mime = metadata.second.orEmpty()
        val extension = safeName.substringAfterLast('.', "").lowercase()
        val resolvedExtension = when {
            extension in SUPPORTED_EXTENSIONS -> extension
            mime == "application/pdf" -> "pdf"
            mime.startsWith("image/") -> mime.substringAfter('/').lowercase()
            else -> extension
        }
        if (resolvedExtension !in SUPPORTED_EXTENSIONS) {
            return@withContext PulseResult(null, "Unsupported selected file: $safeName", false)
        }

        val temp = File.createTempFile("selected-media-", ".$resolvedExtension", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext PulseResult(null, "Could not open selected file: $safeName", false)
            input.use { source ->
                FileOutputStream(temp).use { destination ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_SELECTED_FILE_BYTES) { "Selected file exceeds the 40 MB limit" }
                        destination.write(buffer, 0, count)
                    }
                }
            }
            ingestFile(temp, sourceLabel = "$sourceLabel · $safeName").let { result ->
                if (result.noteId == null && result.preview.startsWith("Skipped")) {
                    result.copy(preview = "${result.preview} · $safeName")
                } else result
            }
        } catch (e: Exception) {
            PulseResult(null, "Could not import $safeName: ${e.message ?: "read or OCR failed"}", false)
        } finally {
            temp.delete()
        }
    }

    private fun queryMetadata(uri: Uri): Pair<String, String?> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "selected-file"
        var mime = context.contentResolver.getType(uri)
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index) ?: name
                }
            }
        return name.take(180) to mime
    }

    private suspend fun ocrImage(file: File): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight.toLong() > MAX_IMAGE_PIXELS
        ) return@withContext ""
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_DIMENSION)
        val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        })
            ?: return@withContext ""
        try {
            recognize(bmp)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    private suspend fun ocrPdf(file: File): String = withContext(Dispatchers.IO) {
        val pages = PdfPageRenderer.renderPages(context, file, maxPages = 2)
        try {
            buildString {
                for ((i, page) in pages.withIndex()) {
                    if (i > 0) append("\n\n")
                    append(recognize(page))
                }
            }.trim()
        } finally {
            pages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    companion object {
        private const val TAG = "MediaIngest"
        private const val MAX_SELECTED_FILE_BYTES = 40L * 1024 * 1024
        private const val MAX_IMAGE_PIXELS = 40_000_000L
        private const val MAX_IMAGE_DIMENSION = 2048
        private val SUPPORTED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "webp", "heic")

        private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
            var sample = 1
            while (width / sample > maxDimension || height / sample > maxDimension) {
                sample *= 2
            }
            return sample
        }
    }
}
