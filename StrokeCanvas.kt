package com.example.stylusdraw

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withSave
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke as InkStroke

/** Draws the given strokes on a plain Canvas without inking input. */
@Composable
fun StrokeCanvas(strokes: List<InkStroke>, renderer: CanvasStrokeRenderer) {
    Canvas(Modifier.fillMaxSize()) {
        val canvas = drawContext.canvas.nativeCanvas
        canvas.withSave {
            val m = Matrix().also { getMatrix(it) }
            strokes.forEach { renderer.draw(canvas, it, m) }
        }
    }
}
