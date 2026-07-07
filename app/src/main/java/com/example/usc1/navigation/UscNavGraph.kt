package com.example.usc1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.usc1.core.ui.ModulePlaceholderScreen
import com.example.usc1.domain.model.AppModules
import com.example.usc1.ui.dashboard.DashboardScreen

@Composable
fun UscNavGraph() {
    val navController = rememberNavController()
    val modules = remember { AppModules.androidModules }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Dashboard,
    ) {
        composable(AppRoute.Dashboard) {
            DashboardScreen(
                modules = modules,
                onOpenModule = { module ->
                    navController.navigate(module.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        modules.forEach { module ->
            composable(module.route) {
                ModulePlaceholderScreen(
                    module = module,
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }
}
