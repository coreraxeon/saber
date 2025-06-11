// Kotlin
package com.example.stylusdraw.data

import androidx.compose.runtime.mutableStateListOf
import androidx.ink.strokes.Stroke
import kotlinx.serialization.Serializable

enum class LayoutMode { PAGES, INFINITE, INFINITE_DOWN }

data class Page(
    var strokes: MutableList<Stroke> = mutableStateListOf(),
    var history: MutableList<HistoryAction> = mutableStateListOf(),
    var future: MutableList<HistoryAction> = mutableStateListOf()
)

sealed interface HistoryAction {
    data class Add(val stroke: Stroke) : HistoryAction
    data class Delete(val stroke: Stroke) : HistoryAction
}

data class Note(
    val id: String,
    var title: String,
    var layout: LayoutMode = LayoutMode.PAGES,
    var currentPage: Int = 0,
    val pages: MutableList<Page> = mutableStateListOf(Page()),
    var filterId: String? = null
) {
    val fileCount: Int
        get() = pages.size
}

data class Folder(
    val id: String,
    var name: String,
    val noteIds: MutableList<String> = mutableStateListOf(),
    // Optional parent folder id to support nested folders (null for root folders)
    val parentId: String? = null
) {
    val fileCount: Int
        get() = noteIds.size
}

data class Filter(
    val id: String,
    var name: String,
    var color: Int
)