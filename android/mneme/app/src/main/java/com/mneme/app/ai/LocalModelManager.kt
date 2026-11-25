package com.mneme.app.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.os.StatFs
import androidx.core.content.edit
import com.arm.aichat.gguf.GgufMetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

data class ImportedLocalModel(
    val displayName: String,
    val sizeBytes: Long,
    val sha256: String,
    val file: File,
)

/** Imports a user-selected GGUF into app-scoped storage. It never downloads model data. */
class LocalModelManager(context: Context) {
    private val appContext = context.applicationContext

    /** Downloads the pinned small Qwen GGUF over HTTPS; inference itself remains local. */
    suspend fun downloadRecommended(onProgress: (Long, Long) -> Unit): ImportedLocalModel =
        withContext(Dispatchers.IO) {
            val destination = ModelPaths.llmGguf(appContext)
            val directory = requireNotNull(destination.parentFile) { "Model storage is unavailable" }
            require(directory.mkdirs() || directory.isDirectory) { "Could not create model storage" }
            val available = StatFs(directory.absolutePath).availableBytes
            require(available >= RECOMMENDED_MODEL_BYTES + MIN_FREE_SPACE_BYTES) {
                "Need at least ${formatMb(RECOMMENDED_MODEL_BYTES + MIN_FREE_SPACE_BYTES)} MB free to download safely"
            }

            val temporary = File(directory, "active.gguf.${System.nanoTime()}.partial")
            val connection = (URL(RECOMMENDED_MODEL_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 45_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Retenqo-Android/0.1")
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                val responseCode = connection.responseCode
                require(responseCode == HttpURLConnection.HTTP_OK) {
                    "Model download failed (HTTP $responseCode)"
                }
                val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: RECOMMENDED_MODEL_BYTES
                require(contentLength <= available - MIN_FREE_SPACE_BYTES) {
                    "Not enough free storage to finish model download"
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                connection.inputStream.use { source ->
                    FileOutputStream(temporary).use { sink ->
                        val buffer = ByteArray(COPY_BUFFER_BYTES)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = source.read(buffer)
                            if (count < 0) break
                            require(StatFs(directory.absolutePath).availableBytes >= MIN_FREE_SPACE_BYTES + count) {
                                "Not enough free storage to finish model download"
                            }
                            copied += count
                            digest.update(buffer, 0, count)
                            sink.write(buffer, 0, count)
                            onProgress(copied, contentLength)
                        }
                        sink.fd.sync()
                    }
                }
                require(copied == RECOMMENDED_MODEL_BYTES) {
                    "Model download size mismatch: expected $RECOMMENDED_MODEL_BYTES bytes, received $copied"
                }
                val sha256 = digest.digest().toHex()
                require(sha256 == RECOMMENDED_MODEL_SHA256) {
                    "Downloaded model checksum did not match the published file"
                }
                require(GgufMetadataReader.create().ensureSourceFileFormat(temporary)) {
                    "Downloaded model is not a supported GGUF file"
                }
                replaceAtomically(temporary, destination)
                saveModelMetadata(RECOMMENDED_MODEL_NAME, copied, sha256)
                ImportedLocalModel(RECOMMENDED_MODEL_NAME, copied, sha256, destination)
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }

    suspend fun import(uri: Uri): ImportedLocalModel = withContext(Dispatchers.IO) {
        val destination = ModelPaths.llmGguf(appContext)
        val directory = requireNotNull(destination.parentFile) { "Model storage is unavailable" }
        require(directory.mkdirs() || directory.isDirectory) { "Could not create private model storage" }

        val (displayName, advertisedSize) = queryDocument(uri)
        val reserve = MIN_FREE_SPACE_BYTES
        val available = StatFs(directory.absolutePath).availableBytes
        if (advertisedSize != null) {
            require(advertisedSize > 0L) { "The selected model file is empty" }
            require(advertisedSize <= available - reserve) {
                "Not enough free storage to install this model safely"
            }
        }

        val temporary = File(directory, "active.gguf.${System.nanoTime()}.partial")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            val input = requireNotNull(appContext.contentResolver.openInputStream(uri)) {
                "Could not read the selected document"
            }
            var copied = 0L
            input.use { source ->
                FileOutputStream(temporary).use { sink ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(StatFs(directory.absolutePath).availableBytes >= reserve + count) {
                            "Not enough free storage to finish model import"
                        }
                        digest.update(buffer, 0, count)
                        sink.write(buffer, 0, count)
                    }
                    sink.fd.sync()
                }
            }
            require(copied > MIN_MODEL_BYTES) { "The selected file is too small to be a language model" }
            require(advertisedSize == null || advertisedSize == copied) {
                "The model copy was incomplete; select the file again"
            }
            require(GgufMetadataReader.create().ensureSourceFileFormat(temporary)) {
                "The selected file is not a supported GGUF model"
            }

            replaceAtomically(temporary, destination)
            val hash = digest.digest().toHex()
            saveModelMetadata(displayName, copied, hash)
            ImportedLocalModel(displayName, copied, hash, destination)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    fun installedModel(): ImportedLocalModel? {
        val file = ModelPaths.llmGguf(appContext)
        if (!file.isFile || file.length() <= MIN_MODEL_BYTES) return null
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ImportedLocalModel(
            displayName = prefs.getString(KEY_DISPLAY_NAME, file.name) ?: file.name,
            sizeBytes = file.length(),
            sha256 = prefs.getString(KEY_SHA256, "").orEmpty(),
            file = file,
        )
    }

    private fun queryDocument(uri: Uri): Pair<String, Long?> {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .takeIf { it >= 0 }
                        ?.let(cursor::getString)
                        ?.takeIf { it.isNotBlank() }
                        ?: "local-model.gguf"
                    val size = cursor.getColumnIndex(OpenableColumns.SIZE)
                        .takeIf { it >= 0 }
                        ?.let(cursor::getLong)
                        ?.takeIf { it > 0L }
                    return name to size
                }
            }
        return "local-model.gguf" to null
    }

    private fun replaceAtomically(source: File, destination: File) {
        val backup = File(destination.parentFile, "active.gguf.previous")
        if (!destination.exists() && backup.exists()) backup.renameTo(destination)
        if (backup.exists()) backup.delete()
        val hadDestination = destination.exists()
        if (hadDestination) require(destination.renameTo(backup)) { "Could not preserve the installed model" }
        if (!source.renameTo(destination)) {
            if (hadDestination) backup.renameTo(destination)
            throw IllegalStateException("Could not finish installing the model")
        }
        backup.delete()
    }

    private fun saveModelMetadata(displayName: String, size: Long, sha256: String) {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_SHA256, sha256)
            putString(KEY_DISPLAY_NAME, displayName)
            putLong(KEY_SIZE, size)
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") {
        "%02x".format(Locale.ROOT, it.toInt() and 0xff)
    }

    private fun formatMb(bytes: Long): Long = bytes / (1024L * 1024L)

    companion object {
        private const val PREFS = "retenqo_local_model"
        private const val KEY_SHA256 = "sha256"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_SIZE = "size"
        private const val COPY_BUFFER_BYTES = 1024 * 1024
        private const val MIN_FREE_SPACE_BYTES = 128L * 1024L * 1024L
        private const val MIN_MODEL_BYTES = 64L * 1024L * 1024L
        private const val RECOMMENDED_MODEL_NAME = "Qwen3.5-0.8B-Q4_0.gguf"
        private const val RECOMMENDED_MODEL_BYTES = 563_036_064L
        private const val RECOMMENDED_MODEL_SHA256 = "57d1997790d1744fba5b40a7317df71ea5e2acee28c47e78f0cce39c0703f8cf"
        private const val RECOMMENDED_MODEL_URL =
            "https://huggingface.co/ggml-org/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_0.gguf?download=true"
    }
}
