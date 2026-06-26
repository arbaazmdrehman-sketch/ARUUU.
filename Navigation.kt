package com.aruuu.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aruuu.app.ui.screens.home.HomeScreen
import com.aruuu.app.ui.screens.apps.ManageAppsScreen
import com.aruuu.app.ui.screens.apps.IntruderLogScreen
import com.aruuu.app.ui.screens.auth.AuthScreen
import com.aruuu.app.ui.screens.onboarding.OnboardingScreen
import com.aruuu.app.ui.screens.settings.SettingsScreen

// ═══════════════════════════════════════════════════════════════════════════
// Route constants
// ═══════════════════════════════════════════════════════════════════════════

object Routes {
    const val ONBOARDING    = "onboarding"
    const val AUTH          = "auth"
    const val HOME          = "home"
    const val MANAGE_APPS   = "manage_apps"
    const val INTRUDER_LOG  = "intruder_log"
    const val SETTINGS      = "settings"
}

// ═══════════════════════════════════════════════════════════════════════════
// Nav host
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ARUUUNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) +
                    fadeIn(tween(300))
        },
        exitTransition   = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) +
                    fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) +
                    fadeIn(tween(300))
        },
        popExitTransition  = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) +
                    fadeOut(tween(300))
        },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToManageApps = { navController.navigate(Routes.MANAGE_APPS) },
                onNavigateToIntruderLog = { navController.navigate(Routes.INTRUDER_LOG) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.MANAGE_APPS) {
            ManageAppsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.INTRUDER_LOG) {
            IntruderLogScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetVault = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
