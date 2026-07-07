package com.example.usc1.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.usc1.core.ui.ModulePlaceholderScreen
import com.example.usc1.domain.model.AppModules
import com.example.usc1.ui.auth.AccountSecurityScreen
import com.example.usc1.ui.auth.AuthViewModel
import com.example.usc1.ui.auth.BannedUserScreen
import com.example.usc1.ui.auth.InviteRequiredScreen
import com.example.usc1.ui.auth.LoginScreen
import com.example.usc1.ui.auth.RegisterScreen
import com.example.usc1.ui.auth.WaitingApprovalScreen
import com.example.usc1.ui.home.HomeScreen
import com.example.usc1.ui.home.HomeViewModel
import com.example.usc1.ui.membershipCard.MembershipCardScreen
import com.example.usc1.ui.membershipCard.MembershipCardViewModel
import com.example.usc1.ui.profile.ProfileScreen
import com.example.usc1.ui.profile.ProfileViewModel
import com.example.usc1.ui.settings.SettingsAction
import com.example.usc1.ui.settings.SettingsScreen
import com.example.usc1.ui.settings.SettingsViewModel

@Composable
fun UscNavGraph() {
    val navController = rememberNavController()
    val modules = remember { AppModules.androidModules }
    val concreteModuleRoutes = remember {
        setOf(
            AppRoute.Dashboard,
            AppRoute.Profile,
            AppRoute.Settings,
            AppRoute.MembershipCard,
        )
    }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val guardedRoute = RouteGuard.destinationFor(authState)

    NavHost(
        navController = navController,
        startDestination = AppRoute.Login,
    ) {
        composable(AppRoute.Login) {
            LoginScreen(
                state = authState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onLoginClick = authViewModel::signIn,
                onRegisterClick = { navController.navigate(AppRoute.Register) },
                onSecurityClick = { navController.navigate(AppRoute.AccountSecurity) },
                onMockWaitingClick = authViewModel::simulateWaitingApproval,
                onMockInviteClick = authViewModel::simulateInviteRequired,
                onMockBannedClick = authViewModel::simulateBanned,
            )
        }

        composable(AppRoute.Register) {
            RegisterScreen(
                state = authState,
                onFullNameChange = authViewModel::onFullNameChange,
                onEmailChange = authViewModel::onEmailChange,
                onInviteCodeChange = authViewModel::onInviteCodeChange,
                onRegisterClick = authViewModel::register,
                onBackToLoginClick = { navController.navigate(AppRoute.Login) },
            )
        }

        composable(AppRoute.WaitingApproval) {
            WaitingApprovalScreen(
                state = authState,
                onRefreshClick = authViewModel::refreshApproval,
                onSignOutClick = authViewModel::signOut,
            )
        }

        composable(AppRoute.InviteRequired) {
            InviteRequiredScreen(
                state = authState,
                onRegisterClick = { navController.navigate(AppRoute.Register) },
                onSignOutClick = authViewModel::signOut,
            )
        }

        composable(AppRoute.BannedUser) {
            BannedUserScreen(
                state = authState,
                onSupportClick = authViewModel::clearError,
                onSignOutClick = authViewModel::signOut,
            )
        }

        composable(AppRoute.AccountSecurity) {
            AccountSecurityScreen(
                onBackClick = { navController.navigateUp() },
                onRecoverAccountClick = authViewModel::clearError,
            )
        }

        composable(AppRoute.Dashboard) {
            val homeViewModel: HomeViewModel = viewModel()
            val homeState by homeViewModel.uiState.collectAsState()

            HomeScreen(
                state = homeState,
                onQuickActionClick = { action ->
                    navController.navigate(action.route) {
                        launchSingleTop = true
                    }
                },
                onModuleClick = { module ->
                    navController.navigate(module.route) {
                        launchSingleTop = true
                    }
                },
                onRetryClick = homeViewModel::refresh,
            )
        }

        composable(AppRoute.Profile) {
            val profileViewModel: ProfileViewModel = viewModel()
            val profileState by profileViewModel.uiState.collectAsState()

            ProfileScreen(
                state = profileState,
                onShortcutClick = { shortcut ->
                    navController.navigate(shortcut.route) {
                        launchSingleTop = true
                    }
                },
                onRetryClick = profileViewModel::refresh,
            )
        }

        composable(AppRoute.Settings) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            SettingsScreen(
                state = settingsState,
                onItemClick = { item ->
                    if (item.action == SettingsAction.SignOut) {
                        authViewModel.signOut()
                    } else {
                        item.action.route?.let { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
            )
        }

        composable(AppRoute.MembershipCard) {
            val membershipCardViewModel: MembershipCardViewModel = viewModel()
            val membershipCardState by membershipCardViewModel.uiState.collectAsState()

            MembershipCardScreen(
                state = membershipCardState,
                onRefreshClick = membershipCardViewModel::refresh,
                onBackClick = { navController.navigateUp() },
            )
        }

        modules.filterNot { module -> module.route in concreteModuleRoutes }.forEach { module ->
            composable(module.route) {
                ModulePlaceholderScreen(
                    module = module,
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }

    LaunchedEffect(guardedRoute) {
        val targetRoute = guardedRoute ?: return@LaunchedEffect
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == targetRoute) return@LaunchedEffect

        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = targetRoute != AppRoute.Login
            }
            launchSingleTop = true
        }
    }
}
