package com.example.stylusdraw

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.ink.strokes.StrokeInput
import com.example.stylusdraw.data.Note
import java.io.File
import java.io.FileOutputStream

object PdfExport {
    fun export(context: Context, note: Note): File {
        val pdfWidth = (8.5f * 72).toInt()
        val pdfHeight = (11f * 72).toInt()
        val pdf = PdfDocument()

        val pageScale = pdfWidth / (8.5f * 160f)
        val matrix = Matrix().apply { setScale(pageScale, pageScale) }
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        val scratch = StrokeInput()

        note.pages.forEachIndexed { idx, page ->
            val info = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, idx + 1).create()
            val p = pdf.startPage(info)
            page.strokes.forEach { stroke ->
                val path = Path()
                val inputs = stroke.inputs
                if (inputs.size > 0) {
                    inputs.populate(0, scratch)
                    path.moveTo(scratch.x, scratch.y)
                    for (i in 1 until inputs.size) {
                        inputs.populate(i, scratch)
                        path.lineTo(scratch.x, scratch.y)
                    }
                }
                p.canvas.save()
                p.canvas.concat(matrix)
                paint.color = stroke.brush.colorLong.toInt()
                paint.strokeWidth = stroke.brush.size * pageScale
                p.canvas.drawPath(path, paint)
                p.canvas.restore()
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