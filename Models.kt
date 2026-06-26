package com.aruuu.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ═══════════════════════════════════════════════════════════════════════════
// AppInfo — represents one installed app
// ═══════════════════════════════════════════════════════════════════════════

@Parcelize
data class AppInfo(
    /** Full package name e.g. "com.google.android.youtube" */
    val packageName: String,
    /** Human-readable app label */
    val appName: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val isSystemApp: Boolean = false,
    /** Managed inside ARUUU — hidden from recents / launcher */
    val isHidden: Boolean = false,
    /** App lock active — requires auth before launch */
    val isLocked: Boolean = false,
    val installedAt: Long = 0L,
) : Parcelable

// ═══════════════════════════════════════════════════════════════════════════
// Auth method
// ═══════════════════════════════════════════════════════════════════════════

enum class AuthMethod {
    PIN,
    PASSWORD,
    PATTERN,
    BIOMETRIC;

    val displayName: String
        get() = when (this) {
            PIN -> "PIN"
            PASSWORD -> "Password"
            PATTERN -> "Pattern"
            BIOMETRIC -> "Biometric"
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// VaultSettings — persisted in DataStore
// ═══════════════════════════════════════════════════════════════════════════

data class VaultSettings(
    val primaryAuthMethod: AuthMethod = AuthMethod.PIN,
    val biometricEnabled: Boolean = false,
    val faceUnlockEnabled: Boolean = false,
    val autoLockDelaySeconds: Int = 30,         // 0 = immediate, -1 = never
    val intruderSelfieEnabled: Boolean = true,
    val maxFailedAttempts: Int = 3,
    val disguiseModeEnabled: Boolean = false,
    val disguiseLabel: String = "Calculator",   // Launcher label when disguised
    val hapticFeedback: Boolean = true,
    val showNotificationBadge: Boolean = false,
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastBackupTimestamp: Long = 0L,
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

// ═══════════════════════════════════════════════════════════════════════════
// IntruderRecord — stored after failed unlock attempts
// ═══════════════════════════════════════════════════════════════════════════

data class IntruderRecord(
    val id: Long = 0L,
    val timestampMs: Long,
    val imagePath: String,          // Absolute path to JPEG selfie
    val failedAttemptCount: Int,
    val targetPackage: String = "", // App the intruder was trying to unlock
)

// ═══════════════════════════════════════════════════════════════════════════
// Unlock result
// ═══════════════════════════════════════════════════════════════════════════

sealed class UnlockResult {
    object Success : UnlockResult()
    data class Failure(val attemptsRemaining: Int) : UnlockResult()
    object Locked : UnlockResult()       // Too many failures, temporarily locked
    object BiometricNotAvailable : UnlockResult()
    object BiometricFailed : UnlockResult()
    object Cancelled : UnlockResult()
}

// ═══════════════════════════════════════════════════════════════════════════
// Onboarding step
// ═══════════════════════════════════════════════════════════════════════════

enum class OnboardingStep {
    WELCOME,
    CHOOSE_AUTH,
    SET_PIN,
    SET_PASSWORD,
    SET_PATTERN,
    BIOMETRIC_ENROL,
    DISGUISE,
    PERMISSIONS,
    COMPLETE,
}
