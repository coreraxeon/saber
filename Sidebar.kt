// File: app/src/main/java/com/example/stylusdraw/Sidebar.kt
package com.example.stylusdraw

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.IntOffset
import com.example.stylusdraw.data.LayoutMode
import kotlin.math.roundToInt
import com.example.stylusdraw.data.DEFAULT_COLOR_OPTIONS

@Composable
fun Sidebar(
    originalWidthDp: Dp,
    boxSize: IntSize,
    animationDurationMs: Int,
    selectedTool: List<Boolean>,
    drawThickness: Float,
    onDrawThicknessChange: (Float) -> Unit,
    eraserRadius: Float,
    onEraserRadiusChange: (Float) -> Unit,
    colorSlots: List<Color>,
    visibleSlots: Int = 5,
    getCurrentColor: () -> Color,
    onSlotSelected: (Int) -> Unit,
    onSlotColorChanged: (Int, Color) -> Unit,
    dismissKey: Int = 0,
    onForward: () -> Unit = {},
    onReverse: () -> Unit = {}
) {
    val funcIcons = listOf(
        R.drawable.ic_draw,
        R.drawable.ic_erase,
        R.drawable.ic_select,
        R.drawable.ic_highlight
    )
    val tailIcons = listOf(
        R.drawable.ic_forward,
        R.drawable.ic_reverse,
        R.drawable.ic_circuit,
        R.drawable.ic_more
    )

    val density   = LocalDensity.current
    val screenWdp = with(density) { boxSize.width.toDp() }
    val availHdp  = with(density) { boxSize.height.toDp() }

    var offsetX      by remember { mutableStateOf(0.dp) }
    var inReposition by remember { mutableStateOf(false) }

    var showDrawCtrl   by remember { mutableStateOf(false) }
    var showEraserCtrl by remember { mutableStateOf(false) }
    var drawBtnCenter  by remember { mutableStateOf(0.dp) }
    var eraserBtnCenter by remember { mutableStateOf(0.dp) }
    var colorPickerFor by remember { mutableStateOf<Int?>(null) }
    val colorCenters = remember { mutableStateMapOf<Int, Dp>() }

    val targetW = if (inReposition) originalWidthDp * .8f else originalWidthDp
    val targetH = if (inReposition) availHdp * .6f else availHdp
    val animW by animateDpAsState(targetW, tween(animationDurationMs))
    val animH by animateDpAsState(targetH, tween(animationDurationMs))

    val btnSize = animW * .7f
    val isOnRight = offsetX > screenWdp / 2

    var lastDismissKey by remember { mutableStateOf(dismissKey) }
    if (dismissKey != lastDismissKey) {
        showDrawCtrl = false
        showEraserCtrl = false
        colorPickerFor = null
        lastDismissKey = dismissKey
    }

    Box(
        Modifier
            .offset(x = offsetX)
            .width(animW)
            .height(animH)
            .background(Color(0xFFF4F4F4), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart  = { inReposition = true },
                    onDrag       = { _, drag -> offsetX += with(density) { drag.x.toDp() } },
                    onDragEnd    = {
                        inReposition = false
                        offsetX =
                            if (offsetX + originalWidthDp / 2 < screenWdp / 2)
                                0.dp
                            else screenWdp - originalWidthDp
                    },
                    onDragCancel = { inReposition = false }
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Function icons
            funcIcons.forEachIndexed { idx, iconRes ->
                val isSelected = selectedTool.getOrNull(idx) == true
                Box(
                    Modifier
                        .size(btnSize)
                        .background(
                            if (isSelected) Color.Gray.copy(alpha = .3f) else Color.Transparent,
                            CircleShape
                        )
                        .onGloballyPositioned { coords ->
                            val center = with(density) { (coords.positionInParent().y + coords.size.height / 2).toDp() }
                            if (idx == 0) drawBtnCenter = center
                            if (idx == 1) eraserBtnCenter = center
                        }
                        .pointerInput(idx) {
                            detectTapGestures {
                                val already = selectedTool.getOrNull(idx) == true
                                (selectedTool as MutableList<Boolean>).forEachIndexed { j, _ ->
                                    selectedTool[j] = j == idx
                                }
                                when (idx) {
                                    0 -> {
                                        if (already) showDrawCtrl = !showDrawCtrl else {
                                            showDrawCtrl = false
                                            showEraserCtrl = false
                                            colorPickerFor = null
                                        }
                                    }
                                    1 -> {
                                        if (already) showEraserCtrl = !showEraserCtrl else {
                                            showDrawCtrl = false
                                            showEraserCtrl = false
                                            colorPickerFor = null
                                        }
                                    }
                                    else -> {
                                        showDrawCtrl = false
                                        showEraserCtrl = false
                                        colorPickerFor = null
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(iconRes), contentDescription = null)
                }
            }

            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(animW * .8f).height(2.dp).background(Color.Black))
            Spacer(Modifier.height(4.dp))

            // Color slots
            val shownSlots = visibleSlots.coerceIn(1, colorSlots.size)
            for (i in 0 until shownSlots) {
                val color = colorSlots[i]
                val isCurrent = color == getCurrentColor()
                Box(
                    Modifier
                        .size(btnSize)
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = Color.DarkGray,
                            shape = CircleShape
                        )
                        .onGloballyPositioned { coords ->
                            val center = with(density) { (coords.positionInParent().y + coords.size.height / 2).toDp() }
                            colorCenters[i] = center
                        }
                        .pointerInput(i, color) {
                            detectTapGestures {
                                if (getCurrentColor() != color) {
                                    onSlotSelected(i)
                                    showDrawCtrl = false
                                    showEraserCtrl = false
                                    colorPickerFor = null
                                } else {
                                    colorPickerFor = if (colorPickerFor == i) null else i
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(btnSize * .6f)
                            .background(color, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(animW * .8f).height(2.dp).background(Color.Black))
            Spacer(Modifier.height(4.dp))

            // Tail icons (forward/rewind/etc.)
            tailIcons.forEachIndexed { idx, iconRes ->
                Box(
                    Modifier
                        .size(btnSize)
                        .pointerInput(idx) {
                            detectTapGestures {
                                showDrawCtrl = false
                                showEraserCtrl = false
                                colorPickerFor = null
                                when (idx) {
                                    0 -> onForward()
                                    1 -> onReverse()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(iconRes), contentDescription = null)
                }
            }
        }

        // Thickness popup
        if (showDrawCtrl) {
            ControlPopup(
                sidebarWidth = animW,
                popupOnLeft  = isOnRight,
                buttonSize   = btnSize,
                centerY      = drawBtnCenter,
                current      = drawThickness.roundToInt(),
                onIncrement  = { onDrawThicknessChange((drawThickness + 1f).coerceAtMost(100f)) },
                onDecrement  = { onDrawThicknessChange((drawThickness - 1f).coerceAtLeast(1f)) }
            )
        }

        // Eraser popup
        if (showEraserCtrl) {
            ControlPopup(
                sidebarWidth = animW,
                popupOnLeft  = isOnRight,
                buttonSize   = btnSize,
                centerY      = eraserBtnCenter,
                current      = eraserRadius.roundToInt(),
                onIncrement  = { onEraserRadiusChange((eraserRadius + 1f).coerceAtMost(100f)) },
                onDecrement  = { onEraserRadiusChange((eraserRadius - 1f).coerceAtLeast(1f)) }
            )
        }

        colorPickerFor?.let { idx ->
            ColorPopup(
                sidebarWidth = animW,
                popupOnLeft  = isOnRight,
                buttonSize   = btnSize,
                centerY      = colorCenters[idx] ?: 0.dp,
                onColorSelected = { col ->
                    onSlotColorChanged(idx, col)
                    onSlotSelected(idx)
                    colorPickerFor = null
                }
            )
        }
    }
}

@Composable
private fun ControlPopup(
    sidebarWidth: Dp,
    popupOnLeft: Boolean,
    buttonSize: Dp,
    centerY: Dp,
    current: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val density = LocalDensity.current
    val offsetX = with(density) {
        if (popupOnLeft) -(buttonSize * 4 + 8.dp).roundToPx() else (sidebarWidth + 8.dp).roundToPx()
    }
    val offsetY = with(density) { (centerY - buttonSize / 2).roundToPx() }

    Popup(
        offset = IntOffset(offsetX, offsetY),
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Row(
                Modifier
                    .widthIn(min = buttonSize * 4)
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDecrement, Modifier.size(32.dp)) {
                    Icon(painterResource(R.drawable.ic_minus), contentDescription = "Decrement")
                }
                Text(
                    text = current.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onIncrement, Modifier.size(32.dp)) {
                    Icon(painterResource(R.drawable.ic_plus), contentDescription = "Increment")
                }
            }
        }
    }
}

@Composable
private fun ColorPopup(
    sidebarWidth: Dp,
    popupOnLeft: Boolean,
    buttonSize: Dp,
    centerY: Dp,
    onColorSelected: (Color) -> Unit
) {
    val density = LocalDensity.current
    val offsetX = with(density) {
        if (popupOnLeft) -(buttonSize * 4 + 8.dp).roundToPx() else (sidebarWidth + 8.dp).roundToPx()
    }
    val offsetY = with(density) { (centerY - buttonSize / 2).roundToPx() }

    val colors = DEFAULT_COLOR_OPTIONS

    Popup(
        offset = IntOffset(offsetX, offsetY),
        properties = PopupProperties(focusable = false)
    ) {
        Column(
            Modifier
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            colors.chunked(4).forEach { row ->
                Row {
                    row.forEach { col ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .padding(2.dp)
                                .background(col, CircleShape)
                                .border(1.dp, Color.DarkGray, CircleShape)
                                .pointerInput(col) {
                                    detectTapGestures { onColorSelected(col) }
                                }
                        )
                    }
                }
            }
        }
    }
}