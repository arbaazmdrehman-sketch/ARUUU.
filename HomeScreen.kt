package com.aruuu.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.local.LockedAppEntity
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.domain.model.VaultSettings
import com.aruuu.app.service.AppLockService
import com.aruuu.app.ui.theme.ARUUUColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════════

data class HomeUiState(
    val lockedApps: List<LockedAppEntity> = emptyList(),
    val settings: VaultSettings = VaultSettings(),
    val isServiceRunning: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeLockedApps(),
        repository.settings,
    ) { apps, settings ->
        HomeUiState(lockedApps = apps, settings = settings, isServiceRunning = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleAppLockService(running: Boolean, context: android.content.Context) {
        if (running) AppLockService.start(context) else AppLockService.stop(context)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToManageApps: () -> Unit,
    onNavigateToIntruderLog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val lockedCount = state.lockedApps.count { it.isLocked }
    val hiddenCount = state.lockedApps.count { it.isHidden }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null,
                            tint = ARUUUColors.CyanPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ARUUU", fontWeight = FontWeight.ExtraBold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            // Background glow orb
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ARUUUColors.CyanGlow, Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.05f),
                        radius = size.width * 0.55f,
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // ── Status card ──
                item {
                    Spacer(Modifier.height(8.dp))
                    VaultStatusCard(
                        isActive = state.isServiceRunning,
                        lockedCount = lockedCount,
                        hiddenCount = hiddenCount,
                    )
                }

                // ── Quick stats row ──
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        StatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Lock,
                            label = "Locked",
                            value = lockedCount.toString(),
                            color = ARUUUColors.CyanPrimary,
                        )
                        StatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.VisibilityOff,
                            label = "Hidden",
                            value = hiddenCount.toString(),
                            color = ARUUUColors.Warning,
                        )
                        StatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.PhotoCamera,
                            label = "Selfies",
                            value = "—",
                            color = ARUUUColors.Danger,
                        )
                    }
                }

                // ── Quick action cards ──
                item {
                    Text("Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item {
                    QuickActionCard(
                        icon = Icons.Rounded.Apps,
                        title = "Manage Apps",
                        subtitle = "Add, remove or toggle app protection",
                        accent = ARUUUColors.CyanPrimary,
                        onClick = onNavigateToManageApps,
                    )
                }

                item {
                    QuickActionCard(
                        icon = Icons.Rounded.PhotoCamera,
                        title = "Intruder Log",
                        subtitle = "View selfies captured on failed unlocks",
                        accent = ARUUUColors.Danger,
                        onClick = onNavigateToIntruderLog,
                    )
                }

                item {
                    QuickActionCard(
                        icon = Icons.Rounded.Settings,
                        title = "Settings",
                        subtitle = "Auth method, auto-lock, disguise mode",
                        accent = ARUUUColors.Purple,
                        onClick = onNavigateToSettings,
                    )
                }

                // ── Recently locked apps ──
                if (state.lockedApps.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Protected Apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.lockedApps.take(5)) { app ->
                        LockedAppRow(app)
                    }
                    if (state.lockedApps.size > 5) {
                        item {
                            TextButton(
                                onClick = onNavigateToManageApps,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("View all ${state.lockedApps.size} protected apps")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────

@Composable
private fun VaultStatusCard(
    isActive: Boolean,
    lockedCount: Int,
    hiddenCount: Int,
) {
    val pulseAnim = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, if (isActive) ARUUUColors.CyanPrimary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pulsing status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (isActive) ARUUUColors.Success.copy(alpha = pulseAlpha)
                        else ARUUUColors.Danger
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isActive) "Vault Protection Active" else "Vault Protection Off",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "$lockedCount apps locked · $hiddenCount apps hidden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (isActive) Icons.Rounded.Shield else Icons.Rounded.ShieldMoon,
                null,
                tint = if (isActive) ARUUUColors.CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LockedAppRow(app: LockedAppEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(ARUUUColors.CyanGlow),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                app.appName.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = ARUUUColors.CyanPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appName, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Text(app.packageName, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (app.isLocked) Badge(containerColor = ARUUUColors.CyanPrimary.copy(alpha = 0.2f)) {
                Text("LOCKED", style = MaterialTheme.typography.labelSmall,
                    color = ARUUUColors.CyanPrimary)
            }
            if (app.isHidden) Badge(containerColor = ARUUUColors.Warning.copy(alpha = 0.2f)) {
                Text("HIDDEN", style = MaterialTheme.typography.labelSmall,
                    color = ARUUUColors.Warning)
            }
        }
    }
}
