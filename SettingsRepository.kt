package com.example.stylusdraw.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val DEFAULT_COLOR_OPTIONS = listOf(
    Color.Black,
    Color.Red,
    Color.Green,
    Color.Blue,
    Color.Yellow,
    Color.Cyan,
    Color.Magenta,
    Color.Gray,
    Color(0xFFFFA500), // Orange
    Color(0xFF800080), // Purple
    Color(0xFF008080), // Teal
    Color.White
)

object SettingsRepository {
    private const val PREF_NAME = "settings"
    private const val KEY_DRAW_THICKNESS = "drawThickness"
    private const val KEY_ERASER_RADIUS = "eraserRadius"
    private const val KEY_CURRENT_INDEX = "currentColorIndex"
    private const val KEY_COLOR_PREFIX = "color_"
    private const val KEY_VISIBLE_SLOTS = "visibleSlots"

    private lateinit var prefs: SharedPreferences
    var drawThickness = mutableStateOf(5f)
    var eraserRadius = mutableStateOf(10f)
    val palette = mutableStateListOf<Color>()
    var currentColorIndex = mutableStateOf(0)
    var visibleColorSlots = mutableStateOf(5)

    /**
     * Initialize settings: clear any previous in-memory state and load from SharedPreferences.
     */
    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Reset state values to defaults before loading
        drawThickness.value = 5f
        eraserRadius.value = 10f
        visibleColorSlots.value = 5
        palette.clear()
        currentColorIndex.value = 0

        // Load saved values
        drawThickness.value = prefs.getFloat(KEY_DRAW_THICKNESS, drawThickness.value)
        eraserRadius.value = prefs.getFloat(KEY_ERASER_RADIUS, eraserRadius.value)
        visibleColorSlots.value = prefs.getInt(KEY_VISIBLE_SLOTS, visibleColorSlots.value)

        val defaultColors = DEFAULT_COLOR_OPTIONS
        for (i in defaultColors.indices) {
            val argb = prefs.getInt("${'$'}KEY_COLOR_PREFIX$i", defaultColors[i].toArgb())
            palette += Color(argb)
        }

        if (palette.isEmpty()) {
            palette += defaultColors
        }

        currentColorIndex.value = prefs.getInt(KEY_CURRENT_INDEX, currentColorIndex.value)
            .coerceIn(palette.indices)
    }

    fun setCurrentColorIndex(idx: Int) {
        currentColorIndex.value = idx.coerceIn(palette.indices)
        save()
    }

    fun setPaletteColor(idx: Int, color: Color) {
        if (idx in palette.indices) {
            palette[idx] = color
            save()
        }
    }

    fun setVisibleSlots(count: Int) {
        visibleColorSlots.value = count.coerceAtLeast(1)
        save()
    }

    /** Persist all settings back to SharedPreferences */
    fun save() {
        if (!::prefs.isInitialized) return
        prefs.edit().apply {
            putFloat(KEY_DRAW_THICKNESS, drawThickness.value)
            putFloat(KEY_ERASER_RADIUS, eraserRadius.value)
            putInt(KEY_CURRENT_INDEX, currentColorIndex.value)
            putInt(KEY_VISIBLE_SLOTS, visibleColorSlots.value)
            palette.forEachIndexed { idx, col ->
                putInt("${'$'}KEY_COLOR_PREFIX$idx", col.toArgb())
            }
        }.apply()
    }

    fun currentColor(): Color = palette.getOrElse(currentColorIndex.value) { Color.Black }
}
