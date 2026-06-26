package com.aruuu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.aruuu.app.domain.model.ThemeMode
import com.aruuu.app.service.AppLockService
import com.aruuu.app.ui.Routes
import com.aruuu.app.ui.ARUUUNavHost
import com.aruuu.app.ui.screens.MainViewModel
import com.aruuu.app.ui.theme.ARUUUColors
import com.aruuu.app.ui.theme.ARUUUTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host for ARUUU.
 *
 * Responsibilities:
 *  1. Install the splash screen and keep it until settings are loaded.
 *  2. Determine the start destination (Onboarding vs Auth vs Home).
 *  3. Apply the correct theme (light / dark / system).
 *  4. Start the App Lock foreground service.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash until settings load
        splash.setKeepOnScreenCondition { vm.isLoading.value }

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Start App Lock service
        AppLockService.start(this)

        setContent {
            val state by vm.uiState.collectAsState()
            val systemInDark = isSystemInDarkTheme()

            val darkTheme = when (state.themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> systemInDark
            }

            ARUUUTheme(darkTheme = darkTheme) {
                // Transparent status bar
                val sysUiController = rememberSystemUiController()
                val useDarkIcons = !darkTheme
                SideEffect {
                    sysUiController.setSystemBarsColor(
                        color = ARUUUColors.NavyDeep.copy(alpha = 0f),
                        darkIcons = useDarkIcons,
                    )
                }

                if (!state.isLoading) {
                    val startDest = when {
                        !state.onboardingComplete -> Routes.ONBOARDING
                        else                     -> Routes.AUTH
                    }
                    ARUUUNavHost(startDestination = startDest)
                }
            }
        }
    }
}
