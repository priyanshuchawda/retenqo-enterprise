package com.mneme.app.pulse

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import androidx.core.graphics.createBitmap
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * One-shot screen capture via MediaProjection (user grants once per session).
 */
class ScreenCapture(
    private val context: Context,
) {
    private val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun createPermissionIntent(): Intent = projectionManager.createScreenCaptureIntent()

    suspend fun capture(resultCode: Int, data: Intent): Bitmap = withContext(Dispatchers.Main) {
        val projection = projectionManager.getMediaProjection(resultCode, data)
            ?: throw IllegalStateException("MediaProjection denied")
        try {
            withTimeout(CAPTURE_TIMEOUT_MILLIS) { captureOnce(projection) }
        } finally {
            runCatching { projection.stop() }
        }
    }

    private suspend fun captureOnce(projection: MediaProjection): Bitmap =
        suspendCancellableCoroutine { cont ->
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val thread = HandlerThread("mneme-capture").also { it.start() }
            val handler = Handler(thread.looper)
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            var virtualDisplay: VirtualDisplay? = null
            lateinit var callback: MediaProjection.Callback
            var cleaned = false
            fun cleanup() {
                if (cleaned) return
                cleaned = true
                runCatching { virtualDisplay?.release() }
                runCatching { reader.close() }
                runCatching { projection.unregisterCallback(callback) }
                runCatching { thread.quitSafely() }
            }

            callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    cleanup()
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("Screen capture permission ended"))
                }
            }
            projection.registerCallback(callback, handler)

            reader.setOnImageAvailableListener({
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bitmap = createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    if (cropped !== bitmap) bitmap.recycle()

                    cleanup()
                    projection.stop()

                    if (cont.isActive) cont.resume(cropped)
                } catch (e: Exception) {
                    cleanup()
                    projection.stop()
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "mneme-pulse",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )

            cont.invokeOnCancellation {
                cleanup()
                projection.stop()
            }
        }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
        private const val CAPTURE_TIMEOUT_MILLIS = 10_000L
    }
}
