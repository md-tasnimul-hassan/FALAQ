package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Shortcut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    shortcuts: List<Shortcut>,
    onShortcutClick: (String) -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onDeleteShortcut: (Shortcut) -> Unit,
    onEditShortcut: (Shortcut) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    val handleSave = {
        if (titleInput.isNotBlank() && urlInput.isNotBlank()) {
            var finalUrl = urlInput
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "https://$finalUrl"
            }
            if (editingShortcut != null) {
                onEditShortcut(editingShortcut!!.copy(title = titleInput, url = finalUrl))
            } else {
                onAddShortcut(titleInput, finalUrl)
            }
            showAddDialog = false
            editingShortcut = null
            titleInput = ""
            urlInput = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "FALAQ",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { shortcut ->
                ShortcutItem(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut.url) },
                    onLongClick = {
                        editingShortcut = shortcut
                        titleInput = shortcut.title
                        urlInput = shortcut.url
                        showAddDialog = true
                    }
                )
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        editingShortcut = null
                        titleInput = ""
                        urlInput = ""
                        showAddDialog = true
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Shortcut",
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingShortcut = null
            },
            title = { Text(if (editingShortcut != null) "Edit Shortcut" else "Add Shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = handleSave) {
                    Text("Save")
                }
            },
            dismissButton = {
                if (editingShortcut != null) {
                    TextButton(
                        onClick = {
                            onDeleteShortcut(editingShortcut!!)
                            showAddDialog = false
                            editingShortcut = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                } else {
                    TextButton(onClick = {
                        showAddDialog = false
                        editingShortcut = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutItem(
    shortcut: Shortcut,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(52.dp)
        ) {
            val domain = try {
                android.net.Uri.parse(shortcut.url).host ?: shortcut.url
            } catch (e: Exception) {
                shortcut.url
            }
            coil.compose.AsyncImage(
                model = "https://www.google.com/s2/favicons?domain=${domain}&sz=128",
                contentDescription = shortcut.title,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
