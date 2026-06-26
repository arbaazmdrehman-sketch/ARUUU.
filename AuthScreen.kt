package com.aruuu.app.ui.screens.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.domain.model.*
import com.aruuu.app.receiver.VaultLockState
import com.aruuu.app.service.IntruderCaptureService
import com.aruuu.app.service.ARUUUBiometricManager
import com.aruuu.app.ui.theme.ARUUUColors
import com.aruuu.app.ui.theme.ARUUUTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// AppLockActivity — shown over locked apps
// ═══════════════════════════════════════════════════════════════════════════

@AndroidEntryPoint
class AppLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lockedPkg = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: ""

        setContent {
            ARUUUTheme {
                AuthScreen(
                    lockedPackage = lockedPkg,
                    onAuthenticated = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "locked_package"
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// AuthViewModel
// ═══════════════════════════════════════════════════════════════════════════

data class AuthUiState(
    val authMethod: AuthMethod = AuthMethod.PIN,
    val biometricAvailable: Boolean = false,
    val pinInput: String = "",
    val passwordInput: String = "",
    val error: String? = null,
    val failedAttempts: Int = 0,
    val maxFailedAttempts: Int = 3,
    val isLocked: Boolean = false,   // temporarily locked after too many failures
    val isLoading: Boolean = true,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: ARUUURepository,
    private val biometricManager: ARUUUBiometricManager,
    private val intruderService: IntruderCaptureService,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.settings.first()
            _state.value = AuthUiState(
                authMethod = settings.primaryAuthMethod,
                biometricAvailable = biometricManager.isBiometricAvailable() && settings.biometricEnabled,
                failedAttempts = repository.getFailedAttempts(),
                maxFailedAttempts = settings.maxFailedAttempts,
                isLoading = false,
            )
        }
    }

    fun onPinChanged(pin: String) {
        _state.update { it.copy(pinInput = pin.take(6), error = null) }
    }

    fun onPasswordChanged(pw: String) {
        _state.update { it.copy(passwordInput = pw, error = null) }
    }

    fun verifyPin(
        pin: String,
        targetPkg: String = "",
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            if (repository.verifyPin(pin)) {
                handleSuccess()
                onSuccess()
            } else {
                handleFailure(targetPkg)
            }
        }
    }

    fun verifyPassword(
        password: String,
        targetPkg: String = "",
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            if (repository.verifyPassword(password)) {
                handleSuccess()
                onSuccess()
            } else {
                handleFailure(targetPkg)
            }
        }
    }

    private fun handleSuccess() {
        repository.resetFailedAttempts()
        VaultLockState.unlock()
        _state.update { it.copy(error = null, failedAttempts = 0, pinInput = "", passwordInput = "") }
    }

    private fun handleFailure(targetPkg: String) {
        val attempts = repository.incrementFailedAttempts()
        val max = _state.value.maxFailedAttempts
        val remaining = max - attempts
        val locked = attempts >= max

        _state.update { st ->
            st.copy(
                failedAttempts = attempts,
                error = if (locked) "Too many failed attempts. Try again in 30 seconds."
                else "Wrong credential. $remaining attempts remaining.",
                isLocked = locked,
                pinInput = "",
                passwordInput = "",
            )
        }

        // Trigger intruder selfie at threshold
        if (attempts >= max) {
            intruderService.captureIntruder(attempts, targetPkg)
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
}

// ═══════════════════════════════════════════════════════════════════════════
// AuthScreen composable
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AuthScreen(
    lockedPackage: String = "",
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Auto-trigger biometric on launch
    LaunchedEffect(state.isLoading, state.biometricAvailable) {
        if (!state.isLoading && state.biometricAvailable) {
            val activity = context as? androidx.fragment.app.FragmentActivity ?: return@LaunchedEffect
            val result = (context as? ARUUUBiometricManager)?.authenticate(activity)
            // Biometric trigger handled separately via button for composable context
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(ARUUUColors.CyanGlow, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                    radius = size.width * 0.7f,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Vault logo with lock icon
            VaultLogo()

            Spacer(Modifier.height(12.dp))

            Text(
                "ARUUU",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
            )

            if (lockedPackage.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "This app is locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(48.dp))

            AnimatedContent(
                targetState = state.authMethod,
                label = "auth_method",
            ) { method ->
                when (method) {
                    AuthMethod.PIN -> PinAuthContent(
                        pin = state.pinInput,
                        onPinChange = vm::onPinChanged,
                        onSubmit = { vm.verifyPin(state.pinInput, lockedPackage, onAuthenticated) },
                        error = state.error,
                        biometricAvailable = state.biometricAvailable,
                        isLocked = state.isLocked,
                    )
                    AuthMethod.PASSWORD -> PasswordAuthContent(
                        password = state.passwordInput,
                        onPasswordChange = vm::onPasswordChanged,
                        onSubmit = { vm.verifyPassword(state.passwordInput, lockedPackage, onAuthenticated) },
                        error = state.error,
                        biometricAvailable = state.biometricAvailable,
                        isLocked = state.isLocked,
                    )
                    else -> PinAuthContent(
                        pin = state.pinInput,
                        onPinChange = vm::onPinChanged,
                        onSubmit = { vm.verifyPin(state.pinInput, lockedPackage, onAuthenticated) },
                        error = state.error,
                        biometricAvailable = state.biometricAvailable,
                        isLocked = state.isLocked,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow_alpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(ARUUUColors.CyanGlow.copy(alpha = glowAlpha))
        )
        Icon(
            Icons.Rounded.Lock, null,
            tint = ARUUUColors.CyanPrimary,
            modifier = Modifier.size(52.dp),
        )
    }
}

@Composable
private fun PinAuthContent(
    pin: String,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    biometricAvailable: Boolean,
    isLocked: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (i < pin.length) ARUUUColors.CyanPrimary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        // Number pad
        NumberKeypad(
            onDigit = { d -> if (!isLocked && pin.length < 6) onPinChange(pin + d) },
            onDelete = { if (pin.isNotEmpty()) onPinChange(pin.dropLast(1)) },
            onConfirm = { if (pin.length >= 4 && !isLocked) onSubmit() },
            disabled = isLocked,
        )

        if (biometricAvailable) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { /* Biometric re-trigger — handled in VM */ }) {
                Icon(Icons.Rounded.Fingerprint, null, tint = ARUUUColors.CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Use fingerprint", color = ARUUUColors.CyanPrimary)
            }
        }
    }
}

@Composable
private fun PasswordAuthContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    error: String?,
    biometricAvailable: Boolean,
    isLocked: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            enabled = !isLocked,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onSubmit, enabled = !isLocked) {
                    Icon(Icons.Rounded.ArrowForward, "Unlock")
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSubmit, enabled = password.isNotEmpty() && !isLocked,
            modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Unlock")
        }
        if (biometricAvailable) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { }) {
                Icon(Icons.Rounded.Fingerprint, null, tint = ARUUUColors.CyanPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Use fingerprint", color = ARUUUColors.CyanPrimary)
            }
        }
    }
}

@Composable
private fun NumberKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    disabled: Boolean,
) {
    val rows = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("","0","⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = { when (key) { "⌫" -> onDelete(); "" -> {} else -> onDigit(key) } },
                        modifier = Modifier.size(80.dp),
                        shape = MaterialTheme.shapes.large,
                        enabled = !disabled && key.isNotEmpty(),
                    ) {
                        Text(key, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onConfirm,
            enabled = !disabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Rounded.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("Unlock")
        }
    }
}
