package com.example.stylusdraw

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.stylusdraw.data.FilterRepository
import com.example.stylusdraw.data.DEFAULT_COLOR_OPTIONS
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.random.Random
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterBar() {
    val filters = FilterRepository.allFilters
    val active = FilterRepository.activeFilterId.value
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var menuTarget by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
        ) {
            filters.forEach { filter ->
                val selected = active == filter.id
                Box {
                    AssistChip(
                        onClick = {
                            FilterRepository.activeFilterId.value = if (selected) null else filter.id
                        },
                        label = { Text(filter.name) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClick = { menuTarget = filter.id },
                                onClick = {}
                            ),
                        border = BorderStroke(2.dp, Color(filter.color)),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) Color(filter.color).copy(alpha = 0.2f) else Color.Transparent
                        )
                    )
                    DropdownMenu(
                        expanded = menuTarget == filter.id,
                        onDismissRequest = { menuTarget = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                renameTarget = filter.id
                                renameName = filter.name
                                menuTarget = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                FilterRepository.deleteFilter(filter.id)
                                if (FilterRepository.activeFilterId.value == filter.id) {
                                    FilterRepository.activeFilterId.value = null
                                }
                                menuTarget = null
                            }
                        )
                    }
                }
            }
            if (filters.size < 10) {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Filter")
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New Filter") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val color = DEFAULT_COLOR_OPTIONS[Random.nextInt(DEFAULT_COLOR_OPTIONS.size)]
                    FilterRepository.createFilter(newName.ifBlank { "Filter" }, color)
                    newName = ""
                    showAdd = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }

    renameTarget?.let { id ->
        val filter = FilterRepository.getFilter(id)!!
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Filter") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    filter.name = renameName
                    FilterRepository.save(filter.id)
                    renameTarget = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}
