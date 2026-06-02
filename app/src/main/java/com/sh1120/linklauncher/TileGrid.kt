package com.sh1120.linklauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.util.UUID

val presetColors = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
    Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
    Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800),
    Color(0xFFFF5722), Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFF000000),
    Color(0xFF333333), Color(0xFF666666), Color(0xFF999999), Color(0xFFCCCCCC), Color(0xFFFFFFFF)
)

@Composable
fun TileGrid(
    modifier: Modifier = Modifier,
    viewModel: TileViewModel = viewModel()
) {
    val context = LocalContext.current
    val tiles = viewModel.tiles

    LaunchedEffect(Unit) {
        viewModel.loadTiles(context)
    }

    var editingTile by remember { mutableStateOf<TileData?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tiles, key = { it.id }) { tile ->
                    TileItem(
                        tile = tile,
                        onClick = {
                            // タップ回数をカウントアップ
                            viewModel.incrementTapCount(context, tile.id)
                            
                            try {
                                if (tile.packageName != null && tile.packageName.isNotEmpty()) {
                                    val intent = context.packageManager.getLaunchIntentForPackage(tile.packageName)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "No Entry", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (tile.url != null && tile.url.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tile.url))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "No Entry", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No Entry", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLongClick = { editingTile = tile }
                    )
                }
            }

            // ディスプレイ表示領域の最下部に配置
            Text(
                text = "Created by sh1120",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            )
        }
    }

    if (showAddDialog) {
        EditTileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newTile ->
                tiles.add(newTile)
                viewModel.saveTiles(context)
                showAddDialog = false
            }
        )
    }

    if (editingTile != null) {
        EditTileDialog(
            initialTile = editingTile,
            onDismiss = { editingTile = null },
            onConfirm = { updatedTile ->
                val index = tiles.indexOfFirst { it.id == updatedTile.id }
                if (index != -1) tiles[index] = updatedTile
                viewModel.saveTiles(context)
                editingTile = null
            },
            onDelete = {
                tiles.removeIf { it.id == editingTile?.id }
                viewModel.saveTiles(context)
                editingTile = null
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun SettingsDialog(viewModel: TileViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(viewModel.exportJson().toByteArray())
                }
                Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.readBytes().decodeToString()
                    if (viewModel.importJson(json)) {
                        viewModel.saveTiles(context)
                        Toast.makeText(context, "Imported successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid format", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Data Management", fontWeight = FontWeight.Bold)
                
                Button(
                    onClick = { createDocumentLauncher.launch("link_launcher_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Config to File")
                }

                Button(
                    onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Config from File")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("About This App", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("App Name", color = Color.Gray, fontSize = 14.sp)
                        Text("LinkLauncher", fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Version", color = Color.Gray, fontSize = 14.sp)
                        Text("1.0.0", fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Developer", color = Color.Gray, fontSize = 14.sp)
                        Text("sh1120", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2026 sh1120. All rights reserved.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun EditTileDialog(
    initialTile: TileData? = null,
    onDismiss: () -> Unit,
    onConfirm: (TileData) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var label by remember { mutableStateOf(initialTile?.label ?: "") }
    var type by remember { mutableStateOf(if (initialTile?.packageName != null) "App" else "URL") }
    var url by remember { mutableStateOf(initialTile?.url ?: "") }
    var packageName by remember { mutableStateOf(initialTile?.packageName ?: "") }
    var selectedColorArgb by remember { mutableIntStateOf(initialTile?.colorArgb ?: presetColors[0].toArgb()) }
    var showAppPicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun suggestTitle(inputUrl: String): String {
        return try {
            val uri = Uri.parse(inputUrl)
            val host = uri.host ?: ""
            host.removePrefix("www.").substringBefore(".")
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                label = app.label
                packageName = app.packageName
                showAppPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTile == null) "Add Tile" else "Edit Tile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Google") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "URL", onClick = { type = "URL" })
                    Text("URL")
                    RadioButton(selected = type == "App", onClick = { type = "App" })
                    Text("App")
                }

                if (type == "URL") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { 
                            url = it
                            if (label.isEmpty()) {
                                label = suggestTitle(it)
                            }
                        },
                        label = { Text("URL") },
                        placeholder = { Text("https://www.google.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = { showAppPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (packageName.isEmpty()) "Select App" else packageName)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Tile Color", fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 5) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (j in 0 until 5) {
                                val color = presetColors[i * 5 + j]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColorArgb == color.toArgb()) 3.dp else 1.dp,
                                            color = if (selectedColorArgb == color.toArgb()) Color.Black else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorArgb = color.toArgb() }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val finalLabel = if (label.isBlank()) (if (type == "URL") suggestTitle(url) else packageName) else label
                val newTile = TileData(
                    id = initialTile?.id ?: UUID.randomUUID().toString(),
                    label = if (finalLabel.isBlank()) "Untitled" else finalLabel,
                    url = if (type == "URL") url else null,
                    packageName = if (type == "App") packageName else null,
                    colorArgb = selectedColorArgb,
                    tapCount = initialTile?.tapCount ?: 0 // ここで既存のカウントを引き継ぐ
                )
                onConfirm(newTile)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun AppPickerDialog(onDismiss: () -> Unit, onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL).map {
            AppInfo(
                it.loadLabel(pm).toString(),
                it.activityInfo.packageName
            )
        }.sortedBy { it.label }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App") },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                LazyColumn {
                    items(apps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = remember(app.packageName) {
                                    try { context.packageManager.getApplicationIcon(app.packageName) } catch(e: Exception) { null }
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TileItem(
    tile: TileData,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "scale")

    val iconModel = remember(tile) {
        if (tile.packageName != null) {
            try {
                context.packageManager.getApplicationIcon(tile.packageName)
            } catch (e: Exception) { null }
        } else if (tile.url != null) {
            val domain = try { Uri.parse(tile.url).host ?: "" } catch(e: Exception) { "" }
            if (domain.isNotEmpty() && domain.contains(".")) {
                "https://www.google.com/s2/favicons?domain=$domain&sz=128"
            } else null
        } else null
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(tile.color)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = iconModel,
            contentDescription = null,
            modifier = Modifier.size(64.dp).alpha(0.8f),
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                        startY = 300f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tile.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tile.packageName != null) {
                Text(
                    text = "APP",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}
