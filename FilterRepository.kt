package com.example.stylusdraw.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Serializable
private data class SavedFilter(val name: String, val color: Int)

object FilterRepository {
    val allFilters = mutableStateListOf<Filter>()
    private val idIdx = mutableStateMapOf<String, Filter>()
    private lateinit var dir: File
    private val json = Json { encodeDefaults = true }

    /** Currently active filter (null means no filter) */
    val activeFilterId = mutableStateOf<String?>(null)

    /**
     * Initialize repository: clear previous state and load from disk.
     */
    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "filters").apply { mkdirs() }
        // Clear any existing in-memory state to avoid duplicates
        allFilters.clear()
        idIdx.clear()
        activeFilterId.value = null

        dir.listFiles()?.forEach { f ->
            runCatching {
                val sf = json.decodeFromString(SavedFilter.serializer(), f.readText())
                val filter = Filter(
                    id = f.nameWithoutExtension,
                    name = sf.name,
                    color = sf.color
                )
                add(filter)
            }
        }
    }

    fun createFilter(name: String, color: Color): Filter =
        Filter(UUID.randomUUID().toString(), name, color.toArgb()).also {
            add(it)
            save(it)
        }

    fun getFilter(id: String?): Filter? = id?.let { idIdx[it] }

    fun deleteFilter(id: String) {
        idIdx[id]?.let { filter ->
            allFilters.remove(filter)
            idIdx.remove(id)
            if (activeFilterId.value == id) activeFilterId.value = null
            File(dir, "$id.json").delete()
        }
    }

    fun save(id: String) { idIdx[id]?.let { save(it) } }

    fun save(filter: Filter) {
        val sf = SavedFilter(filter.name, filter.color)
        dir.resolve("${'$'}{filter.id}.json")
            .writeText(json.encodeToString(SavedFilter.serializer(), sf))
    }

    private fun add(f: Filter) {
        allFilters += f
        idIdx[f.id] = f
    }
}