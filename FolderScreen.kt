package com.example.stylusdraw

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.FolderRepository
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import com.example.stylusdraw.FilterBar
import com.example.stylusdraw.data.FilterRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.example.stylusdraw.data.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderScreen(nav: NavController, folderId: String, onMenu: () -> Unit) {
    val folder = remember(folderId) { FolderRepository.getFolder(folderId)!! }
    val activeFilter by FilterRepository.activeFilterId
    val notes = if (activeFilter == null)
                    NoteRepository.allNotes.filter { it.id in folder.noteIds && it.filterId == null }
                else
                    NoteRepository.allNotes.filter { it.id in folder.noteIds && it.filterId == activeFilter }
    val subfolders = FolderRepository.allFolders.filter { it.parentId == folder.id }
    var folderOption by remember { mutableStateOf<Folder?>(null) }
    var folderRenameName by remember { mutableStateOf("") }
    var noteOption by remember { mutableStateOf<com.example.stylusdraw.data.Note?>(null) }
    var noteRenameName by remember { mutableStateOf("") }
    var showAddMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val selectedNotes = remember { mutableStateListOf<String>() }
    var showNoteFilterMenuForNote by remember { mutableStateOf<String?>(null) }
    // NEW: refreshTrigger to force recomposition on exit from popups.
    var refreshTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(refreshTrigger) { /* triggers refresh when incremented */ }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedNotes.isNotEmpty()) Text("${selectedNotes.size} selected")
                    else Text(folder.name)
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                        IconButton(onClick = {
                            if (folder.parentId != null) nav.navigate("folder/${folder.parentId}")
                            else nav.popBackStack("home", false)
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                },
                actions = {
                    if (selectedNotes.isNotEmpty()) {
                        IconButton(onClick = {
                            selectedNotes.forEach { noteId ->
                                FolderRepository.removeNoteFromFolder(noteId, folder.id)
                                NoteRepository.deleteNote(noteId)
                            }
                            selectedNotes.clear()
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete Selected") }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Box {
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New")
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(text = { Text("New Note") }, onClick = {
                            val note = NoteRepository.createNote("Untitled", activeFilter)
                            FolderRepository.addNoteToFolder(note.id, folder.id)
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
                        onDismissRequest = { showFilterMenu = false }) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (subfolders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Folders",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            items(
                                items = subfolders,
                                // Changed: Use both parent folder ID and subfolder index
                                key = { subfolder -> "folder_${folder.id}_${subfolders.indexOf(subfolder)}_${subfolder.id}" }
                            ) { subfolder ->
                                FolderItem(
                                    folder = subfolder,
                                    onClick = { nav.navigate("folder/${subfolder.id}") },
                                    onLongClick = {
                                        folderOption = subfolder
                                        folderRenameName = subfolder.name
                                    },
                                    isSelected = false, // no multi-select
                                    subFolders = FolderRepository.allFolders.filter { it.parentId == subfolder.id }
                                )
                            }
                        }
                    }
                }
                if (notes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    items(notes, key = { it.id }) { note ->
                        ListItem(
                            headlineContent = { Text(note.title) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Anchor Box for filter circle with its dropdown.
                                    Box {
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isPressed by interactionSource.collectIsPressedAsState()
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
                                        if (showNoteFilterMenuForNote == note.id) {
                                            DropdownMenu(
                                                expanded = true,
                                                onDismissRequest = { showNoteFilterMenuForNote = null },
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
        }
        // Folder Options AlertDialog (for renaming folder) update on dismiss.
        folderOption?.let { folder ->
            AlertDialog(
                onDismissRequest = {
                    folder.name = folderRenameName
                    FolderRepository.save(folder.id)
                    folderOption = null
                },
                title = { Text("Rename Folder") },
                text = {
                    OutlinedTextField(
                        value = folderRenameName,
                        onValueChange = { newText ->
                            folderRenameName = newText
                        },
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
        // Note Options AlertDialog (unchanged)
        noteOption?.let { note ->
            AlertDialog(
                onDismissRequest = { noteOption = null },
                title = { Text("Note Options") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = noteRenameName,
                            onValueChange = { noteRenameName = it },
                            label = { Text("Rename Note") }
                        )
                    }
                },
                confirmButton = { /* no default confirmButton */ },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            FolderRepository.removeNoteFromFolder(note.id, folder.id)
                            NoteRepository.deleteNote(note.id)
                            noteOption = null
                        }) { Text("Delete") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
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
                        val newFolder = FolderRepository.createFolder(
                            newFolderName.ifBlank { "Folder" },
                            parentId = folder.id
                        )
                        showNewFolderDialog = false
                        newFolderName = ""
                        nav.navigate("folder/${newFolder.id}")
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                },
                title = { Text("Folder Name") },
                text = {
                    TextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Enter folder name") }
                    )
                }
            )
        }
    }
}