package com.aruuu.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
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
import androidx.compose.ui.text.style.TextAlign
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

// ─── ViewModel ───────────────────────────────────────────────────────────

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: ARUUURepository,
) : ViewModel() {

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _selectedAuth = MutableStateFlow(AuthMethod.PIN)
    val selectedAuth: StateFlow<AuthMethod> = _selectedAuth.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun selectAuthMethod(method: AuthMethod) { _selectedAuth.value = method }

    fun onPinChanged(pin: String) { _pinInput.value = pin.take(6) }
    fun onPasswordChanged(pw: String) { _passwordInput.value = pw }

    fun next() { _step.value++ }
    fun back() { if (_step.value > 0) _step.value-- }

    fun savePinAndContinue() {
        val pin = _pinInput.value
        if (pin.length < 4) { _error.value = "PIN must be at least 4 digits"; return }
        repository.setPin(pin)
        _error.value = null
        next()
    }

    fun savePasswordAndContinue() {
        val pw = _passwordInput.value
        if (pw.length < 6) { _error.value = "Password must be at least 6 characters"; return }
        repository.setPassword(pw)
        _error.value = null
        next()
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.updateSettings(
                repository.settings.first().copy(
                    primaryAuthMethod = _selectedAuth.value,
                    onboardingComplete = true,
                )
            )
            onDone()
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val step by vm.step.collectAsState()
    val selectedAuth by vm.selectedAuth.collectAsState()
    val pinInput by vm.pinInput.collectAsState()
    val passwordInput by vm.passwordInput.collectAsState()
    val error by vm.error.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated cyan glow background orb
        CyanGlowOrb()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Progress dots
            StepProgressDots(currentStep = step, totalSteps = 4)

            Spacer(Modifier.height(40.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "onboarding_step",
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep(onNext = { vm.next() })
                    1 -> ChooseAuthStep(
                        selected = selectedAuth,
                        onSelect = vm::selectAuthMethod,
                        onNext = { vm.next() },
                    )
                    2 -> when (selectedAuth) {
                        AuthMethod.PIN -> SetPinStep(
                            pin = pinInput,
                            onPinChange = vm::onPinChanged,
                            error = error,
                            onNext = vm::savePinAndContinue,
                            onBack = { vm.back() },
                        )
                        AuthMethod.PASSWORD -> SetPasswordStep(
                            password = passwordInput,
                            onPasswordChange = vm::onPasswordChanged,
                            error = error,
                            onNext = vm::savePasswordAndContinue,
                            onBack = { vm.back() },
                        )
                        else -> SetPinStep(
                            pin = pinInput,
                            onPinChange = vm::onPinChanged,
                            error = error,
                            onNext = vm::savePinAndContinue,
                            onBack = { vm.back() },
                        )
                    }
                    else -> AllSetStep(onFinish = { vm.completeOnboarding(onComplete) })
                }
            }
        }
    }
}

// ─── Step composables ────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Vault icon with pulsing ring
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "pulse_scale",
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(ARUUUColors.CyanGlow, shape = MaterialTheme.shapes.extraLarge)
            )
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = ARUUUColors.CyanPrimary,
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Welcome to ARUUU",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Your privacy-first app vault.\nLock, hide, and protect your apps with military-grade security — all stored on-device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(40.dp))

        FeatureRow(Icons.Rounded.Fingerprint,      "Biometric unlock")
        Spacer(Modifier.height(12.dp))
        FeatureRow(Icons.Rounded.VisibilityOff,    "App hide & lock")
        Spacer(Modifier.height(12.dp))
        FeatureRow(Icons.Rounded.PhotoCamera,       "Intruder selfie")
        Spacer(Modifier.height(12.dp))
        FeatureRow(Icons.Rounded.Shield,            "100% on-device")

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Get Started", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun ChooseAuthStep(
    selected: AuthMethod,
    onSelect: (AuthMethod) -> Unit,
    onNext: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose your unlock method",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text("You can add fingerprint / face unlock later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        AuthOptionCard(
            icon = Icons.Rounded.Pin,
            title = "PIN",
            subtitle = "4–6 digit numeric PIN",
            selected = selected == AuthMethod.PIN,
            onClick = { onSelect(AuthMethod.PIN) },
        )
        Spacer(Modifier.height(12.dp))
        AuthOptionCard(
            icon = Icons.Rounded.Password,
            title = "Password",
            subtitle = "Alphanumeric passphrase",
            selected = selected == AuthMethod.PASSWORD,
            onClick = { onSelect(AuthMethod.PASSWORD) },
        )
        Spacer(Modifier.height(12.dp))
        AuthOptionCard(
            icon = Icons.Rounded.Pattern,
            title = "Pattern",
            subtitle = "Draw a unique unlock pattern",
            selected = selected == AuthMethod.PATTERN,
            onClick = { onSelect(AuthMethod.PATTERN) },
        )

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Continue") }
    }
}

@Composable
private fun SetPinStep(
    pin: String,
    onPinChange: (String) -> Unit,
    error: String?,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Set your PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text("Enter a 4–6 digit PIN to protect your vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        // PIN dots display
        PinDotsRow(length = pin.length, maxLength = 6)
        Spacer(Modifier.height(24.dp))

        // Number pad
        NumberPad(
            onDigit = { d -> onPinChange(pin + d) },
            onDelete = { if (pin.isNotEmpty()) onPinChange(pin.dropLast(1)) },
            onSubmit = onNext,
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun SetPasswordStep(
    password: String,
    onPasswordChange: (String) -> Unit,
    error: String?,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Set your Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text("At least 6 characters. Use letters, numbers, and symbols.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Save & Continue")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun AllSetStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.CheckCircle, null,
            tint = ARUUUColors.Success,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text("You're all set!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text("ARUUU is ready to protect your apps.\nHead to the dashboard to add apps to your vault.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Open ARUUU")
        }
    }
}

// ─── Shared UI components ─────────────────────────────────────────────────

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, null, tint = ARUUUColors.CyanPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun AuthOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) ARUUUColors.CyanPrimary else MaterialTheme.colorScheme.outline
    val bgColor = if (selected) ARUUUColors.CyanGlow else MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (selected) ARUUUColors.CyanPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, null,
                tint = ARUUUColors.CyanPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StepProgressDots(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { i ->
            val isActive = i == currentStep
            val width by animateDpAsState(if (isActive) 24.dp else 8.dp, label = "dot_width")
            Box(
                modifier = Modifier
                    .width(width).height(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (i <= currentStep) ARUUUColors.CyanPrimary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

@Composable
private fun PinDotsRow(length: Int, maxLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(maxLength) { i ->
            val filled = i < length
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (filled) ARUUUColors.CyanPrimary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = {
                            when (key) {
                                "⌫"  -> onDelete()
                                ""   -> { /* no-op */ }
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = MaterialTheme.shapes.large,
                        enabled = key.isNotEmpty(),
                    ) {
                        Text(key, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CyanGlowOrb() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ARUUUColors.CyanGlow, Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.1f),
                radius = size.width * 0.6f,
            ),
        )
    }
}
