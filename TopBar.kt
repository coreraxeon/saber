// File: app/src/main/java/com/example/stylusdraw/TopBar.kt
package com.example.stylusdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*

@Composable
fun TopBar(
    title: String,
    hasContent: Boolean,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onAddPage: () -> Unit,
    onPageLayout: () -> Unit,
    onTitleClick: () -> Unit   // callback when user taps the title
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFFF4F4F4)) // Now resolves background
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment   = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Menu & Back
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onMenu, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = "Menu")
            }
            IconButton(onClick = onBack, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
            }
        }

        // Center: The title itself is clickable to rename
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally)
                .clickable { onTitleClick() },  // Now resolves clickable
            maxLines = 1
        )

        // Right side: Export, Settings, and layout toggles
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            var exportMenuExpanded by remember { mutableStateOf(false) }

            IconButton(onClick = onAddPage, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add Page")
            }
            IconButton(onClick = onPageLayout, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_page_layout), contentDescription = "Page Menu")
            }
            IconButton(onClick = { exportMenuExpanded = true }, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_export), contentDescription = "Export")
            }
            DropdownMenu(
                expanded = exportMenuExpanded,
                onDismissRequest = { exportMenuExpanded = false }
            ) {
                DropdownMenuItem(text = { Text("Export to PDF") }, onClick = {
                    exportMenuExpanded = false
                    onExport()
                })
            }
            IconButton(onClick = onSettings, Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
            }
        }
    }
}
