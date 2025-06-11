package com.example.stylusdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.stylusdraw.data.Note
import com.example.stylusdraw.data.Page
import com.example.stylusdraw.data.NoteRepository
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import androidx.compose.material3.Divider
import androidx.compose.material.icons.filled.Close

@Composable
fun PageMenu(
    note: Note,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Column(
        Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFFF0F0F0))
            .padding(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                note.pages.add(Page())
                NoteRepository.save(note.id)
                scope.launch { pagerState.scrollToPage(note.pages.lastIndex) }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Page")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(note.pages) { index, _ ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${index + 1}",
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                scope.launch { pagerState.scrollToPage(index) }
                                onClose()
                            }
                    )
                    IconButton(onClick = {
                        if (index > 0) {
                            note.pages.add(index - 1, note.pages.removeAt(index))
                            NoteRepository.save(note.id)
                            scope.launch { pagerState.scrollToPage(index - 1) }
                        }
                    }, enabled = index > 0) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                    }
                    IconButton(onClick = {
                        if (index < note.pages.lastIndex) {
                            note.pages.add(index + 1, note.pages.removeAt(index))
                            NoteRepository.save(note.id)
                            scope.launch { pagerState.scrollToPage(index + 1) }
                        }
                    }, enabled = index < note.pages.lastIndex) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Down")
                    }
                    IconButton(onClick = {
                        if (note.pages.size > 1) {
                            note.pages.removeAt(index)
                            NoteRepository.save(note.id)
                            val newIndex = index.coerceAtMost(note.pages.lastIndex)
                            scope.launch { pagerState.scrollToPage(newIndex) }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                Divider()
            }
        }
    }
}
