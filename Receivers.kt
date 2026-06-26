package com.aruuu.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aruuu.app.service.AppLockService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Restarts the App Lock foreground service after device reboot.
 * Requires RECEIVE_BOOT_COMPLETED permission.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            AppLockService.start(context)
        }
    }
}

/**
 * Handles screen off (lock vault) and user present (allow unlock) events.
 */
class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // The vault automatically locks when the screen turns off.
                // AppLockService will handle re-authentication on next foreground.
                VaultLockState.isUnlocked = false
            }
            Intent.ACTION_USER_PRESENT -> {
                // Screen unlocked by device owner — still require ARUUU auth
                // (vault remains locked until user authenticates via ARUUU)
            }
        }
    }
}

/**
 * In-memory vault unlock state.
 *
 * Intentionally NOT persisted — the vault re-locks whenever the process
 * dies or the screen turns off (configurable via autoLockDelay).
 */
object VaultLockState {
    @Volatile var isUnlocked: Boolean = false
    @Volatile var unlockedAt: Long = 0L

    fun unlock() {
        isUnlocked = true
        unlockedAt = System.currentTimeMillis()
    }

    fun lock() {
        isUnlocked = false
        unlockedAt = 0L
    }

    /** Returns true if the vault should still be considered unlocked given [delaySeconds]. */
    fun isWithinAutoLockWindow(delaySeconds: Int): Boolean {
        if (!isUnlocked) return false
        if (delaySeconds == -1) return true          // Never auto-lock
        if (delaySeconds == 0) return false          // Immediate lock
        val elapsed = (System.currentTimeMillis() - unlockedAt) / 1000
        return elapsed < delaySeconds
    }
}
