package com.aruuu.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aruuu.app.MainActivity
import com.aruuu.app.R
import com.aruuu.app.ARUUUApplication.Companion.CHANNEL_APP_LOCK
import com.aruuu.app.data.repository.ARUUURepository
import com.aruuu.app.ui.screens.auth.AppLockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Foreground service that polls UsageStatsManager every 500 ms to detect
 * when a locked app comes to the foreground, then launches AppLockActivity
 * over the top of it.
 *
 * ⚠️  Requires PACKAGE_USAGE_STATS permission (granted manually by user
 *      via Settings → Apps → Special app access → Usage access).
 */
@AndroidEntryPoint
class AppLockService : Service() {

    @Inject lateinit var repository: ARUUURepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private var lastForegroundPkg = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoring()
        return START_STICKY     // Restart automatically if killed
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ─── Monitoring loop ──────────────────────────────────────────────────

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                checkForegroundApp()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val pkg = getForegroundPackage() ?: return
        if (pkg == lastForegroundPkg) return
        lastForegroundPkg = pkg

        // Skip ARUUU itself and system launcher
        if (pkg == packageName || pkg.startsWith("com.android.launcher")) return

        if (repository.isLocked(pkg)) {
            repository.updateLastAccessed(pkg)
            launchLockScreen(pkg)
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 3_000L   // last 3 seconds
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        return stats
            ?.filter { it.lastTimeUsed > begin }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private fun launchLockScreen(lockedPkg: String) {
        val intent = Intent(this, AppLockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AppLockActivity.EXTRA_LOCKED_PACKAGE, lockedPkg)
        }
        startActivity(intent)
    }

    // ─── Notification ─────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_APP_LOCK)
            .setContentTitle("ARUUU active")
            .setContentText("App lock protection is running")
            .setSmallIcon(R.drawable.ic_aruuu_notification)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 500L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AppLockService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppLockService::class.java))
        }
    }
}
