package com.example.stylusdraw

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stylusdraw.data.Folder
import com.example.stylusdraw.ui.theme.lightGray

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    fileCountOffsetX: Int = 5,
    fileCountOffsetY: Int = 5,
    folderNameOffsetX: Int = 0,
    folderNameOffsetY: Int = -10,
    subFolders: List<Folder> = emptyList(),
    subFolderOffsetX: Int = 5,
    subFolderOffsetY: Int = 15
) {
    val clickInteraction = remember { MutableInteractionSource() }
    val isPressed by clickInteraction.collectIsPressedAsState()

    val hoverInteraction = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }
    LaunchedEffect(hoverInteraction) {
        hoverInteraction.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> isHovered = true
                is HoverInteraction.Exit -> isHovered = false
            }
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .hoverable(hoverInteraction)
            .combinedClickable(
                interactionSource = clickInteraction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Change icon based on hover/selected state
        Image(
            painter = painterResource(
                id = if (isHovered || isPressed || isSelected) R.drawable.ic_folder2
                else R.drawable.ic_folder
            ),
            contentDescription = "Folder Icon",
            modifier = Modifier.size(100.dp)
        )
        
        Text(
            text = "${folder.fileCount}",
            color = lightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(fileCountOffsetX.dp, fileCountOffsetY.dp)
        )
        
        Text(
            text = folder.name,
            color = lightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(folderNameOffsetX.dp, folderNameOffsetY.dp)
        )
        
        if (subFolders.isNotEmpty()) {
            Text(
                text = "${subFolders.size}",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset((-subFolderOffsetX).dp, subFolderOffsetY.dp)
            )
        }
    }
}
