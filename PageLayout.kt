package com.example.stylusdraw

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
            .pointerInput(Unit) {
            awaitPointerEventScope {
                var prevCentroid: Offset? = null
                while (true) {
                    val event = awaitPointerEvent()

                    // Swallow stylus events so they never trigger page swipes
                    event.changes
                        .filter { it.pressed && it.type == PointerType.Stylus }
                        .forEach { it.consume() }

                    val fingers = event.changes.filter { it.pressed && it.type == PointerType.Touch }
                    if (fingers.isNotEmpty()) {
                        val centroid = fingers.map { it.position }.reduce { a, b -> a + b } / fingers.size.toFloat()
                        val pan = prevCentroid?.let { centroid - it } ?: Offset.Zero
                        prevCentroid = centroid

                        // Only pan when zoomed in so finger swipes can flip pages
                        if (viewMatrixManager.scale > 1f && pan != Offset.Zero) {
                            viewMatrixManager.onGesture(
                                zoom = 1f,
                                pan = pan,
                                pivotX = centroid.x,
                                pivotY = centroid.y
                            )
                            fingers.forEach { it.consume() }
                        }
                    } else {
                        prevCentroid = null
                    }
                }
            }
        }
    ) {
        content()
    }
}
