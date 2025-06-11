package com.example.stylusdraw

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ViewMatrixManager {
    var scale by mutableStateOf(1f)
        private set
    var translationX by mutableStateOf(0f)
        private set
    var translationY by mutableStateOf(0f)
        private set

    val viewMatrix: Matrix = Matrix()
    val inverseMatrix: Matrix = Matrix()

    // Position of the page on screen, used to map MotionEvent coordinates
    var pageOffset: Offset = Offset.Zero

    fun setPageOffset(offset: Offset) {
        pageOffset = offset
    }

    fun onGesture(zoom: Float, pan: Offset, pivotX: Float, pivotY: Float) {
        viewMatrix.postTranslate(-pivotX, -pivotY)
        viewMatrix.postScale(zoom, zoom)
        viewMatrix.postTranslate(pivotX, pivotY)
        viewMatrix.postTranslate(pan.x, pan.y)
        updateValues()
    }

    fun screenToPage(x: Float, y: Float): Offset {
        val pts = floatArrayOf(x - pageOffset.x, y - pageOffset.y)
        inverseMatrix.mapPoints(pts)
        return Offset(pts[0], pts[1])
    }

    fun copyMatrix(out: Matrix) {
        out.set(viewMatrix)
    }

    private fun updateValues() {
        val values = FloatArray(9)
        viewMatrix.getValues(values)
        scale = values[Matrix.MSCALE_X]
        translationX = values[Matrix.MTRANS_X]
        translationY = values[Matrix.MTRANS_Y]
        viewMatrix.invert(inverseMatrix)
    }
}

// Default manager used by StrokeCanvas for static pages
val DefaultViewMatrixManager = ViewMatrixManager()

fun getMatrix(out: Matrix) {
    DefaultViewMatrixManager.copyMatrix(out)
}