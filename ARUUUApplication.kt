package com.aruuu.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * ARUUU Application class.
 *
 * Initialises Hilt dependency injection and creates the persistent
 * notification channel used by the App Lock foreground service.
 */
@HiltAndroidApp
class ARUUUApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Channels (required for Android 8+)
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // App Lock service persistent notification
            NotificationChannel(
                CHANNEL_APP_LOCK,
                "App Lock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ARUUU app lock active in the background"
                setShowBadge(false)
                manager.createNotificationChannel(this)
            }

            // Intruder selfie alert
            NotificationChannel(
                CHANNEL_INTRUDER,
                "Intruder Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when an unauthorised access attempt is detected"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_APP_LOCK = "aruuu_app_lock"
        const val CHANNEL_INTRUDER = "aruuu_intruder"
    }
}
