package com.aruuu.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aruuu.app.MainActivity
import com.aruuu.app.R
import com.aruuu.app.ARUUUApplication.Companion.CHANNEL_INTRUDER
import com.aruuu.app.data.repository.ARUUURepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silently captures a photo using the front camera after [MAX_FAILED_ATTEMPTS]
 * failed unlock attempts. Saves the JPEG to internal storage and stores a
 * record in the Room database for display in the Intruder Selfie gallery.
 *
 * Uses CameraX with a synthetic LifecycleOwner so it can operate from a
 * non-Activity context.
 */
@Singleton
class IntruderCaptureService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ARUUURepository,
) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun captureIntruder(failedAttempts: Int, targetPkg: String = "") {
        try {
            val lifecycleOwner = createSyntheticLifecycleOwner()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                    val photoFile = createOutputFile()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                cameraProvider.unbindAll()
                                val path = output.savedUri?.path ?: photoFile.absolutePath
                                scope.launch {
                                    repository.saveIntruderRecord(path, failedAttempts, targetPkg)
                                    postIntruderNotification(path)
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                cameraProvider.unbindAll()
                            }
                        }
                    )
                } catch (e: Exception) {
                    cameraProvider.unbindAll()
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            // Camera not available — fail silently
        }
    }

    private fun createOutputFile(): File {
        val dir = File(context.filesDir, "intruder_selfies").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "intruder_$timestamp.jpg")
    }

    private fun postIntruderNotification(imagePath: String) {
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("nav_to", "intruder_log")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_INTRUDER)
            .setContentTitle("⚠️ Unauthorised access attempt")
            .setContentText("Someone tried to unlock a protected app — tap to view")
            .setSmallIcon(R.drawable.ic_aruuu_notification)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(INTRUDER_NOTIFICATION_ID++, notification)
    }

    /** Minimal LifecycleOwner that reports RESUMED so CameraX binds correctly. */
    private fun createSyntheticLifecycleOwner(): LifecycleOwner {
        return object : LifecycleOwner {
            private val lifecycleRegistry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val lifecycle: Lifecycle = lifecycleRegistry
        }
    }

    companion object {
        private var INTRUDER_NOTIFICATION_ID = 2001
    }
}
