package com.clearout.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clearout.app.data.datastore.GamificationDataStore
import com.clearout.app.ui.home.HomeScreen
import com.clearout.app.ui.home.HomeViewModel
import com.clearout.app.ui.onboarding.OnboardingScreen
import com.clearout.app.ui.results.ResultsScreen
import com.clearout.app.ui.splash.SplashScreen
import com.clearout.app.ui.swipe.SwipeScreen
import com.clearout.app.ui.swipe.SwipeViewModel

@Composable
fun AppNavigation(dataStore: GamificationDataStore) {
    val navController = rememberNavController()
    val gamificationState by dataStore.gamificationFlow.collectAsState(initial = null)

    NavHost(
        navController = navController,
        // Always start with the branded Compose splash screen
        startDestination = Screen.Splash.route
    ) {
        // 0. Animated Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    // After splash, decide where to go based on onboarding state
                    val destination = if (gamificationState?.onboardingCompleted == true) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 1. Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                dataStore = dataStore,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Dashboard Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onStartClearing = {
                    navController.navigate(Screen.Swipe.route)
                }
            )
        }

        // 3. Swipe Purger Screen
        composable(Screen.Swipe.route) {
            val swipeViewModel: SwipeViewModel = hiltViewModel()
            SwipeScreen(
                viewModel = swipeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResults = { photosDeleted, bytesFreed ->
                    navController.navigate(Screen.Results.createRoute(photosDeleted, bytesFreed)) {
                        popUpTo(Screen.Swipe.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Session Results Completion Screen
        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("photosDeleted") { type = NavType.IntType },
                navArgument("bytesFreed") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val photosDeleted = backStackEntry.arguments?.getInt("photosDeleted") ?: 0
            val bytesFreed = backStackEntry.arguments?.getLong("bytesFreed") ?: 0L

            ResultsScreen(
                photosDeleted = photosDeleted,
                bytesFreed = bytesFreed,
                onBackToDashboard = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Results.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
