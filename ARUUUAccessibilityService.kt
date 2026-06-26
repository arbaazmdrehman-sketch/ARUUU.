package com.aruuu.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.ui.screens.auth.AppLockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Accessibility-based foreground app detector.
 *
 * This is the primary detection method on Android 12+. When the user
 * grants ARUUU accessibility access, this service receives
 * TYPE_WINDOW_STATE_CHANGED events whenever a new Activity window appears.
 *
 * Note: requires user to manually enable in Settings → Accessibility → ARUUU.
 */
@AndroidEntryPoint
class ARUUUAccessibilityService : AccessibilityService() {

    @Inject lateinit var repository: ARUUURepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastCheckedPkg = ""

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Skip system UI, launcher, ARUUU itself
        if (pkg == packageName ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.launcher") ||
            pkg == lastCheckedPkg
        ) return

        lastCheckedPkg = pkg

        scope.launch {
            if (repository.isLocked(pkg)) {
                repository.updateLastAccessed(pkg)
                val intent = Intent(this@ARUUUAccessibilityService, AppLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(AppLockActivity.EXTRA_LOCKED_PACKAGE, pkg)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        scope.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
