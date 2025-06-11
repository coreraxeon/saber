package com.example.stylusdraw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Constants for default page dimensions and spacing */
private const val INCH_TO_DP = 96
val PAGE_WIDTH = (8.5f * INCH_TO_DP).dp
val PAGE_HEIGHT = (11f * INCH_TO_DP).dp
val PAGE_SPACING = PAGE_WIDTH * 0.05f

@Composable
fun PageLayout(
    viewMatrixManager: ViewMatrixManager,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFFE0E0E0))
            // Detect pinch zoom and finger pan gestures with fingers only
            .pointerInput("transform") {
                awaitPointerEventScope {
                    var prevCentroid: Offset? = null
                    var prevDistance: Float? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.filter { it.pressed && it.type == PointerType.Touch }
                        if (fingers.isEmpty()) {
                            prevCentroid = null
                            prevDistance = null
                            continue
                        }

                        val centroid = fingers.map { it.position }.reduce { a, b -> a + b } / fingers.size.toFloat()

                        if (fingers.size >= 2) {
                            val diff = fingers[0].position - fingers[1].position
                            val d = kotlin.math.hypot(diff.x.toDouble(), diff.y.toDouble()).toFloat()
                            val prevD = prevDistance ?: d
                            val zoom = d / prevD
                            val pan = if (prevCentroid != null) centroid - prevCentroid!! else Offset.Zero
                            if (zoom != 1f || pan != Offset.Zero) {
                                viewMatrixManager.onGesture(zoom, pan, centroid.x, centroid.y)
                            }
                            prevDistance = d
                        } else {
                            val pan = if (prevCentroid != null) centroid - prevCentroid!! else Offset.Zero
                            if (viewMatrixManager.scale > 1f && pan != Offset.Zero) {
                                viewMatrixManager.onGesture(1f, pan, centroid.x, centroid.y)
                            }
                            prevDistance = null
                        }

                        prevCentroid = centroid
                        fingers.forEach { it.consume() }
                    }
                }
            }
            .graphicsLayer {
                translationX = viewMatrixManager.translationX
                translationY = viewMatrixManager.translationY
                scaleX = viewMatrixManager.scale
                scaleY = viewMatrixManager.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
    ) {
        content()
    }
}