package com.example.stylusdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ZoomControls(viewMatrixManager: ViewMatrixManager, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.8f))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = { viewMatrixManager.onGesture(1.25f, Offset.Zero, 0f, 0f) }) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
        }
        IconButton(onClick = { viewMatrixManager.onGesture(0.8f, Offset.Zero, 0f, 0f) }) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
        }
        IconButton(onClick = {
            val reset = 1f / viewMatrixManager.scale
            viewMatrixManager.onGesture(reset, Offset.Zero, 0f, 0f)
        }) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
        }
        Text(
            text = "${(viewMatrixManager.scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}