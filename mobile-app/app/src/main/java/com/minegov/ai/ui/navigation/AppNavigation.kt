package com.minegov.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minegov.ai.ui.auth.LoginScreen
import com.minegov.ai.ui.home.HomeScreen
import com.minegov.ai.ui.notifications.NotificationsScreen
import com.minegov.ai.ui.profile.ProfileScreen
import com.minegov.ai.ui.splash.SplashScreen
import com.minegov.ai.ui.violation.NewViolationScreen
import com.minegov.ai.ui.violations.MyViolationsScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val NEW_VIOLATION = "new_violation"
    const val MY_VIOLATIONS = "my_violations"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Login
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Home
        composable(Routes.HOME) {
            HomeScreen(
                onNewViolation = {
                    navController.navigate(Routes.NEW_VIOLATION)
                },
                onMyViolations = {
                    navController.navigate(Routes.MY_VIOLATIONS)
                },
                onNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                },
                onProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        // New Violation
        composable(Routes.NEW_VIOLATION) {
            NewViolationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // My Violations
        composable(Routes.MY_VIOLATIONS) {
            MyViolationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Notifications
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Profile
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}