package com.aruuu.app.ui.screens.apps

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.local.IntruderEntity
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.ui.theme.ARUUUColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class IntruderLogViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    val records: StateFlow<List<IntruderEntity>> = repository.observeIntruderRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) { viewModelScope.launch { repository.deleteIntruderRecord(id) } }
    fun clearAll() { viewModelScope.launch { repository.clearIntruderRecords() } }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderLogScreen(
    onBack: () -> Unit,
    vm: IntruderLogViewModel = hiltViewModel(),
) {
    val records by vm.records.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear intruder log?") },
            text = { Text("All captured selfies and records will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ARUUUColors.Danger)) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruder Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Rounded.DeleteSweep, "Clear all",
                                tint = ARUUUColors.Danger)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (records.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.NoPhotography, null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No intruder records",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Photos are captured automatically\nafter failed unlock attempts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ARUUUColors.Danger.copy(alpha = 0.1f),
                        ),
                        border = BorderStroke(1.dp, ARUUUColors.Danger.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, null, tint = ARUUUColors.Danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${records.size} unauthorised access attempt${if (records.size != 1) "s" else ""} detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = ARUUUColors.Danger)
                        }
                    }
                }

                items(records, key = { it.id }) { record ->
                    IntruderCard(record = record, onDelete = { vm.delete(record.id) })
                }
            }
        }
    }
}

@Composable
private fun IntruderCard(record: IntruderEntity, onDelete: () -> Unit) {
    val dateStr = remember(record.timestampMs) {
        SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault())
            .format(Date(record.timestampMs))
    }
    val bitmap = remember(record.imagePath) {
        runCatching {
            val file = File(record.imagePath)
            if (file.exists()) BitmapFactory.decodeFile(record.imagePath) else null
        }.getOrNull()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            // Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Intruder photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.BrokenImage, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp))
                        Text("Photo unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Overlay badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(ARUUUColors.Danger)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("${record.failedAttempts} failed attempts",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White)
                }
            }

            // Info row
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(dateStr,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (record.targetPackage.isNotBlank()) {
                        Text("App: ${record.targetPackage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "Delete",
                        tint = ARUUUColors.Danger, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
