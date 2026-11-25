package com.mneme.app.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/** Scoped MediaStore access to recent WhatsApp photos; never walks shared storage paths. */
object WhatsAppMediaPaths {
    data class Candidate(
        val uri: Uri,
        val displayName: String,
        val modifiedAtSeconds: Long,
        val sizeBytes: Long,
    )

    private const val WHATSAPP_IMAGES = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/"
    private const val BUSINESS_IMAGES = "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/"

    fun listRecentCandidates(
        context: Context,
        maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000,
        limit: Int = 12,
    ): List<Candidate> {
        if (limit <= 0) return emptyList()
        val cutoffSeconds = (System.currentTimeMillis() - maxAgeMs).coerceAtLeast(0) / 1000
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.RELATIVE_PATH,
        )
        val selection = """
            ${MediaStore.Images.Media.DATE_MODIFIED} >= ? AND
            ${MediaStore.Images.Media.SIZE} >= ? AND
            ${MediaStore.Images.Media.SIZE} <= ? AND
            (${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?)
        """.trimIndent()
        val args = arrayOf(
            cutoffSeconds.toString(),
            MIN_FILE_BYTES.toString(),
            MAX_FILE_BYTES.toString(),
            "$WHATSAPP_IMAGES%",
            "$BUSINESS_IMAGES%",
        )
        val candidates = ArrayList<Candidate>(limit)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext() && candidates.size < limit) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)?.take(180) ?: "WhatsApp photo"
                val modifiedAt = cursor.getLong(modifiedColumn)
                val size = cursor.getLong(sizeColumn)
                if (name.substringAfterLast('.', "").lowercase() !in IMAGE_EXTENSIONS) continue
                candidates += Candidate(
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    displayName = name,
                    modifiedAtSeconds = modifiedAt,
                    sizeBytes = size,
                )
            }
        }
        return candidates
    }

    private const val MIN_FILE_BYTES = 2_000L
    private const val MAX_FILE_BYTES = 40L * 1024 * 1024
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic")
}
