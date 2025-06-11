// File: app/src/main/java/com/example/stylusdraw/NoteScreen.kt
package com.example.stylusdraw

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.ink.authoring.InProgressStrokesView
import androidx.navigation.NavController
import com.example.stylusdraw.data.LayoutMode
import com.example.stylusdraw.data.NoteRepository
import com.example.stylusdraw.data.Page
import com.example.stylusdraw.data.SettingsRepository
import com.example.stylusdraw.PAGE_SPACING
import com.example.stylusdraw.data.HistoryAction
import com.example.stylusdraw.PdfExport
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import com.example.stylusdraw.StrokeCanvas
import com.example.stylusdraw.ViewMatrixManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    nav: NavController,
    id: String,
    // Non-null if TabbedEditor requested a long‐press rename for this note
    renameTrigger: String?,
    // Called when rename dialog is dismissed (OK or Cancel)
    onRenameDone: () -> Unit,
    onMenu: () -> Unit
) {
    // Look up the note (must exist)
    val note = remember(id) { NoteRepository.getNote(id)!! }
    val context = LocalContext.current

    // === PER‐NOTE STATE (keyed on id) ===
    val tools = remember(id) { mutableStateListOf(true, false, false, false) }

    val settings = SettingsRepository
    var thickness by settings.drawThickness
    var eraser by settings.eraserRadius
    val palette = settings.palette
    var currentIdx by settings.currentColorIndex
    fun currentColor(): Color = settings.currentColor()

    val renderer =
        remember(id) { androidx.ink.rendering.android.canvas.CanvasStrokeRenderer.create() }
    val strokeMap =
        remember(id) { mutableMapOf<androidx.ink.authoring.InProgressStrokeId, androidx.ink.strokes.Stroke>() }

    var penDown by remember(id) { mutableStateOf(false) }
    var boxSize by remember(id) { mutableStateOf(IntSize.Zero) }
    var dismissKey by remember { mutableStateOf(0) }
    val viewMatrixManager = remember { ViewMatrixManager() }

    // Pager state for “page” layout, reset on note.id change
    val pager = rememberPagerState(
        initialPage = note.currentPage,
        pageCount = { note.pages.size }
    )

    // === RENAME‐DIALOG STATE (inside this composable) ===
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(note.title) }

    // If parent says “longPressRenameFor == this note’s ID,” open rename dialog:
    LaunchedEffect(renameTrigger) {
        if (renameTrigger == id) {
            renameText = note.title
            showRenameDialog = true
        }
    }

    /** Undo the most recent stroke on the current page, if any. */
    fun undo() {
        val pageIdx = if (note.layout == LayoutMode.PAGES) pager.currentPage else 0
        val page = note.pages[pageIdx]
        if (page.history.isNotEmpty()) {
            val action = page.history.removeAt(page.history.lastIndex)
            when (action) {
                is HistoryAction.Add -> page.strokes.remove(action.stroke)
                is HistoryAction.Delete -> page.strokes.add(action.stroke)
            }
            page.future.add(action)
            NoteRepository.save(note.id)
        }
    }

    /** Redo the most recently undone stroke on the current page, if any. */
    fun redo() {
        val pageIdx = if (note.layout == LayoutMode.PAGES) pager.currentPage else 0
        val page = note.pages[pageIdx]
        if (page.future.isNotEmpty()) {
            val action = page.future.removeAt(page.future.lastIndex)
            when (action) {
                is HistoryAction.Add -> page.strokes.add(action.stroke)
                is HistoryAction.Delete -> page.strokes.remove(action.stroke)
            }
            page.history.add(action)
            NoteRepository.save(note.id)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showPageDrawer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showPageDrawer) {
        if (showPageDrawer) drawerState.open() else drawerState.close()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                PageMenu(
                    note = note,
                    pagerState = pager,
                    onClose = { showPageDrawer = false }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopBar(
                        title = note.title,
                        hasContent = note.pages.any { it.strokes.isNotEmpty() },
                        onMenu = onMenu,
                        onBack = { nav.popBackStack() },
                        onExport = { PdfExport.export(context, note) },
                        onSettings = { /* settings logic */ },
                        onAddPage = {
                            note.pages += Page()
                            NoteRepository.save(note.id)
                            scope.launch { pager.scrollToPage(note.pages.lastIndex) }
                        },
                        onPageLayout = {
                            if (note.layout != LayoutMode.PAGES) note.layout = LayoutMode.PAGES
                            else showPageDrawer = true
                        },
                        onTitleClick = {
                            renameText = note.title
                            showRenameDialog = true
                        }
                    )
                }
            ) { padding ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .onSizeChanged { boxSize = it }
                ) {
                    val density = LocalDensity.current
                    val screenW = with(density) { boxSize.width.toDp() }
                    val screenH = with(density) { boxSize.height.toDp() }
                    val sidebarW = 40.dp
                    val topbarH = 40.dp

                    val isPortrait = screenH >= screenW
                    val baseScale: Float
                    val pageWidthDp: Dp
                    val pageHeightDp: Dp
                    val pagePaddingStart: Dp
                    val pagePaddingTop: Dp

                    if (isPortrait) {
                        val availW = screenW - sidebarW
                        baseScale = availW / PAGE_WIDTH
                        pageWidthDp = PAGE_WIDTH * baseScale
                        pageHeightDp = PAGE_HEIGHT * baseScale
                        pagePaddingStart = sidebarW
                        pagePaddingTop = (screenH - pageHeightDp) / 2f
                    } else {
                        val availH = screenH - topbarH
                        baseScale = availH / PAGE_HEIGHT
                        pageWidthDp = PAGE_WIDTH * baseScale
                        pageHeightDp = PAGE_HEIGHT * baseScale
                        pagePaddingStart = sidebarW + ((screenW - sidebarW - pageWidthDp) / 2f)
                        pagePaddingTop = topbarH
                    }


                    when (note.layout) {
                        LayoutMode.PAGES -> {
                            HorizontalPager(
                                state = pager,
                                // Disable page swipes while a stylus stroke is in progress
                                // but allow normal finger swipes otherwise
                                userScrollEnabled = !penDown &&
                                    viewMatrixManager.scale == 1f,
                                pageSpacing = PAGE_SPACING
                            ) { pageIdx ->
                                // Auto‐append new page if you scroll beyond the last page

                                PageLayout(
                                    viewMatrixManager = viewMatrixManager,
                                    modifier = Modifier
                                        .padding(start = pagePaddingStart, top = pagePaddingTop)
                                        .size(pageWidthDp, pageHeightDp)
                                ) {
                                    if (pageIdx == pager.currentPage) {
                                        DrawingSurface(
                                            strokes = note.pages[pageIdx].strokes,
                                            history = note.pages[pageIdx].history,
                                            future = note.pages[pageIdx].future,
                                            strokeIdMap = strokeMap,
                                            renderer = renderer,
                                            getSelectedTool = { tools.indexOfFirst { it } },
                                            setSelectedTool = { i ->
                                                tools.forEachIndexed { idx, _ ->
                                                    tools[idx] = idx == i
                                                }
                                            },
                                            getDrawThickness = { thickness },
                                            getEraserRadius = { eraser },
                                            getCurrentColor = { currentColor() },
                                            noteId = note.id,
                                            viewMatrixManager = viewMatrixManager,
                                            onStylusDown = { penDown = true },
                                            onStylusUp = { penDown = false }
                                        )
                                    } else {
                                        StrokeCanvas(
                                            strokes = note.pages[pageIdx].strokes,
                                            renderer = renderer
                                        )
                                    }
                                }
                            }

                            // Reset pager to the note’s saved currentPage whenever this note appears:
                            LaunchedEffect(key1 = note.id) {
                                pager.scrollToPage(note.currentPage)
                            }
                        }

                        LayoutMode.INFINITE_DOWN -> {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                DrawingSurface(
                                    strokes = note.pages[0].strokes,
                                    history = note.pages[0].history,
                                    future = note.pages[0].future,
                                    strokeIdMap = strokeMap,
                                    renderer = renderer,
                                    getSelectedTool = { tools.indexOfFirst { it } },
                                    setSelectedTool = { i ->
                                        tools.forEachIndexed { idx, _ ->
                                            tools[idx] = idx == i
                                        }
                                    },
                                    getDrawThickness = { thickness },
                                    getEraserRadius = { eraser },
                                    getCurrentColor = { currentColor() },
                                    noteId = note.id,
                                    viewMatrixManager = viewMatrixManager,
                                    onStylusDown = { penDown = true; dismissKey++ },
                                    onStylusUp = { penDown = false }
                                )
                            }
                        }

                        LayoutMode.INFINITE -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .verticalScroll(rememberScrollState())
                            ) {
                                DrawingSurface(
                                    strokes = note.pages[0].strokes,
                                    strokeIdMap = strokeMap,
                                    renderer = renderer,
                                    getSelectedTool = { tools.indexOfFirst { it } },
                                    setSelectedTool = { i ->
                                        tools.forEachIndexed { idx, _ ->
                                            tools[idx] = idx == i
                                        }
                                    },
                                    getDrawThickness = { thickness },
                                    getEraserRadius = { eraser },
                                    getCurrentColor = { currentColor() },
                                    noteId = note.id,
                                    viewMatrixManager = viewMatrixManager,
                                    onStylusDown = { penDown = true; dismissKey++ },
                                    history = note.pages[0].history,
                                    future = note.pages[0].future,
                                    onStylusUp = { penDown = false }
                                )
                            }
                        }
                    }

                    // Sidebar (tools selector, unchanged)
                    Sidebar(
                        originalWidthDp = 40.dp,
                        boxSize = boxSize,
                        animationDurationMs = 300,
                        selectedTool = tools,
                        drawThickness = thickness,
                        onDrawThicknessChange = {
                            thickness = it.coerceAtLeast(1f)
                            settings.save()
                        },
                        eraserRadius = eraser,
                        onEraserRadiusChange = {
                            eraser = it.coerceAtLeast(1f)
                            settings.save()
                        },
                        colorSlots = palette,
                        visibleSlots = settings.visibleColorSlots.value,
                        getCurrentColor = { currentColor() },
                        onSlotSelected = { idx ->
                            settings.setCurrentColorIndex(idx)
                        },
                        onSlotColorChanged = { idx, col ->
                            settings.setPaletteColor(idx, col)
                        },
                        dismissKey = dismissKey,
                        onForward = { redo() },
                        onReverse = { undo() }
                    )
                }
            }
        }
        // === RENAME DIALOG ===
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false; onRenameDone() },
                title = { Text("Rename Note") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        // Immediately update note title on value change:
                        onValueChange = { newText ->
                            renameText = newText
                            note.title = newText
                            NoteRepository.save(note.id)
                        },
                        label = { Text("New Title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        // (Optional additional action)
                        showRenameDialog = false
                        onRenameDone()
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRenameDialog = false
                        onRenameDone()
                    }) { Text("Cancel") }
                }
            )
        }

        // Android Back button handling
        BackHandler { nav.popBackStack("home", false) }
    }
}
