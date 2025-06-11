package com.example.stylusdraw.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.example.stylusdraw.SavedStroke
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import androidx.compose.runtime.toMutableStateList

@Serializable
private data class SavedPage(val strokes: List<SavedStroke>)

@Serializable
private data class SavedNote(
    val title: String,
    val layout: Int,
    val currentPage: Int,
    val pages: List<SavedPage>,
    val filterId: String? = null
)

object NoteRepository {
    val allNotes = mutableStateListOf<Note>()
    private val idIdx = mutableStateMapOf<String, Note>()
    private lateinit var dir: File
    private val json = Json { encodeDefaults = true }

    /**
     * Initialize repository: clear previous state and load from disk.
     */
    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "notes").apply { mkdirs() }
        allNotes.clear()
        idIdx.clear()
        dir.listFiles()?.forEach { f ->
            runCatching {
                val sn = json.decodeFromString(SavedNote.serializer(), f.readText())
                val note = Note(
                    id = f.nameWithoutExtension,
                    title = sn.title,
                    layout = LayoutMode.entries[sn.layout],
                    currentPage = sn.currentPage,
                    pages = sn.pages.map { p ->
                        Page(p.strokes.map { it.toStroke() }.toMutableStateList())
                    }.toMutableStateList(),
                    filterId = sn.filterId
                )
                add(note)
            }
        }
    }

    fun createNote(title: String, filterId: String? = null): Note =
        Note(UUID.randomUUID().toString(), title, filterId = filterId).also {
            add(it)
            save(it)
        }

    fun getNote(id: String): Note? = idIdx[id]

    fun save(id: String) = idIdx[id]?.let { save(it) }

    fun save(note: Note) {
        val sn = SavedNote(
            title       = note.title,
            layout      = note.layout.ordinal,
            currentPage = note.currentPage,
            pages       = note.pages.map { SavedPage(it.strokes.map(SavedStroke::fromStroke)) },
            filterId    = note.filterId
        )
        dir.resolve("${note.id}.json")
            .writeText(json.encodeToString(SavedNote.serializer(), sn))
    }

    /** Delete a note from memory and disk */
    fun deleteNote(id: String) {
        idIdx[id]?.let { note ->
            allNotes.remove(note)
            idIdx.remove(id)
            File(dir, "$id.json").delete()
        }
    }

    private fun add(n: Note) {
        allNotes += n
        idIdx[n.id] = n
    }
}
