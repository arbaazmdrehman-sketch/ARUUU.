package com.aruuu.app.ui.screens.apps

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.domain.model.AppInfo
import com.aruuu.app.ui.theme.ARUUUColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════════

data class ManageAppsUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filterLocked: Boolean = false,
    val isLoading: Boolean = true,
    val snackMessage: String? = null,
)

@HiltViewModel
class ManageAppsViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ManageAppsUiState())
    val state: StateFlow<ManageAppsUiState> = _state.asStateFlow()

    init { loadApps() }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps(includeSystem = false)
            _state.update { it.copy(allApps = apps, filteredApps = apps, isLoading = false) }
        }
    }

    fun onSearchChanged(query: String) {
        _state.update { st ->
            val filtered = filterApps(st.allApps, query, st.filterLocked)
            st.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun toggleFilterLocked() {
        _state.update { st ->
            val newFilter = !st.filterLocked
            val filtered = filterApps(st.allApps, st.searchQuery, newFilter)
            st.copy(filterLocked = newFilter, filteredApps = filtered)
        }
    }

    private fun filterApps(apps: List<AppInfo>, query: String, lockedOnly: Boolean): List<AppInfo> {
        return apps
            .filter { if (lockedOnly) it.isLocked || it.isHidden else true }
            .filter { it.appName.contains(query, ignoreCase = true) ||
                      it.packageName.contains(query, ignoreCase = true) }
    }

    fun toggleLock(app: AppInfo) {
        viewModelScope.launch {
            if (app.isLocked) {
                repository.removeFromVault(app.packageName)
                _state.update { st ->
                    val updated = st.allApps.map {
                        if (it.packageName == app.packageName) it.copy(isLocked = false, isHidden = false) else it
                    }
                    st.copy(
                        allApps = updated,
                        filteredApps = filterApps(updated, st.searchQuery, st.filterLocked),
                        snackMessage = "${app.appName} removed from vault",
                    )
                }
            } else {
                repository.addToVault(app.packageName, app.appName, hidden = false)
                _state.update { st ->
                    val updated = st.allApps.map {
                        if (it.packageName == app.packageName) it.copy(isLocked = true) else it
                    }
                    st.copy(
                        allApps = updated,
                        filteredApps = filterApps(updated, st.searchQuery, st.filterLocked),
                        snackMessage = "${app.appName} locked",
                    )
                }
            }
        }
    }

    fun toggleHide(app: AppInfo) {
        viewModelScope.launch {
            if (app.isHidden) {
                repository.removeFromVault(app.packageName)
                _state.update { st ->
                    val updated = st.allApps.map {
                        if (it.packageName == app.packageName) it.copy(isHidden = false, isLocked = false) else it
                    }
                    st.copy(
                        allApps = updated,
                        filteredApps = filterApps(updated, st.searchQuery, st.filterLocked),
                        snackMessage = "${app.appName} unhidden",
                    )
                }
            } else {
                repository.addToVault(app.packageName, app.appName, hidden = true)
                _state.update { st ->
                    val updated = st.allApps.map {
                        if (it.packageName == app.packageName) it.copy(isHidden = true, isLocked = true) else it
                    }
                    st.copy(
                        allApps = updated,
                        filteredApps = filterApps(updated, st.searchQuery, st.filterLocked),
                        snackMessage = "${app.appName} hidden from launcher",
                    )
                }
            }
        }
    }

    fun clearSnack() { _state.update { it.copy(snackMessage = null) } }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsScreen(
    onBack: () -> Unit,
    vm: ManageAppsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Apps", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::onSearchChanged,
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.onSearchChanged("") }) {
                            Icon(Icons.Rounded.Close, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )

            Spacer(Modifier.height(12.dp))

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filterLocked,
                    onClick = vm::toggleFilterLocked,
                    label = { Text("Protected only") },
                    leadingIcon = if (state.filterLocked) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                    } else null,
                )
                Text(
                    "${state.filteredApps.size} apps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ARUUUColors.CyanPrimary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(
                        items = state.filteredApps,
                        key = { it.packageName },
                    ) { app ->
                        AppListItem(
                            app = app,
                            onToggleLock = { vm.toggleLock(app) },
                            onToggleHide = { vm.toggleHide(app) },
                        )
                    }
                }
            }
        }
    }
}

// ─── App list item with swipe actions ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListItem(
    app: AppInfo,
    onToggleLock: () -> Unit,
    onToggleHide: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val icon: Drawable? = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
        }.getOrNull()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked || app.isHidden)
                MaterialTheme.colorScheme.surfaceVariant
            else Color.Transparent,
        ),
        border = if (app.isLocked || app.isHidden)
            BorderStroke(1.dp, ARUUUColors.CyanPrimary.copy(alpha = 0.3f))
        else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(44, 44).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(ARUUUColors.CyanGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            app.appName.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = ARUUUColors.CyanPrimary,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Status badges
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (app.isHidden) Icon(Icons.Rounded.VisibilityOff, null,
                    tint = ARUUUColors.Warning, modifier = Modifier.size(16.dp))
                if (app.isLocked) Icon(Icons.Rounded.Lock, null,
                    tint = ARUUUColors.CyanPrimary, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.width(4.dp))

            // More options button
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (app.isLocked) "Remove lock" else "Lock app") },
                        leadingIcon = {
                            Icon(if (app.isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                null, tint = ARUUUColors.CyanPrimary)
                        },
                        onClick = { expanded = false; onToggleLock() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (app.isHidden) "Unhide app" else "Hide app") },
                        leadingIcon = {
                            Icon(if (app.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                null, tint = ARUUUColors.Warning)
                        },
                        onClick = { expanded = false; onToggleHide() },
                    )
                }
            }
        }
    }
}
