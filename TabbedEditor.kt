package com.example.stylusdraw

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.FolderRepository
import com.example.stylusdraw.DrawerInterop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TabbedEditor(nav: NavController, startId: String, openDrawer: () -> Unit) {
    val repo      = NoteRepository
    val openTabs  = remember(startId) { mutableStateListOf(startId) }
    var currentId by remember { mutableStateOf(startId) }
    var optionsFor by remember { mutableStateOf<String?>(null) }
    var longPressRenameFor by remember { mutableStateOf<String?>(null) }
    var showMenu   by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        DrawerInterop.openInTab = { noteId ->
            if (noteId !in openTabs) {
                val insert = openTabs.indexOf(currentId) + 1
                openTabs.add(insert, noteId)
            }
            currentId = noteId
        }
        onDispose { DrawerInterop.openInTab = null }
    }
    Column {
        ScrollableTabRow(
            selectedTabIndex = openTabs.indexOf(currentId).coerceAtLeast(0)
        ) {
            openTabs.forEach { id ->
                val note = repo.getNote(id) ?: return@forEach
                Tab(
                    selected = (id == currentId),
                    onClick  = { currentId = id },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .combinedClickable(
                            onClick     = { currentId = id },
                            onLongClick = { optionsFor = id }
                        ),
                    text     = {
                        Text(
                            text     = note.title,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 2.dp),
                            color    = if (id == currentId) Color.Black else Color.Gray,
                            style    = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                        )
                    }
                )
            }

            // If fewer than 10 tabs, show a “+” tab for Create/Attach
            if (openTabs.size < 10) {
                Box {
                    Tab(
                        selected = false,
                        onClick  = { showMenu = true },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .padding(WindowInsets.statusBars.asPaddingValues()),
                        icon     = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New/Attach",
                                tint = if (showMenu) MaterialTheme.colorScheme.secondary else Color.Gray
                            )
                        }
                    )
                    DropdownMenu(
                        expanded         = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier         = Modifier.align(Alignment.TopCenter)
                    ) {
                        DropdownMenuItem(
                            text    = { Text("Create New") },
                            onClick = {
                                val currentNote = repo.getNote(currentId)
                                // Lookup the folder for the current note using FolderRepository
                                val folder = currentNote?.let { FolderRepository.folderForNote(it.id) }
                                val newNote = repo.createNote("Untitled")
                                if (folder != null) {
                                    FolderRepository.addNoteToFolder(newNote.id, folder.id)
                                }
                                openTabs.add(newNote.id) // add new note only once
                                currentId = newNote.id
                                showMenu  = false
                            }
                        )
                        DropdownMenuItem(
                            text    = { Text("Attach Existing") },
                            onClick = {
                                openDrawer()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Long‐press “Tab Options” dialog
        optionsFor?.let { id ->
            AlertDialog(
                onDismissRequest = { optionsFor = null },
                title            = { Text("Tab Options") },
                text             = { Text("Delete, Unattach, or Rename this tab?") },
                confirmButton    = {
                    TextButton(onClick = {
                        repo.deleteNote(id)
                        openTabs.remove(id)
                        if (currentId == id) {
                            currentId = openTabs.firstOrNull() ?: run {
                                nav.popBackStack("home", false)
                                ""
                            }
                        }
                        optionsFor = null
                    }) { Text("Delete") }
                },
                dismissButton    = {
                    Row {
                        TextButton(onClick = {
                            openTabs.remove(id)
                            if (currentId == id) {
                                currentId = openTabs.firstOrNull() ?: run {
                                    nav.popBackStack("home", false)
                                    ""
                                }
                            }
                            optionsFor = null
                        }) { Text("Unattach") }
                        Spacer(Modifier.width(16.dp))
                        TextButton(onClick = {
                            longPressRenameFor = id
                            optionsFor         = null
                        }) { Text("Rename") }
                    }
                }
            )
        }

        key(currentId) {
            NoteScreen(
                nav           = nav,
                id            = currentId,
                renameTrigger = longPressRenameFor,
                onRenameDone  = { longPressRenameFor = null },
                onMenu        = openDrawer
            )
        }

        // Minimal content to avoid missing reference in MainActivity
        Text("Editor for note: $startId")
    }
}