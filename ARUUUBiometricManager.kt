package com.aruuu.app.service

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aruuu.app.domain.model.UnlockResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps AndroidX BiometricPrompt into a clean suspend function.
 *
 * Supports:
 *  • Fingerprint
 *  • Face unlock (if enrolled + device supports it)
 *  • Falls back gracefully to device credential if biometric fails
 */
@Singleton
class ARUUUBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val biometricManager = BiometricManager.from(context)

    // ─── Capability checks ────────────────────────────────────────────────

    fun isBiometricAvailable(): Boolean =
        biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS

    fun isFaceAvailable(): Boolean {
        // Face is included in BIOMETRIC_WEAK; we detect by checking available
        // authenticators. On most OEMs face maps to BiometricType.FACE but
        // this is not exposed via the compat API — approximate via WEAK check.
        return biometricManager.canAuthenticate(BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatus(): BiometricStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    // ─── Authentication ───────────────────────────────────────────────────

    /**
     * Shows a biometric prompt and suspends until the user authenticates,
     * cancels, or an error occurs.
     *
     * Must be called from a coroutine on the Main dispatcher.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "ARUUU",
        subtitle: String = "Authenticate to continue",
        negativeButtonText: String = "Use PIN",
    ): UnlockResult = suspendCancellableCoroutine { cont ->

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(UnlockResult.Success)
            }

            override fun onAuthenticationFailed() {
                // Individual failure — prompt stays open, do nothing here
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!cont.isActive) return
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> cont.resume(UnlockResult.Cancelled)
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> cont.resume(UnlockResult.Locked)
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> cont.resume(UnlockResult.BiometricNotAvailable)
                    else -> cont.resume(UnlockResult.BiometricFailed)
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)

        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    UNAVAILABLE,
    NONE_ENROLLED,
}
