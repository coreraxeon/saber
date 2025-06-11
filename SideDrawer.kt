package com.example.stylusdraw

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.example.stylusdraw.data.FilterRepository
import com.example.stylusdraw.data.FolderRepository
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.Folder
import com.example.stylusdraw.DrawerInterop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SideDrawer(nav: NavController, closeDrawer: () -> Unit) {
    val folders = FolderRepository.allFolders.filter { it.parentId == null }
    val activeFilter by FilterRepository.activeFilterId
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var editFolder by remember { mutableStateOf<Folder?>(null) }
    var editName by remember { mutableStateOf("") }
    // Modified: Notes outside folders are only those not contained in any folder.
    val notesNoFolder = if (activeFilter == null)
        NoteRepository.allNotes.filter { note ->
            FolderRepository.allFolders.all { !it.noteIds.contains(note.id) } && note.filterId == null
        }
    else 
        NoteRepository.allNotes.filter { note ->
            FolderRepository.allFolders.all { !it.noteIds.contains(note.id) } && note.filterId == activeFilter
        }
    ModalDrawerSheet {
        Column(
            Modifier
                .width(250.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp) // changed from .padding(all = 8.dp)
        ) {
            // Header: title on left, back button on right
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Notes", modifier = Modifier.weight(1f))
                IconButton(onClick = closeDrawer) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                }
            }
            FilterBar()
            // Modified: Recursive FolderRow with notes appearing above embedded folders and filtering applied.
            @Composable
            fun FolderRow(folder: Folder, indent: Int) {
                val isExpanded = expanded[folder.id] ?: false
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = (indent * 16).dp, top = 4.dp, bottom = 4.dp)
                        .combinedClickable(
                            onClick = { expanded[folder.id] = !isExpanded },
                            onLongClick = {
                                editFolder = folder
                                editName = folder.name
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { expanded[folder.id] = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                    // Folder name: no clickable navigation.
                    Text(folder.name, modifier = Modifier.weight(1f))
                }
                if (isExpanded) {
                    // First: Render filtered notes within this folder.
                    val folderNotes = folder.noteIds.mapNotNull { nid ->
                        NoteRepository.getNote(nid)
                    }.filter { note ->
                        if (activeFilter == null) note.filterId == null else note.filterId == activeFilter
                    }
                    folderNotes.forEach { note ->
                        Text(
                            note.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = ((indent + 1) * 16).dp, top = 2.dp, bottom = 2.dp)
                                .clickable {
                                    closeDrawer()
                                    DrawerInterop.openInTab?.invoke(note.id)
                                        ?: nav.navigate("editor/${note.id}")
                                }
                        )
                    }
                    // Second: Recursively render embedded subfolders.
                    FolderRepository.allFolders.filter { it.parentId == folder.id }
                        .forEach { subFolder -> FolderRow(subFolder, indent + 1) }
                }
            }
            // Render top-level folders.
            folders.forEach { folder ->
                FolderRow(folder, 0)
                Divider()
            }
            // Render notes not contained in any folder.
            notesNoFolder.forEach { note ->
                Text(
                    note.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            closeDrawer()
                            DrawerInterop.openInTab?.invoke(note.id)
                                ?: nav.navigate("editor/${note.id}")
                        }
                )
                Divider()
            }
            // Edit folder dialog remains unchanged:
            editFolder?.let { f ->
                AlertDialog(
                    onDismissRequest = { editFolder = null },
                    title = { Text("Edit Folder") },
                    text = {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") }
                        )
                    },
                    confirmButton = {
                        Row {
                            TextButton(onClick = {
                                FolderRepository.deleteFolder(f.id)
                                editFolder = null
                            }) { Text("Delete") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                f.name = editName
                                FolderRepository.save(f.id)
                                editFolder = null
                            }) { Text("OK") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editFolder = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}