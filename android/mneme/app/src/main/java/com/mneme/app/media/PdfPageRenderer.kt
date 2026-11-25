package com.mneme.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.File

/** Render PDF pages to bitmaps for ML Kit OCR (digital PDFs don't need a VLM). */
object PdfPageRenderer {
    fun renderPages(context: Context, pdf: File, maxPages: Int = 2, scale: Int = 2): List<Bitmap> {
        if (!pdf.isFile) return emptyList()
        val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val out = ArrayList<Bitmap>(minOf(maxPages, renderer.pageCount))
        try {
            val n = minOf(maxPages, renderer.pageCount)
            for (i in 0 until n) {
                renderer.openPage(i).use { page ->
                    val w = page.width * scale
                    val h = page.height * scale
                    val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    out.add(bmp)
                }
            }
        } finally {
            renderer.close()
            pfd.close()
        }
        return out
    }
}
