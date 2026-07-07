package com.example.usc1.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.usc1.core.ui.ModulePlaceholderScreen
import com.example.usc1.data.repository.MockEventsRepository
import com.example.usc1.domain.model.AppModules
import com.example.usc1.ui.auth.AccountSecurityScreen
import com.example.usc1.ui.auth.AuthViewModel
import com.example.usc1.ui.auth.BannedUserScreen
import com.example.usc1.ui.auth.InviteRequiredScreen
import com.example.usc1.ui.auth.LoginScreen
import com.example.usc1.ui.auth.RegisterScreen
import com.example.usc1.ui.auth.WaitingApprovalScreen
import com.example.usc1.ui.events.EventCheckoutScreen
import com.example.usc1.ui.events.EventCheckoutUiState
import com.example.usc1.ui.events.EventDetailScreen
import com.example.usc1.ui.events.EventDetailViewModel
import com.example.usc1.ui.events.EventsScreen
import com.example.usc1.ui.events.EventsViewModel
import com.example.usc1.ui.home.HomeScreen
import com.example.usc1.ui.home.HomeViewModel
import com.example.usc1.ui.membershipCard.MembershipCardScreen
import com.example.usc1.ui.membershipCard.MembershipCardViewModel
import com.example.usc1.ui.orders.EventOrderDetailScreen
import com.example.usc1.ui.orders.EventOrderDetailViewModel
import com.example.usc1.ui.orders.EventOrdersScreen
import com.example.usc1.ui.orders.EventOrdersViewModel
import com.example.usc1.ui.profile.ProfileScreen
import com.example.usc1.ui.profile.ProfileViewModel
import com.example.usc1.ui.settings.SettingsAction
import com.example.usc1.ui.settings.SettingsScreen
import com.example.usc1.ui.settings.SettingsViewModel
import com.example.usc1.ui.tickets.EventTicketDetailScreen
import com.example.usc1.ui.tickets.EventTicketDetailViewModel
import com.example.usc1.ui.tickets.EventTicketsScreen
import com.example.usc1.ui.tickets.EventTicketsViewModel

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
            AppRoute.Events,
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

        composable(AppRoute.Events) {
            val eventsViewModel: EventsViewModel = viewModel()
            val eventsState by eventsViewModel.uiState.collectAsState()

            EventsScreen(
                state = eventsState,
                onEventClick = { event ->
                    navController.navigate(AppRoute.eventDetail(event.id)) {
                        launchSingleTop = true
                    }
                },
                onStatusFilterClick = eventsViewModel::loadEvents,
                onTicketsClick = {
                    navController.navigate(AppRoute.EventTickets) {
                        launchSingleTop = true
                    }
                },
                onOrdersClick = {
                    navController.navigate(AppRoute.EventOrders) {
                        launchSingleTop = true
                    }
                },
                onRetryClick = { eventsViewModel.loadEvents() },
            )
        }

        composable(
            route = AppRoute.EventDetail,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            val eventDetailViewModel: EventDetailViewModel = viewModel()
            val eventDetailState by eventDetailViewModel.uiState.collectAsState()

            LaunchedEffect(eventId) {
                eventDetailViewModel.loadEvent(eventId)
            }

            EventDetailScreen(
                state = eventDetailState,
                onCheckoutClick = { event ->
                    navController.navigate(AppRoute.eventCheckout(event.id)) {
                        launchSingleTop = true
                    }
                },
                onTicketsClick = {
                    navController.navigate(AppRoute.EventTickets) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.EventCheckout,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            val event = remember(eventId) {
                MockEventsRepository.mockEvents.firstOrNull { it.id == eventId }
                    ?: MockEventsRepository.mockEvents.first()
            }

            EventCheckoutScreen(
                state = EventCheckoutUiState(event = event),
                onConfirmClick = {
                    navController.navigate(AppRoute.EventOrders) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.EventTickets) {
            val ticketsViewModel: EventTicketsViewModel = viewModel()
            val ticketsState by ticketsViewModel.uiState.collectAsState()

            EventTicketsScreen(
                state = ticketsState,
                onTicketClick = { ticket ->
                    navController.navigate(AppRoute.eventTicketDetail(ticket.id)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = AppRoute.EventTicketDetail,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId").orEmpty()
            val ticketDetailViewModel: EventTicketDetailViewModel = viewModel()
            val ticketDetailState by ticketDetailViewModel.uiState.collectAsState()

            LaunchedEffect(ticketId) {
                ticketDetailViewModel.loadTicket(ticketId)
            }

            EventTicketDetailScreen(
                state = ticketDetailState,
                onTransferClick = {},
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.EventOrders) {
            val ordersViewModel: EventOrdersViewModel = viewModel()
            val ordersState by ordersViewModel.uiState.collectAsState()

            EventOrdersScreen(
                state = ordersState,
                onOrderClick = { order ->
                    navController.navigate(AppRoute.eventOrderDetail(order.id)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = AppRoute.EventOrderDetail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val orderDetailViewModel: EventOrderDetailViewModel = viewModel()
            val orderDetailState by orderDetailViewModel.uiState.collectAsState()

            LaunchedEffect(orderId) {
                orderDetailViewModel.loadOrder(orderId)
            }

            EventOrderDetailScreen(
                state = orderDetailState,
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
