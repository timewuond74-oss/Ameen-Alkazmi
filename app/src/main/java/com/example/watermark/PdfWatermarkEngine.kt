package com.example.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfWatermarkEngine {

    suspend fun getFirstPagePreview(context: Context, sourceUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext null
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pdfRenderer.pageCount == 0) {
                pdfRenderer.close()
                parcelFileDescriptor.close()
                return@withContext null
            }

            val page = pdfRenderer.openPage(0)
            val width = (page.width * 1.5f).toInt()
            val height = (page.height * 1.5f).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.PageRenderMode.FOR_DISPLAY)

            page.close()
            pdfRenderer.close()
            parcelFileDescriptor.close()
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun applyUnremovableWatermark(
        context: Context,
        sourceUri: Uri,
        watermarkText: String,
        opacity: Int,
        textSize: Float,
        angle: Float,
        textColor: Int
    ): File? = withContext(Dispatchers.IO) {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return@withContext null
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val outputPdf = PdfDocument()

            val paint = Paint().apply {
                color = textColor
                alpha = opacity
                this.textSize = textSize
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val scale = 2f

            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.PageRenderMode.FOR_PRINT)

                canvas.save()
                val stepX = width / 2.5f
                val stepY = height / 4f

                var x = -width.toFloat()
                while (x < width * 2) {
                    var y = -height.toFloat()
                    while (y < height * 2) {
                        canvas.save()
                        canvas.rotate(angle, x, y)
                        canvas.drawText(watermarkText, x, y, paint)
                        canvas.restore()
                        y += stepY
                    }
                    x += stepX
                }
                canvas.restore()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val newPage = outputPdf.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                outputPdf.finishPage(newPage)

                bitmap.recycle()
                page.close()
            }

            pdfRenderer.close()
            parcelFileDescriptor.close()

            val outputFile = File(context.cacheDir, "Protected_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { out ->
                outputPdf.writeTo(out)
            }
            outputPdf.close()

            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
