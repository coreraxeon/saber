package com.example.stylusdraw.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
private data class SavedFolder(
    val name: String,
    val noteIds: List<String>,
    val parentId: String? = null
)

object FolderRepository {
    val allFolders = mutableStateListOf<Folder>()
    private val idIdx = mutableMapOf<String, Folder>()
    private lateinit var dir: File
    private val json = Json { encodeDefaults = true }

    /**
     * Initialize repository: clear previous state and load from disk.
     */
    fun init(ctx: Context) {
        dir = File(ctx.filesDir, "folders").apply { mkdirs() }
        allFolders.clear()
        idIdx.clear()
        dir.listFiles()?.forEach { f ->
            runCatching {
                val sf = json.decodeFromString(SavedFolder.serializer(), f.readText())
                val folder = Folder(
                    id = f.nameWithoutExtension,
                    name = sf.name,
                    noteIds = sf.noteIds.toMutableList(),
                    parentId = sf.parentId
                )
                add(folder)
            }
        }
    }

    fun createFolder(name: String, parentId: String? = null): Folder =
        Folder(UUID.randomUUID().toString(), name, parentId = parentId).also { folder ->
            add(folder)
            save(folder)
        }

    fun getFolder(id: String): Folder? = idIdx[id]

    fun addNoteToFolder(noteId: String, folderId: String) {
        val folder = idIdx[folderId] ?: return
        if (noteId !in folder.noteIds) {
            folder.noteIds.add(noteId)
            save(folder)
        }
    }

    fun folderForNote(noteId: String): Folder? =
        allFolders.firstOrNull { noteId in it.noteIds }

    fun removeNoteFromFolder(noteId: String, folderId: String) {
        val folder = idIdx[folderId] ?: return
        if (folder.noteIds.remove(noteId)) {
            save(folder)
        }
    }

    fun deleteFolder(id: String) {
        idIdx[id]?.let { folder ->
            allFolders.remove(folder)
            idIdx.remove(id)
            File(dir, "${folder.id}.json").delete()
        }
    }

    fun updateFileCount(folderId: String) {
        idIdx[folderId]?.let { folder ->
            val index = allFolders.indexOf(folder)
            if (index != -1) {
                val updatedFolder = folder.copy(noteIds = folder.noteIds)
                allFolders[index] = updatedFolder
                idIdx[folderId] = updatedFolder
                save(updatedFolder)
            }
        }
    }

    fun save(id: String) { idIdx[id]?.let { save(it) } }

    fun save(folder: Folder) {
        val sf = SavedFolder(folder.name, folder.noteIds, folder.parentId)
        dir.resolve("${folder.id}.json")
            .writeText(json.encodeToString(SavedFolder.serializer(), sf))
    }

    private fun add(folder: Folder) {
        allFolders.add(folder)
        idIdx[folder.id] = folder
    }
}