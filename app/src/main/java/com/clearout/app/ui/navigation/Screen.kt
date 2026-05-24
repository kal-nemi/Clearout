package com.clearout.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Swipe : Screen("swipe")
    data object Results : Screen("results/{photosDeleted}/{bytesFreed}") {
        fun createRoute(photosDeleted: Int, bytesFreed: Long): String {
            return "results/$photosDeleted/$bytesFreed"
        }
    }
}
