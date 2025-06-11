// File: `app/src/main/java/com/example/stylusdraw/HomeScreen.kt`
package com.example.stylusdraw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.stylusdraw.data.Folder
import com.example.stylusdraw.data.FolderRepository
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.FilterRepository
import com.example.stylusdraw.FilterBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlin.collections.remove
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(nav: NavController, onMenu: () -> Unit) {
    // Modified: Only include folders with no parent.
    val folders = FolderRepository.allFolders.filter { it.parentId == null }
    val activeFilter by FilterRepository.activeFilterId
    // Added: Gather note IDs contained in any folder.
    val folderNoteIds = FolderRepository.allFolders.flatMap { it.noteIds }.toSet()
    // Modified: Filter out notes contained in any folder.
    val notes = if (activeFilter == null)
        NoteRepository.allNotes.filter { it.id !in folderNoteIds && it.filterId == null }
    else
        NoteRepository.allNotes.filter { it.id !in folderNoteIds && it.filterId == activeFilter }
    // Removed multi-select state:
    // val selected = remember { mutableStateListOf<String>() }
    // Added state for long-press options:
    var folderOption by remember { mutableStateOf<Folder?>(null) }
    var folderRenameName by remember { mutableStateOf("") }
    var noteOption by remember { mutableStateOf<com.example.stylusdraw.data.Note?>(null) }
    var noteRenameName by remember { mutableStateOf("") }
    var showAddMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false)}
    // Removed: var newItemFilter by remember { mutableStateOf<String?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val selectedNotes = remember { mutableStateListOf<String>() }
    var showNoteFilterMenuForNote by remember { mutableStateOf<String?>(null) }  // NEW state
    // NEW: refreshTrigger forces recomposition after popups exit.
    var refreshTrigger by remember { mutableStateOf(0) }
    // Use refreshTrigger in a dummy read to trigger recomposition.
    LaunchedEffect(refreshTrigger) { /* triggers refresh when incremented */ }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedNotes.isNotEmpty()) Text("${selectedNotes.size} selected")
                    else Text("My Notes")
                },
                navigationIcon = {
                    IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    if (selectedNotes.isNotEmpty()) {
                        IconButton(onClick = {
                            selectedNotes.forEach { noteId ->
                                NoteRepository.deleteNote(noteId)
                            }
                            selectedNotes.clear()
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete Selected") }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New")
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("New Note") }, onClick = {
                            val note = NoteRepository.createNote("Untitled", activeFilter)
                            showAddMenu = false
                            nav.navigate("editor/${note.id}")
                        })
                        DropdownMenuItem(text = { Text("New Folder") }, onClick = {
                            showAddMenu = false
                            showNewFolderDialog = true
                        })
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    FloatingActionButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        val color = FilterRepository.getFilter(activeFilter)?.color ?: 0
                        Box(Modifier.size(24.dp).background(Color(color), shape = CircleShape))
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("None") }, onClick = {
                            FilterRepository.activeFilterId.value = null
                            showFilterMenu = false
                        })
                        FilterRepository.allFilters.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.name) },
                                onClick = {
                                    FilterRepository.activeFilterId.value = filter.id
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Box(
                                        Modifier.size(16.dp)
                                            .background(Color(filter.color), shape = CircleShape)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FilterBar()
            // Horizontal Folders Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(50.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = folders,
                    // Changed: Use folder index in list as part of the key
                    key = { folder -> "home_${folders.indexOf(folder)}_${folder.id}" }
                ) { folder ->
                    FolderItem(
                        folder = folder,
                        onClick = { nav.navigate("folder/${folder.id}") },
                        onLongClick = {
                            folderOption = folder
                            folderRenameName = folder.name
                        },
                        isSelected = false, // no multi-select for folder items
                        subFolders = FolderRepository.allFolders.filter { it.parentId == folder.id }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // Vertical list for Notes only
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notes, key = { it.id }) { note ->
                    ListItem(
                        headlineContent = { Text(note.title) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Anchor Box for filter circle and its dropdown menu.
                                Box {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    // Filter circle with border highlight when pressed or active.
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(
                                                BorderStroke(
                                                    if (isPressed || showNoteFilterMenuForNote == note.id) 2.dp else 1.dp,
                                                    if (isPressed || showNoteFilterMenuForNote == note.id) Color.Blue else Color.Black
                                                ),
                                                shape = CircleShape
                                            )
                                            .background(Color(FilterRepository.getFilter(note.filterId)?.color ?: 0), shape = CircleShape)
                                            .clickable(indication = null, interactionSource = interactionSource) {
                                                showNoteFilterMenuForNote = note.id
                                            }
                                    )
                                    // DropdownMenu anchored directly below the filter circle.
                                    if (showNoteFilterMenuForNote == note.id) {
                                        DropdownMenu(
                                            expanded = true,
                                            onDismissRequest = { showNoteFilterMenuForNote = null },
                                            // Align it directly below by aligning to BottomStart of the anchor Box.
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            DropdownMenuItem(text = { Text("None") }, onClick = {
                                                note.filterId = null
                                                NoteRepository.save(note.id)
                                                showNoteFilterMenuForNote = null
                                                refreshTrigger++
                                            })
                                            FilterRepository.allFilters.forEach { filter ->
                                                DropdownMenuItem(
                                                    text = { Text(filter.name) },
                                                    onClick = {
                                                        note.filterId = filter.id
                                                        NoteRepository.save(note.id)
                                                        showNoteFilterMenuForNote = null
                                                        refreshTrigger++
                                                    },
                                                    leadingIcon = {
                                                        Box(
                                                            Modifier
                                                                .size(16.dp)
                                                                .background(Color(filter.color), shape = CircleShape)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                // Selector checkbox remains unchanged.
                                Checkbox(
                                    checked = selectedNotes.contains(note.id),
                                    onCheckedChange = {
                                        if (it) selectedNotes.add(note.id) else selectedNotes.remove(note.id)
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedNotes.isNotEmpty()) {
                                        if (selectedNotes.contains(note.id)) selectedNotes.remove(note.id)
                                        else selectedNotes.add(note.id)
                                    } else {
                                        nav.navigate("editor/${note.id}")
                                    }
                                },
                                onLongClick = {
                                    noteOption = note
                                    noteRenameName = note.title
                                }
                            )
                            .padding(vertical = 4.dp)
                    )
                    Divider()
                }
            }
        }
        // Folder Options AlertDialog (updated with active text box for renaming)
        folderOption?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderOption = null },
                title = { Text("Rename Folder") },
                text = {
                    OutlinedTextField(
                        value = folderRenameName,
                        onValueChange = { folderRenameName = it },
                        label = { Text("New Folder Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        folder.name = folderRenameName
                        FolderRepository.save(folder.id)
                        folderOption = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            FolderRepository.deleteFolder(folder.id)
                            folderOption = null
                        }) { Text("Delete") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { folderOption = null }) { Text("Cancel") }
                    }
                }
            )
        }
        // Note Options AlertDialog
        noteOption?.let { note ->
            AlertDialog(
                 onDismissRequest = {
                     note.title = noteRenameName
                     NoteRepository.save(note.id)
                     noteOption = null
                 },
                 title = { Text("Note Options") },
                 text = {
                    Column {
                       OutlinedTextField(
                           value = noteRenameName,
                           onValueChange = { newText ->
                               noteRenameName = newText
                           },
                           label = { Text("Rename Note") }
                       )
                    }
                 },
                 confirmButton = { /* no default confirmButton needed */ },
                 dismissButton = {
                     Row {
                        TextButton(onClick = {
                            NoteRepository.deleteNote(note.id)
                            noteOption = null
                        }) { Text("Delete") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            // Save changes on "Save":
                            note.title = noteRenameName
                            NoteRepository.save(note.id)
                            noteOption = null
                        }) { Text("Save") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { noteOption = null }) { Text("Cancel") }
                     }
                 }
            )
        }
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val folder = FolderRepository.createFolder(newFolderName.ifBlank { "Folder" })
                        showNewFolderDialog = false
                        newFolderName = ""
                        nav.navigate("folder/${folder.id}")
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                },
                title = { Text("Folder Name") },
                text = { TextField(value = newFolderName, onValueChange = { newFolderName = it }) }
            )
        }
    }
}