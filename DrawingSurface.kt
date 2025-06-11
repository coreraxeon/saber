// File: app/src/main/java/com/example/stylusdraw/DrawingSurface.kt
package com.example.stylusdraw

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.FrameLayout
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.withSave
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke as InkStroke
import com.example.stylusdraw.ui.stylusinput.StylusInput
import com.example.stylusdraw.ViewMatrixManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.ink.strokes.StrokeInput
import com.example.stylusdraw.SavedStroke
import com.example.stylusdraw.data.NoteRepository

/**
 * DrawingSurface
 *
 * • Renders only the strokes passed in (from note.page.strokes).
 * • Shows an eraser circle when getSelectedTool() == 1, tracking hover + touch.
 * • Combines StylusInput (hover / side-button) and InkingTouchListener (draw / erase)
 *   in a single OnTouchListener on inkView.
 * • Expects inkView to come from NoteScreen (where it’s remembered per noteId),
 *   so that each tab/note has its own independent canvas.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrawingSurface(
    // The committed strokes for this page
    strokes: MutableList<InkStroke>,
    history: MutableList<com.example.stylusdraw.data.HistoryAction>,
    future: MutableList<com.example.stylusdraw.data.HistoryAction>,
    // A per-note map of InProgressStrokeId → Stroke for saving/removal
    strokeIdMap: MutableMap<androidx.ink.authoring.InProgressStrokeId, InkStroke>,
    // Draws each committed stroke onto the Compose Canvas
    renderer: CanvasStrokeRenderer,
    // Return the current tool index: 0 = Draw, 1 = Erase
    getSelectedTool: () -> Int,
    // Switch tools when side-button is pressed/released
    setSelectedTool: (Int) -> Unit,
    // Current pen thickness
    getDrawThickness: () -> Float,
    // Current eraser radius
    getEraserRadius: () -> Float,
    // Current ink color
    getCurrentColor: () -> Color,
    // The note’s unique ID (used to rebuild listeners when you switch tabs)
    noteId: String,
    viewMatrixManager: ViewMatrixManager,
    // Callbacks for stylus touch down/up
    onStylusDown: () -> Unit,
    onStylusUp: () -> Unit
) {
    val context = LocalContext.current

    // InProgressStrokesView cannot be moved between parents, so create a fresh
    // instance for each DrawingSurface.
    val inkView = remember(noteId) { InProgressStrokesView(context) }

    // Update the view transform to match the current zoom/pan state
    LaunchedEffect(viewMatrixManager.viewMatrix) {
        inkView.motionEventToViewTransform = viewMatrixManager.inverseMatrix
    }

    // Keep track of stylus tip position for the eraser circle
    var stylusPos by remember { mutableStateOf(Offset.Zero) }
    val selectedStrokes = remember { mutableStateListOf<InkStroke>() }
    val lassoPoints     = remember { mutableStateListOf<Offset>() }

    // === Step 1: Build a fresh InkingTouchListener for this noteId ===
    val inkingListener = remember(noteId, strokes, history, future) {
        InkingTouchListener(
            inkView = inkView,
            getDrawThickness = getDrawThickness,
            getEraserRadius = getEraserRadius,
            getCurrentColor = getCurrentColor,
            strokes = strokes,
            selected = selectedStrokes,
            lasso = lassoPoints,
            strokeIdMap = strokeIdMap,
            getSelectedTool = getSelectedTool,
            history = history,
            future = future,
            noteId = noteId,
            viewMatrixManager = viewMatrixManager
        )
    }

    // === Step 2: Build a fresh StylusInput for this noteId ===
    var stylusInput: StylusInput? by remember(noteId) { mutableStateOf(null) }
    DisposableEffect(inkView) {
        onDispose { stylusInput?.cleanup() }
    }

    Box(Modifier.fillMaxSize()) {
        // === Step 3: Key AndroidView on noteId so it fully rebuilds on tab switch ===
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // (3a) A small FrameLayout subclass that overrides performClick()
                val frame = object : FrameLayout(ctx) {
                    override fun performClick(): Boolean {
                        super.performClick()
                        return true
                    }
                }

                // (3b) Add the provided per-note inkView into that frame
                (inkView.parent as? ViewGroup)?.removeView(inkView)
                frame.addView(inkView)

                // (3c) Create StylusInput bound to this frame (hover + side-button)
                val stylus = StylusInput(
                    activity = ctx as Activity,
                    targetView = frame,
                    listener = object : StylusInput.Listener {
                        override fun onSideButtonDown() {
                            setSelectedTool(1) // switch to Erase
                        }
                        override fun onSideButtonUp() {
                            setSelectedTool(0) // switch back to Draw
                        }
                        override fun onPenHover(x: Float, y: Float, isEraser: Boolean) {
                            stylusPos = Offset(x, y)
                        }
                        override fun onStylusDown(x: Float, y: Float, isEraser: Boolean) {
                            stylusPos = Offset(x, y)
                            onStylusDown()
                        }
                        override fun onStylusMove(x: Float, y: Float, isEraser: Boolean) {
                            stylusPos = Offset(x, y)
                        }
                        override fun onStylusUp(x: Float, y: Float, isEraser: Boolean) {
                            stylusPos = Offset(x, y)
                            onStylusUp()
                        }
                    }
                )
                stylusInput = stylus

                // (3d) Combine StylusInput and InkingTouchListener in one OnTouchListener:
                inkView.setOnTouchListener { v, ev ->
                    // 1) Update hover/side-button (so eraser circle never freezes)
                    stylus.handleTouchEvent(ev)
                    // 2) Run draw/erase logic for this note
                    inkingListener.onTouch(v, ev)
                    true
                }

                // (3e) Register for “strokes finished” callbacks from the Ink SDK:
                inkView.addFinishedStrokesListener(
                    object : InProgressStrokesFinishedListener {
                        override fun onStrokesFinished(
                            finished: Map<androidx.ink.authoring.InProgressStrokeId, InkStroke>
                        ) {
                            inkingListener.onStrokesFinished(finished)
                        }
                    }
                )

                frame
            },
            update = {
                // Nothing to update here—`factory` is keyed on noteId
            }
        )

        // === Step 4: Draw committed strokes onto the Compose Canvas ===
        Canvas(Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            canvas.withSave {
                strokes.forEach { renderer.draw(canvas, it, viewMatrixManager.viewMatrix) }
            }
            if (getSelectedTool() == 1) {
                drawCircle(
                    Color.Gray.copy(alpha = 0.5f),
                    radius = getEraserRadius(),
                    center = stylusPos,
                    style  = Stroke(width = getDrawThickness())
                )
            }


            // Draw lasso path in select mode
            if (getSelectedTool() == 2 && lassoPoints.size > 1) {
                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(lassoPoints[0].x, lassoPoints[0].y)
                for (i in 1 until lassoPoints.size) {
                    path.lineTo(lassoPoints[i].x, lassoPoints[i].y)
                }
                drawPath(
                    path,
                    Color.Blue,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
            }

            // Draw bounding box around selected strokes
            if (selectedStrokes.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE
                val scratch = StrokeInput()
                for (s in selectedStrokes) {
                    val inputs = s.inputs
                    for (i in 0 until inputs.size) {
                        inputs.populate(i, scratch)
                        if (scratch.x < minX) minX = scratch.x
                        if (scratch.x > maxX) maxX = scratch.x
                        if (scratch.y < minY) minY = scratch.y
                        if (scratch.y > maxY) maxY = scratch.y
                    }
                }
                if (maxX > minX && maxY > minY) {
                    drawRect(
                        Color.Blue,
                        topLeft = Offset(minX, minY),
                        size = androidx.compose.ui.geometry.Size(maxX - minX, maxY - minY),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    )
                }
            }
        }

        if (selectedStrokes.isNotEmpty()) {
            Row(
                Modifier.align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                androidx.compose.material3.TextButton(onClick = {
                    // Copy selected strokes
                    val scratch = mutableListOf<InkStroke>()
                    selectedStrokes.forEach { s ->
                        val copy = SavedStroke.fromStroke(s).toStroke()
                        scratch.add(copy)
                    }
                    strokes.addAll(scratch)
                    selectedStrokes.clear()
                    NoteRepository.save(noteId)
                }) { androidx.compose.material3.Text("Copy") }
                androidx.compose.material3.TextButton(onClick = {
                    strokes.removeAll(selectedStrokes)
                    selectedStrokes.clear()
                    NoteRepository.save(noteId)
                }) { androidx.compose.material3.Text("Delete") }
            }
        }
    }
}
