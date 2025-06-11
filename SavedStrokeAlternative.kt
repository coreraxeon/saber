// File: app/src/main/java/com/example/stylusdraw/data/SavedStrokeAlternative.kt
package com.example.stylusdraw

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlinx.serialization.Serializable

@Serializable
data class SavedPoint(
    val x: Float, val y: Float, val t: Long,
    val p: Float, val tilt: Float, val orient: Float
)

@Serializable
data class SavedBrush(
    val fam: Int, val size: Float, val eps: Float, val color: Long
)

@Serializable
data class SavedStroke(
    val brush: SavedBrush,
    val tool: Int,
    val unitCm: Float,
    val pts: List<SavedPoint>
) {
    companion object {
        fun fromStroke(s: Stroke): SavedStroke {
            val fam = when (s.brush.family) {
                StockBrushes.markerV1      -> 0
                StockBrushes.pressurePenV1 -> 1
                StockBrushes.highlighterV1 -> 2
                else                       -> 0
            }
            val tool = when (s.inputs.getToolType()) {
                InputToolType.STYLUS -> 1
                InputToolType.TOUCH  -> 2
                InputToolType.MOUSE  -> 3
                else                 -> 0
            }
            val scratch = StrokeInput()
            val points  = buildList {
                repeat(s.inputs.size) { i ->
                    s.inputs.populate(i, scratch)
                    add(SavedPoint(
                        scratch.x, scratch.y, scratch.elapsedTimeMillis,
                        scratch.pressure, scratch.tiltRadians, scratch.orientationRadians
                    ))
                }
            }
            return SavedStroke(
                brush  = SavedBrush(fam, s.brush.size, s.brush.epsilon, s.brush.colorLong),
                tool   = tool,
                unitCm = s.inputs.getStrokeUnitLengthCm(),
                pts    = points
            )
        }
    }

    fun toStroke(): Stroke {
        val fam = when (brush.fam) {
            1 -> StockBrushes.pressurePenV1
            2 -> StockBrushes.highlighterV1
            else -> StockBrushes.markerV1
        }
        val realBrush = Brush.createWithColorLong(fam, brush.color, brush.size, brush.eps)
        val tool = when (tool) {
            1 -> InputToolType.STYLUS
            2 -> InputToolType.TOUCH
            3 -> InputToolType.MOUSE
            else -> InputToolType.UNKNOWN
        }
        val batch = MutableStrokeInputBatch()
        pts.forEach {
            batch.addOrThrow(
                type               = tool,
                x                  = it.x,
                y                  = it.y,
                elapsedTimeMillis  = it.t,
                pressure           = it.p,
                tiltRadians        = it.tilt,
                orientationRadians = it.orient
            )
        }
        return Stroke(realBrush, batch)
    }
}
