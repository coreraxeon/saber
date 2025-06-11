package com.example.stylusdraw

import android.content.Context
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.stylusdraw.data.Note
import java.io.File
import java.io.FileOutputStream

private const val APP_DPI = 96f
private const val PDF_DPI = 72f
private const val PAGE_WIDTH_PT = (8.5f * PDF_DPI).toInt()
private const val PAGE_HEIGHT_PT = (11f * PDF_DPI).toInt()
private const val SCALE = PDF_DPI / APP_DPI

object PdfExport {
    fun export(context: Context, note: Note): File {
        val pdfWidth = PAGE_WIDTH_PT
        val pdfHeight = PAGE_HEIGHT_PT
        val pdf = PdfDocument()

        val matrix = Matrix().apply { setScale(SCALE, SCALE) }
        val renderer = CanvasStrokeRenderer.create()

        note.pages.forEachIndexed { idx, page ->
            val info = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, idx + 1).create()
            val p = pdf.startPage(info)
            page.strokes.forEach { stroke ->
                renderer.draw(p.canvas, stroke, matrix)
            }
            pdf.finishPage(p)
        }

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outFile = File(downloads, "${note.title}.pdf")
        FileOutputStream(outFile).use { fos -> pdf.writeTo(fos) }
        pdf.close()
        Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
        return outFile
    }
}