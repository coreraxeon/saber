// File: app/src/main/java/com/example/stylusdraw/InkingTouchListener.kt
package com.example.stylusdraw

import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.example.stylusdraw.data.NoteRepository
import kotlin.math.hypot
import com.example.stylusdraw.data.HistoryAction
import androidx.compose.ui.geometry.Offset
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import androidx.ink.strokes.MutableStrokeInputBatch
import com.example.stylusdraw.ViewMatrixManager

/**
 * InkingTouchListener
 *
 * • Draws new strokes when getSelectedTool()==0 (“Draw” mode).
 * • Erases entire strokes (if any point is within the eraser circle) when getSelectedTool()==1 (“Erase” mode).
 * • Does NOT use MotionEventPredictor, so we avoid “duplicate position/elapsed_time” errors.
 */
class InkingTouchListener(
    private val inkView: InProgressStrokesView,
    private val getDrawThickness: () -> Float,
    private val getEraserRadius: () -> Float,
    private val getCurrentColor: () -> Color,
    private val strokes: MutableList<Stroke>,
    private val selected: MutableList<Stroke>,
    private val lasso: MutableList<Offset>,
    private val strokeIdMap: MutableMap<InProgressStrokeId, Stroke>,
    private val getSelectedTool: () -> Int,
    private val history: MutableList<HistoryAction>,
    private val future: MutableList<HistoryAction>,
    private val noteId: String,
    private val viewMatrixManager: ViewMatrixManager
) : View.OnTouchListener {

    private var currentStrokeId: InProgressStrokeId? = null
    private val scratch = StrokeInput()
    private var moving = false
    private var lastX = 0f
    private var lastY = 0f

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        // Only respond to stylus input
        if (ev.getToolType(ev.actionIndex) != MotionEvent.TOOL_TYPE_STYLUS) {
            return false
        }

        val pagePos = viewMatrixManager.viewToPage(ev.x, ev.y)
        val pageX = pagePos.x
        val pageY = pagePos.y

        // === Erase mode (tool index == 1) ===
        if (getSelectedTool() == 1) {
            val r = getEraserRadius()
            if (ev.actionMasked == MotionEvent.ACTION_DOWN ||
                ev.actionMasked == MotionEvent.ACTION_MOVE
            ) {
                var anyDeleted = false
                val keptStrokes = mutableListOf<Stroke>()
                val deletedStrokes = mutableListOf<Stroke>()

                // For each existing stroke, if any point is within radius r, drop the entire stroke
                for (s in strokes) {
                    val inputs = s.inputs
                    var shouldDelete = false
                    for (i in 0 until inputs.size) {
                        inputs.populate(i, scratch)
                        val dx = scratch.x - pageX
                        val dy = scratch.y - pageY
                        if (hypot(dx, dy) <= r) {
                            shouldDelete = true
                            break
                        }
                    }
                    if (shouldDelete) {
                        anyDeleted = true
                        deletedStrokes.add(s)
                    } else {
                        keptStrokes.add(s)
                    }
                }

                if (anyDeleted) {
                    // Replace the strokes list with only those not deleted
                    strokes.clear()
                    strokes.addAll(keptStrokes)
                    // Record deletion actions and clear future redo history
                    deletedStrokes.forEach { history.add(HistoryAction.Delete(it)) }
                    future.clear()


                    // Also prune strokeIdMap so it only holds surviving strokes
                    val keptSet: Set<Stroke> = keptStrokes.toSet()
                    val itr = strokeIdMap.entries.iterator()
                    while (itr.hasNext()) {
                        val entry = itr.next()
                        if (!keptSet.contains(entry.value)) {
                            itr.remove()
                        }
                    }

                    // Persist the updated strokes immediately
                    NoteRepository.save(noteId)
                }
                return true
            }
            return true
        }

        // === Select mode (tool index == 2) ===
        if (getSelectedTool() == 2) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    moving = false
                    lastX = pageX
                    lastY = pageY
                    // If tap hits existing selection, start moving
                    if (selected.isNotEmpty() && pointInSelection(pageX, pageY)) {
                        moving = true
                    } else {
                        selected.clear()
                        lasso.clear()
                        lasso.add(Offset(pageX, pageY))
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (moving) {
                        val dx = pageX - lastX
                        val dy = pageY - lastY
                        translateSelected(dx, dy)
                        lastX = pageX
                        lastY = pageY
                    } else {
                        lasso.add(Offset(pageX, pageY))
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (moving) {
                        moving = false
                    } else {
                        lasso.add(Offset(pageX, pageY))
                        selected.clear()
                        selected.addAll(findStrokesInLasso())
                        lasso.clear()
                    }
                    if (selected.isEmpty()) {
                        moving = false
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    moving = false
                    lasso.clear()
                    return true
                }
            }
        }


        // === Draw mode (tool index == 0) ===
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.requestUnbufferedDispatch(ev)
                val brush = Brush.createWithColorIntArgb(
                    family       = StockBrushes.pressurePenV1,
                    colorIntArgb = getCurrentColor().toArgb(),
                    size         = getDrawThickness(),
                    epsilon      = 0.1f
                )
                future.clear()
                currentStrokeId = inkView.startStroke(
                    ev, ev.getPointerId(ev.actionIndex), brush
                )
                true
            }
            MotionEvent.ACTION_MOVE -> {
                currentStrokeId?.let { id ->
                    // Directly send the raw MotionEvent to addToStroke (no predictor)
                    inkView.addToStroke(ev, ev.getPointerId(ev.actionIndex), id, null)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                currentStrokeId?.let { id ->
                    inkView.finishStroke(ev, ev.getPointerId(ev.actionIndex), id)
                    v.performClick()
                }
                currentStrokeId = null
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                currentStrokeId?.let { inkView.cancelStroke(it, ev) }
                currentStrokeId = null
                true
            }
            else -> false
        }
    }

    /**
     * Called by InProgressStrokesView when strokes are finalized by the Ink SDK.
     * We take each new stroke, move it into our `strokes` list + `strokeIdMap`, persist, then
     * remove it from the inkView’s in-progress buffer.
     */
    fun onStrokesFinished(map: Map<InProgressStrokeId, Stroke>) {
        strokeIdMap.putAll(map)
        strokes += map.values
        map.values.forEach { history.add(HistoryAction.Add(it)) }
        future.clear()
        NoteRepository.save(noteId)
        inkView.removeFinishedStrokes(map.keys)
    }

    private fun pointInSelection(x: Float, y: Float): Boolean {
        val scratch = StrokeInput()
        selected.forEach { s ->
            val inputs = s.inputs
            for (i in 0 until inputs.size) {
                inputs.populate(i, scratch)
                if (hypot(scratch.x - x, scratch.y - y) <= getEraserRadius()) {
                    return true
                }
            }
        }
        return false
    }

    private fun translateSelected(dx: Float, dy: Float) {
        val si = StrokeInput()
        val toReplace = mutableListOf<Pair<Stroke, Stroke>>()
        for (s in selected) {
            val batch = androidx.ink.strokes.MutableStrokeInputBatch()
            val inputs = s.inputs
            for (i in 0 until inputs.size) {
                inputs.populate(i, si)
                batch.addOrThrow(
                    type = inputs.getToolType(),
                    x = si.x + dx,
                    y = si.y + dy,
                    elapsedTimeMillis = si.elapsedTimeMillis,
                    pressure = si.pressure,
                    tiltRadians = si.tiltRadians,
                    orientationRadians = si.orientationRadians
                )
            }
            val copy = Stroke(s.brush, batch)
            toReplace.add(s to copy)
        }
        toReplace.forEach { (old, new) ->
            val idx = strokes.indexOf(old)
            if (idx >= 0) strokes[idx] = new
            val selIdx = selected.indexOf(old)
            if (selIdx >= 0) selected[selIdx] = new
        }
        NoteRepository.save(noteId)
    }

    private fun findStrokesInLasso(): List<Stroke> {
        if (lasso.size < 3) return emptyList()
        val path = android.graphics.Path().apply {
            moveTo(lasso[0].x, lasso[0].y)
            for (i in 1 until lasso.size) lineTo(lasso[i].x, lasso[i].y)
            close()
        }
        val region = android.graphics.Region().apply {
            val bounds = android.graphics.RectF()
            path.computeBounds(bounds, true)
            setPath(path, android.graphics.Region(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()))
        }
        val found = mutableListOf<Stroke>()
        val si = StrokeInput()
        for (s in strokes) {
            val inputs = s.inputs
            var inside = false
            for (i in 0 until inputs.size) {
                inputs.populate(i, si)
                if (region.contains(si.x.toInt(), si.y.toInt())) {
                    inside = true
                    break
                }
            }
            if (inside) found.add(s)
        }
        return found
    }
}
