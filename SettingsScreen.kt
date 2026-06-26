package com.aruuu.app.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.domain.model.*
import com.aruuu.app.ui.theme.ARUUUColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    val settings: StateFlow<VaultSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultSettings())

    fun update(block: VaultSettings.() -> VaultSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.block())
        }
    }

    fun factoryReset(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.factoryReset()
            onDone()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetVault: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.Warning, null, tint = ARUUUColors.Danger) },
            title = { Text("Factory Reset ARUUU?") },
            text = { Text("This will delete ALL locked apps, credentials, intruder records, and settings. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.factoryReset(onResetVault) },
                    colors = ButtonDefaults.buttonColors(containerColor = ARUUUColors.Danger),
                ) { Text("Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            // ── Security ──────────────────────────────────────────────────
            SettingsSectionHeader("Security")

            SettingsToggleRow(
                icon = Icons.Rounded.Fingerprint,
                iconTint = ARUUUColors.CyanPrimary,
                title = "Biometric unlock",
                subtitle = "Use fingerprint or face to unlock",
                checked = settings.biometricEnabled,
                onCheckedChange = { vm.update { copy(biometricEnabled = it) } },
            )

            SettingsDropdownRow(
                icon = Icons.Rounded.Timer,
                iconTint = ARUUUColors.Warning,
                title = "Auto-lock delay",
                value = when (settings.autoLockDelaySeconds) {
                    0 -> "Immediately"
                    15 -> "15 seconds"
                    30 -> "30 seconds"
                    60 -> "1 minute"
                    300 -> "5 minutes"
                    -1 -> "Never"
                    else -> "${settings.autoLockDelaySeconds}s"
                },
                options = listOf(
                    "Immediately" to 0,
                    "15 seconds" to 15,
                    "30 seconds" to 30,
                    "1 minute" to 60,
                    "5 minutes" to 300,
                    "Never" to -1,
                ),
                onSelect = { vm.update { copy(autoLockDelaySeconds = it) } },
            )

            SettingsDropdownRow(
                icon = Icons.Rounded.Block,
                iconTint = ARUUUColors.Danger,
                title = "Max failed attempts",
                value = "${settings.maxFailedAttempts}",
                options = listOf("3" to 3, "5" to 5, "7" to 7, "10" to 10),
                onSelect = { vm.update { copy(maxFailedAttempts = it) } },
            )

            // ── Intruder Selfie ───────────────────────────────────────────
            SettingsSectionHeader("Intruder Detection")

            SettingsToggleRow(
                icon = Icons.Rounded.PhotoCamera,
                iconTint = ARUUUColors.Danger,
                title = "Intruder selfie",
                subtitle = "Capture photo after failed unlocks",
                checked = settings.intruderSelfieEnabled,
                onCheckedChange = { vm.update { copy(intruderSelfieEnabled = it) } },
            )

            // ── Appearance ────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")

            SettingsSegmentedRow(
                icon = Icons.Rounded.Palette,
                iconTint = ARUUUColors.Purple,
                title = "Theme",
                options = listOf("Light", "Dark", "System"),
                selectedIndex = when (settings.themeMode) {
                    ThemeMode.LIGHT  -> 0
                    ThemeMode.DARK   -> 1
                    ThemeMode.SYSTEM -> 2
                },
                onSelect = {
                    val mode = when (it) { 0 -> ThemeMode.LIGHT; 1 -> ThemeMode.DARK; else -> ThemeMode.SYSTEM }
                    vm.update { copy(themeMode = mode) }
                },
            )

            SettingsToggleRow(
                icon = Icons.Rounded.Vibration,
                iconTint = Color(0xFF8BC34A),
                title = "Haptic feedback",
                subtitle = "Vibrate on keypad input",
                checked = settings.hapticFeedback,
                onCheckedChange = { vm.update { copy(hapticFeedback = it) } },
            )

            // ── Disguise Mode ─────────────────────────────────────────────
            SettingsSectionHeader("Disguise Mode")

            SettingsToggleRow(
                icon = Icons.Rounded.Calculate,
                iconTint = Color(0xFFFF9800),
                title = "Enable disguise mode",
                subtitle = "Make ARUUU appear as '${settings.disguiseLabel}'",
                checked = settings.disguiseModeEnabled,
                onCheckedChange = { vm.update { copy(disguiseModeEnabled = it) } },
            )

            if (settings.disguiseModeEnabled) {
                SettingsDropdownRow(
                    icon = Icons.Rounded.Apps,
                    iconTint = Color(0xFFFF9800),
                    title = "Disguise as",
                    value = settings.disguiseLabel,
                    options = listOf(
                        "Calculator" to "Calculator",
                        "Notes" to "Notes",
                        "Clock" to "Clock",
                        "Weather" to "Weather",
                    ),
                    onSelect = { vm.update { copy(disguiseLabel = it) } },
                )
            }

            // ── Privacy ───────────────────────────────────────────────────
            SettingsSectionHeader("Privacy")

            SettingsInfoRow(
                icon = Icons.Rounded.Shield,
                iconTint = ARUUUColors.Success,
                title = "100% on-device",
                subtitle = "No data is ever sent to external servers",
            )

            SettingsInfoRow(
                icon = Icons.Rounded.Key,
                iconTint = ARUUUColors.CyanPrimary,
                title = "Android Keystore encryption",
                subtitle = "Credentials secured by hardware-backed keys",
            )

            // ── Danger zone ───────────────────────────────────────────────
            SettingsSectionHeader("Danger Zone")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ARUUUColors.Danger.copy(alpha = 0.08f),
                ),
                border = BorderStroke(1.dp, ARUUUColors.Danger.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.DeleteForever, null,
                        tint = ARUUUColors.Danger, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Factory Reset", style = MaterialTheme.typography.titleMedium,
                            color = ARUUUColors.Danger, fontWeight = FontWeight.SemiBold)
                        Text("Delete all data and start over",
                            style = MaterialTheme.typography.bodySmall,
                            color = ARUUUColors.Danger.copy(alpha = 0.7f))
                    }
                    Icon(Icons.Rounded.ChevronRight, null,
                        tint = ARUUUColors.Danger.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Version footer
            Text(
                "ARUUU v1.0.0 · Privacy-first app vault",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Settings row components ─────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ARUUUColors.CyanPrimary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> SettingsDropdownRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.ExpandMore, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, v) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(v); expanded = false },
                    leadingIcon = if (label == value) {
                        { Icon(Icons.Rounded.Check, null, tint = ARUUUColors.CyanPrimary) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun SettingsSegmentedRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == selectedIndex,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
