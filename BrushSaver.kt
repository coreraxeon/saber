// File: app/src/main/java/com/example/stylusdraw/ui/BrushSaver.kt
package com.example.stylusdraw.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes

private val stocks = arrayOf(
    StockBrushes.markerV1,
    StockBrushes.pressurePenV1,
    StockBrushes.highlighterV1
)

val BrushSaver: Saver<MutableState<Brush>, FloatArray> = Saver(
    save = { state ->
        floatArrayOf(
            stocks.indexOf(state.value.family).toFloat(),
            state.value.size,
            state.value.epsilon,
            state.value.colorLong.toFloat()
        )
    },
    restore = { array ->
        val brush = Brush.createWithColorLong(
            family    = stocks[array[0].toInt()],
            colorLong = array[3].toLong(),
            size      = array[1],
            epsilon   = array[2]
        )
        mutableStateOf(brush)
    }
)
