package com.example.usc1.navigation

import android.net.Uri
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.ui.PermissionDeniedScreen
import com.example.usc1.core.ui.ModulePlaceholderScreen
import com.example.usc1.domain.model.AppModules
import com.example.usc1.domain.model.AdminMiniVendorDirectoryMode
import com.example.usc1.domain.model.AdminPlanSubscriptionListKind
import com.example.usc1.domain.model.AdminReportsSection
import com.example.usc1.domain.model.AdminStoreOrdersMode
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.ui.admin.AdminAlbumScreen
import com.example.usc1.ui.admin.AdminAlbumViewModel
import com.example.usc1.ui.admin.AdminPartnersActiveScreen
import com.example.usc1.ui.admin.AdminPartnersBiScreen
import com.example.usc1.ui.admin.AdminPartnersCompaniesScreen
import com.example.usc1.ui.admin.AdminPartnersHistoryScreen
import com.example.usc1.ui.admin.AdminPartnersHubScreen
import com.example.usc1.ui.admin.AdminPartnersViewModel
import com.example.usc1.ui.admin.AdminDashboardModulesScreen
import com.example.usc1.ui.admin.AdminDashboardModulesViewModel
import com.example.usc1.ui.admin.AdminDashboardScreen
import com.example.usc1.ui.admin.AdminDashboardViewModel
import com.example.usc1.ui.admin.AdminDatabaseScannerScreen
import com.example.usc1.ui.admin.AdminDatabaseScannerViewModel
import com.example.usc1.domain.model.AdminStoreMenuKind
import com.example.usc1.ui.admin.AdminGamesScreen
import com.example.usc1.ui.admin.AdminGamesViewModel
import com.example.usc1.ui.admin.AdminMiniVendorsHubScreen
import com.example.usc1.ui.admin.AdminMiniVendorsScreen
import com.example.usc1.ui.admin.AdminMiniVendorsViewModel
import com.example.usc1.ui.admin.AdminManagementHubScreen
import com.example.usc1.ui.admin.AdminActivityLogsScreen
import com.example.usc1.ui.admin.AdminActivityLogsViewModel
import com.example.usc1.ui.admin.AdminMentorshipScreen
import com.example.usc1.ui.admin.AdminMentorshipViewModel
import com.example.usc1.ui.admin.AdminPendingRouteScreen
import com.example.usc1.ui.admin.AdminPermissionUsersScreen
import com.example.usc1.ui.admin.AdminPermissionUsersViewModel
import com.example.usc1.ui.admin.AdminPlanAuditScreen
import com.example.usc1.ui.admin.AdminPlanAuditViewModel
import com.example.usc1.ui.admin.AdminPlanSubscriptionsScreen
import com.example.usc1.ui.admin.AdminPlanSubscriptionsViewModel
import com.example.usc1.ui.admin.AdminPlansHubScreen
import com.example.usc1.ui.admin.AdminReportsHubScreen
import com.example.usc1.ui.admin.AdminReportsListScreen
import com.example.usc1.ui.admin.AdminReportsViewModel
import com.example.usc1.ui.admin.AdminStoreCategoriesScreen
import com.example.usc1.ui.admin.AdminStoreCategoriesViewModel
import com.example.usc1.ui.admin.AdminStoreOrdersScreen
import com.example.usc1.ui.admin.AdminStoreOrdersViewModel
import com.example.usc1.ui.admin.AdminStoreProductsScreen
import com.example.usc1.ui.admin.AdminStoreProductsViewModel
import com.example.usc1.ui.admin.AdminStoreScreen
import com.example.usc1.ui.admin.AdminStoreReviewsScreen
import com.example.usc1.ui.admin.AdminStoreReviewsViewModel
import com.example.usc1.ui.admin.AdminStoreViewModel
import com.example.usc1.ui.admin.AdminTenantPoliciesScreen
import com.example.usc1.ui.admin.AdminTenantPoliciesViewModel
import com.example.usc1.ui.admin.AdminUserDetailScreen
import com.example.usc1.ui.admin.AdminUserDetailViewModel
import com.example.usc1.ui.admin.AdminUsersScreen
import com.example.usc1.ui.admin.AdminUsersViewModel
import com.example.usc1.ui.auth.AccountSecurityScreen
import com.example.usc1.ui.auth.AuthViewModel
import com.example.usc1.ui.auth.BannedUserScreen
import com.example.usc1.ui.auth.InviteRequiredScreen
import com.example.usc1.ui.auth.LoginScreen
import com.example.usc1.ui.auth.RegisterScreen
import com.example.usc1.ui.auth.WaitingApprovalScreen
import com.example.usc1.ui.events.EventCheckoutUnavailableScreen
import com.example.usc1.ui.events.EventDetailScreen
import com.example.usc1.ui.events.EventDetailViewModel
import com.example.usc1.ui.events.EventFlowUnavailableScreen
import com.example.usc1.ui.events.EventsScreen
import com.example.usc1.ui.events.EventsViewModel
import com.example.usc1.ui.home.HomeScreen
import com.example.usc1.ui.home.HomeViewModel
import com.example.usc1.ui.home.withSession
import com.example.usc1.ui.membershipCard.MembershipCardScreen
import com.example.usc1.ui.membershipCard.MembershipCardViewModel
import com.example.usc1.ui.membershipCard.withSession
import com.example.usc1.ui.partners.PartnerBenefitsScreen
import com.example.usc1.ui.partners.PartnerDetailScreen
import com.example.usc1.ui.partners.PartnersScreen
import com.example.usc1.ui.partners.PartnersViewModel
import com.example.usc1.ui.plans.PlanDetailScreen
import com.example.usc1.ui.plans.PlanOrdersScreen
import com.example.usc1.ui.plans.PlansMockData
import com.example.usc1.ui.plans.PlansScreen
import com.example.usc1.ui.plans.PlansViewModel
import com.example.usc1.ui.plans.UserPlanStatusScreen
import com.example.usc1.ui.profile.ProfileScreen
import com.example.usc1.ui.profile.ProfileViewModel
import com.example.usc1.ui.profile.withSession
import com.example.usc1.ui.settings.SettingsAction
import com.example.usc1.ui.settings.SettingsScreen
import com.example.usc1.ui.settings.SettingsViewModel
import com.example.usc1.ui.settings.withSession
import com.example.usc1.ui.store.CartScreen
import com.example.usc1.ui.store.CartViewModel
import com.example.usc1.ui.store.CheckoutScreen
import com.example.usc1.ui.store.ProductDetailStateScreen
import com.example.usc1.ui.store.ProductDetailViewModel
import com.example.usc1.ui.store.StoreOrderDetailUnavailableScreen
import com.example.usc1.ui.store.StoreOrdersScreen
import com.example.usc1.ui.store.StoreOrdersViewModel
import com.example.usc1.ui.store.StoreScreen
import com.example.usc1.ui.store.StoreViewModel
import com.example.usc1.ui.training.TrainingCheckInDetailScreen
import com.example.usc1.ui.training.TrainingCheckInScreen
import com.example.usc1.ui.training.TrainingFrequencyScreen
import com.example.usc1.ui.training.TrainingHistoryScreen
import com.example.usc1.ui.training.TrainingMockData
import com.example.usc1.ui.training.TrainingScreen
import com.example.usc1.ui.training.TrainingViewModel

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
            AppRoute.Store,
            AppRoute.Plans,
            AppRoute.Training,
            AppRoute.Gym,
            AppRoute.Partners,
            AppRoute.Community,
            AppRoute.Leagues,
            AppRoute.Directory,
            AppRoute.Commissions,
            AppRoute.Tenant,
            AppRoute.MiniVendor,
            AppRoute.Scanner,
            AppRoute.Guide,
            AppRoute.Legal,
            AppRoute.Album,
            AppRoute.Games,
            AppRoute.Boardround,
            AppRoute.Achievements,
            AppRoute.Loyalty,
            AppRoute.OrdersHub,
            AppRoute.Admin,
        )
    }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val guardedRoute = RouteGuard.redirectFor(authState, currentRoute)

    NavHost(
        navController = navController,
        startDestination = AppRoute.Login,
    ) {
        composable(AppRoute.Login) {
            LoginScreen(
                state = authState,
                onGoogleClick = authViewModel::signInWithGoogle,
                onGuestClick = authViewModel::loginAsGuest,
            )
        }

        composable(AppRoute.Register) {
            RegisterScreen(
                state = authState,
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

            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                homeViewModel.load(authState.session)
            }

            HomeScreen(
                state = homeState.withSession(authState.session),
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onSignOut = authViewModel::signOut,
                onRetryClick = homeViewModel::refresh,
            )
        }

        composable(AppRoute.Profile) {
            val profileViewModel: ProfileViewModel = viewModel()
            val profileState by profileViewModel.uiState.collectAsState()

            ProfileScreen(
                state = profileState.withSession(authState.session),
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
                state = settingsState.withSession(authState.session),
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
                state = membershipCardState.withSession(authState.session),
                onRefreshClick = membershipCardViewModel::refresh,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Admin) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Painel admin",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val adminDashboardViewModel: AdminDashboardViewModel = viewModel()
                val adminState by adminDashboardViewModel.uiState.collectAsState()

                LaunchedEffect(authState.session.tenant?.id, user.id) {
                    adminDashboardViewModel.load(authState.session)
                }

                AdminDashboardScreen(
                    state = adminState,
                    onModulesClick = {
                        navController.navigate(AppRoute.AdminDashboardModules) {
                            launchSingleTop = true
                        }
                    },
                    onPoliciesClick = {
                        navController.navigate(AppRoute.AdminPolicies) {
                            launchSingleTop = true
                        }
                    },
                    onUsersClick = {
                        navController.navigate(AppRoute.AdminUsers) {
                            launchSingleTop = true
                        }
                    },
                    onStoreClick = {
                        navController.navigate(AppRoute.AdminStore) {
                            launchSingleTop = true
                        }
                    },
                    onRefreshClick = { adminDashboardViewModel.load(authState.session, forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminDashboardModules) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Modulos do App",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val modulesViewModel: AdminDashboardModulesViewModel = viewModel()
                val modulesState by modulesViewModel.uiState.collectAsState()

                LaunchedEffect(authState.session.tenant?.id, user.id) {
                    modulesViewModel.load(authState.session)
                }

                AdminDashboardModulesScreen(
                    state = modulesState,
                    onToggleModule = modulesViewModel::toggleModule,
                    onSaveClick = modulesViewModel::save,
                    onRefreshClick = { modulesViewModel.load(authState.session, forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminPolicies) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Políticas públicas",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val policiesViewModel: AdminTenantPoliciesViewModel = viewModel()
                val policiesState by policiesViewModel.uiState.collectAsState()

                LaunchedEffect(authState.session.tenant?.id, user.id) {
                    policiesViewModel.load(authState.session)
                }

                AdminTenantPoliciesScreen(
                    state = policiesState,
                    onContentChange = policiesViewModel::updatePolicyContent,
                    onToggleVisibility = policiesViewModel::togglePolicyVisibility,
                    onSaveClick = policiesViewModel::save,
                    onRefreshClick = { policiesViewModel.load(authState.session, forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminUsers) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Usuários",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val usersViewModel: AdminUsersViewModel = viewModel()
                val usersState by usersViewModel.uiState.collectAsState()

                AdminUsersScreen(
                    state = usersState,
                    onSearchChange = usersViewModel::onSearchChange,
                    onPlanFilterChange = usersViewModel::onPlanFilterChange,
                    onLetterGroupChange = usersViewModel::onLetterGroupChange,
                    onEditUser = { userItem ->
                        navController.navigate(AppRoute.adminUserDetail(userItem.id)) {
                            launchSingleTop = true
                        }
                    },
                    onToggleStatus = usersViewModel::toggleStatus,
                    onRequestDelete = usersViewModel::requestDelete,
                    onCancelDelete = usersViewModel::cancelDelete,
                    onConfirmDelete = usersViewModel::confirmDelete,
                    onLoadMoreClick = usersViewModel::loadMore,
                    onRefreshClick = usersViewModel::refresh,
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(
            route = AppRoute.AdminUserDetail,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Perfil do Usuário",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val userDetailViewModel: AdminUserDetailViewModel = viewModel()
                val userDetailState by userDetailViewModel.uiState.collectAsState()

                LaunchedEffect(userId) {
                    userDetailViewModel.load(userId)
                }

                LaunchedEffect(userDetailState.shouldNavigateBack) {
                    if (userDetailState.shouldNavigateBack) {
                        userDetailViewModel.consumeNavigateBack()
                        navController.navigateUp()
                    }
                }

                AdminUserDetailScreen(
                    state = userDetailState,
                    onNomeChange = userDetailViewModel::updateNome,
                    onTelefoneChange = userDetailViewModel::updateTelefone,
                    onMatriculaChange = userDetailViewModel::updateMatricula,
                    onTurmaChange = userDetailViewModel::updateTurma,
                    onPlanoChange = userDetailViewModel::updatePlano,
                    onStatusChange = userDetailViewModel::updateStatus,
                    onSaveClick = userDetailViewModel::save,
                    onToggleStatusClick = userDetailViewModel::toggleStatus,
                    onRequestDelete = userDetailViewModel::requestDelete,
                    onCancelDelete = userDetailViewModel::cancelDelete,
                    onConfirmDelete = userDetailViewModel::confirmDelete,
                    onRefreshClick = { userDetailViewModel.load(userId, forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminPermissionUsers) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Cargos de Acesso",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val permissionUsersViewModel: AdminPermissionUsersViewModel = viewModel()
                val permissionUsersState by permissionUsersViewModel.uiState.collectAsState()
                val tenantLabel = authState.session.tenant?.name?.ifBlank { null } ?: "Tenant atual"
                val canAssignTurmaLeader = user.role == UserRole.Master || user.role == UserRole.MasterTenant

                AdminPermissionUsersScreen(
                    state = permissionUsersState,
                    tenantLabel = tenantLabel,
                    currentUserId = user.id,
                    canAssignTurmaLeader = canAssignTurmaLeader,
                    onSearchChange = permissionUsersViewModel::onSearchChange,
                    onLetterGroupChange = permissionUsersViewModel::onLetterGroupChange,
                    onRoleChange = permissionUsersViewModel::selectRole,
                    onSaveRole = { row ->
                        permissionUsersViewModel.saveRole(
                            user = row,
                            actorUserId = user.id,
                            actorName = user.name,
                        )
                    },
                    onSaveAllRoles = {
                        permissionUsersViewModel.saveAllRoles(
                            actorUserId = user.id,
                            actorName = user.name,
                        )
                    },
                    onToggleTurmaLeader = permissionUsersViewModel::toggleTurmaLeader,
                    onLoadMoreClick = permissionUsersViewModel::loadMore,
                    onRefreshClick = permissionUsersViewModel::refresh,
                    onStatusCompleteClick = {
                        navController.navigate(AppRoute.AdminUsers) {
                            launchSingleTop = true
                        }
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminReports) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Denúncias",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                AdminReportsHubScreen(
                    onSectionClick = { section ->
                        val target = when (section) {
                            AdminReportsSection.Banned -> AppRoute.AdminReportsBanned
                            AdminReportsSection.Community -> AppRoute.AdminReportsCommunity
                            AdminReportsSection.Gym -> AppRoute.AdminReportsGym
                            AdminReportsSection.Support -> AppRoute.AdminReportsSupport
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminReportsBanned) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Banidos",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val reportsViewModel: AdminReportsViewModel = viewModel()
                val reportsState by reportsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    reportsViewModel.load(AdminReportsSection.Banned)
                }
                AdminReportsListScreen(
                    state = reportsState,
                    onResponseChange = reportsViewModel::updateResponse,
                    onResolveClick = reportsViewModel::resolve,
                    onDeleteClick = reportsViewModel::delete,
                    onUserClick = { reporterId ->
                        navController.navigate(AppRoute.adminUserDetail(reporterId)) { launchSingleTop = true }
                    },
                    onPreviousPageClick = reportsViewModel::previousPage,
                    onNextPageClick = reportsViewModel::nextPage,
                    onRefreshClick = { reportsViewModel.load(AdminReportsSection.Banned, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminReports) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminReportsCommunity) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Comunidade",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val reportsViewModel: AdminReportsViewModel = viewModel()
                val reportsState by reportsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    reportsViewModel.load(AdminReportsSection.Community)
                }
                AdminReportsListScreen(
                    state = reportsState,
                    onResponseChange = reportsViewModel::updateResponse,
                    onResolveClick = reportsViewModel::resolve,
                    onDeleteClick = reportsViewModel::delete,
                    onUserClick = { reporterId ->
                        navController.navigate(AppRoute.adminUserDetail(reporterId)) { launchSingleTop = true }
                    },
                    onPreviousPageClick = reportsViewModel::previousPage,
                    onNextPageClick = reportsViewModel::nextPage,
                    onRefreshClick = { reportsViewModel.load(AdminReportsSection.Community, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminReports) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminReportsGym) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Gym",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val reportsViewModel: AdminReportsViewModel = viewModel()
                val reportsState by reportsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    reportsViewModel.load(AdminReportsSection.Gym)
                }
                AdminReportsListScreen(
                    state = reportsState,
                    onResponseChange = reportsViewModel::updateResponse,
                    onResolveClick = reportsViewModel::resolve,
                    onDeleteClick = reportsViewModel::delete,
                    onUserClick = { reporterId ->
                        navController.navigate(AppRoute.adminUserDetail(reporterId)) { launchSingleTop = true }
                    },
                    onPreviousPageClick = reportsViewModel::previousPage,
                    onNextPageClick = reportsViewModel::nextPage,
                    onRefreshClick = { reportsViewModel.load(AdminReportsSection.Gym, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminReports) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminReportsSupport) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Suporte",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val reportsViewModel: AdminReportsViewModel = viewModel()
                val reportsState by reportsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    reportsViewModel.load(AdminReportsSection.Support)
                }
                AdminReportsListScreen(
                    state = reportsState,
                    onResponseChange = reportsViewModel::updateResponse,
                    onResolveClick = reportsViewModel::resolve,
                    onDeleteClick = reportsViewModel::delete,
                    onUserClick = { reporterId ->
                        navController.navigate(AppRoute.adminUserDetail(reporterId)) { launchSingleTop = true }
                    },
                    onPreviousPageClick = reportsViewModel::previousPage,
                    onNextPageClick = reportsViewModel::nextPage,
                    onRefreshClick = { reportsViewModel.load(AdminReportsSection.Support, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminReports) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminStore) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Loja",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val storeViewModel: AdminStoreViewModel = viewModel()
                val storeState by storeViewModel.uiState.collectAsState()

                AdminStoreScreen(
                    state = storeState,
                    onPixKeyChange = storeViewModel::updatePixKey,
                    onBankChange = storeViewModel::updateBank,
                    onHolderChange = storeViewModel::updateHolder,
                    onWhatsappChange = storeViewModel::updateWhatsapp,
                    onSaveFinanceClick = storeViewModel::saveFinance,
                    onMenuItemClick = { item ->
                        val targetRoute = when (item.kind) {
                            AdminStoreMenuKind.Category -> AppRoute.AdminStoreCategories
                            AdminStoreMenuKind.Products -> AppRoute.AdminStoreProducts
                            AdminStoreMenuKind.Disabled -> AppRoute.AdminStoreDisabledProducts
                            AdminStoreMenuKind.PendingOrders -> AppRoute.AdminStorePendingOrders
                            AdminStoreMenuKind.ApprovedOrders -> AppRoute.AdminStoreApprovedOrders
                            AdminStoreMenuKind.Reviews -> AppRoute.AdminStoreReviews
                        }
                        navController.navigate(targetRoute) { launchSingleTop = true }
                    },
                    onRefreshClick = { storeViewModel.load(forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStoreCategories) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Categorias da Loja",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val categoriesViewModel: AdminStoreCategoriesViewModel = viewModel()
                val categoriesState by categoriesViewModel.uiState.collectAsState()
                val tenantLogoUrl = authState.session.tenant?.logoUrl.orEmpty()
                val defaultColor = authState.session.tenant?.palette.toCategoryColor()

                LaunchedEffect(authState.session.tenant?.id, tenantLogoUrl, defaultColor) {
                    categoriesViewModel.load(
                        tenantLogoUrl = tenantLogoUrl,
                        defaultButtonColor = defaultColor,
                        forceRefresh = false,
                    )
                }

                AdminStoreCategoriesScreen(
                    state = categoriesState,
                    onProductsClick = {
                        navController.navigate(AppRoute.AdminStoreProducts) {
                            launchSingleTop = true
                        }
                    },
                    onNewProductClick = {
                        navController.navigate(AppRoute.AdminStoreProducts) {
                            launchSingleTop = true
                        }
                    },
                    onToggleOrderPanelClick = categoriesViewModel::toggleOrderPanel,
                    onMoveCategoryUp = categoriesViewModel::moveCategoryUp,
                    onMoveCategoryDown = categoriesViewModel::moveCategoryDown,
                    onRestoreOrderClick = categoriesViewModel::restoreOrder,
                    onSaveOrderClick = categoriesViewModel::saveOrder,
                    onNameChange = categoriesViewModel::updateName,
                    onCoverUrlChange = categoriesViewModel::updateCoverUrl,
                    onButtonColorChange = categoriesViewModel::updateButtonColor,
                    onStartNewCategoryClick = categoriesViewModel::startNewCategory,
                    onSaveCategoryClick = categoriesViewModel::saveCategory,
                    onEditCategoryClick = categoriesViewModel::editCategory,
                    onToggleVisibilityClick = categoriesViewModel::toggleVisibility,
                    onPendingOrdersClick = { category ->
                        navController.navigate(AppRoute.adminStorePendingOrdersByCategory(category)) {
                            launchSingleTop = true
                        }
                    },
                    onApprovedOrdersClick = { category ->
                        navController.navigate(AppRoute.adminStoreApprovedOrdersByCategory(category)) {
                            launchSingleTop = true
                        }
                    },
                    onRefreshClick = {
                        categoriesViewModel.load(
                            tenantLogoUrl = tenantLogoUrl,
                            defaultButtonColor = defaultColor,
                            forceRefresh = true,
                        )
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStoreProducts) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Produtos",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val productsViewModel: AdminStoreProductsViewModel = viewModel()
                val productsState by productsViewModel.uiState.collectAsState()
                val tenantName = authState.session.tenant?.name.orEmpty()
                val tenantLogoUrl = authState.session.tenant?.logoUrl.orEmpty()

                LaunchedEffect(authState.session.tenant?.id) {
                    productsViewModel.load(inactiveOnly = false, forceRefresh = false)
                }

                AdminStoreProductsScreen(
                    state = productsState,
                    onInactiveProductsClick = {
                        navController.navigate(AppRoute.AdminStoreDisabledProducts) {
                            launchSingleTop = true
                        }
                    },
                    onActiveProductsClick = {
                        navController.navigate(AppRoute.AdminStoreProducts) {
                            launchSingleTop = true
                        }
                    },
                    onCategoriesClick = {
                        navController.navigate(AppRoute.AdminStoreCategories) {
                            launchSingleTop = true
                        }
                    },
                    onPendingOrdersClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) {
                            launchSingleTop = true
                        }
                    },
                    onReviewsClick = {
                        navController.navigate(AppRoute.AdminStoreReviews) {
                            launchSingleTop = true
                        }
                    },
                    onNewProductClick = productsViewModel::openCreateProduct,
                    onCloseProductFormClick = productsViewModel::closeProductForm,
                    onEditProductClick = productsViewModel::editProduct,
                    onOpenProductClick = { productId ->
                        navController.navigate(AppRoute.productDetail(productId)) {
                            launchSingleTop = true
                        }
                    },
                    onToggleProductActiveClick = productsViewModel::toggleProductActive,
                    onCategoryClick = productsViewModel::selectCategory,
                    onNameChange = productsViewModel::updateName,
                    onCategoryChange = productsViewModel::updateCategory,
                    onDescriptionChange = productsViewModel::updateDescription,
                    onImageChange = productsViewModel::updateImage,
                    onPriceChange = productsViewModel::updatePrice,
                    onOldPriceChange = productsViewModel::updateOldPrice,
                    onStatusChange = productsViewModel::updateStatus,
                    onStockChange = productsViewModel::updateStock,
                    onLotChange = productsViewModel::updateLot,
                    onTagLabelChange = productsViewModel::updateTagLabel,
                    onTagColorChange = productsViewModel::updateTagColor,
                    onTagEffectChange = productsViewModel::updateTagEffect,
                    onColorsTextChange = productsViewModel::updateColorsText,
                    onFeaturesTextChange = productsViewModel::updateFeaturesText,
                    onSaveProductClick = {
                        productsViewModel.saveProduct(
                            tenantName = tenantName,
                            tenantLogoUrl = tenantLogoUrl,
                        )
                    },
                    onRefreshClick = {
                        productsViewModel.load(
                            categoryLabel = productsState.selectedCategoryLabel,
                            inactiveOnly = false,
                            forceRefresh = true,
                        )
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStoreDisabledProducts) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Produtos Desativados",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val productsViewModel: AdminStoreProductsViewModel = viewModel()
                val productsState by productsViewModel.uiState.collectAsState()
                val tenantName = authState.session.tenant?.name.orEmpty()
                val tenantLogoUrl = authState.session.tenant?.logoUrl.orEmpty()

                LaunchedEffect(authState.session.tenant?.id) {
                    productsViewModel.load(inactiveOnly = true, forceRefresh = false)
                }

                AdminStoreProductsScreen(
                    state = productsState,
                    onInactiveProductsClick = {
                        navController.navigate(AppRoute.AdminStoreDisabledProducts) {
                            launchSingleTop = true
                        }
                    },
                    onActiveProductsClick = {
                        navController.navigate(AppRoute.AdminStoreProducts) {
                            launchSingleTop = true
                        }
                    },
                    onCategoriesClick = {
                        navController.navigate(AppRoute.AdminStoreCategories) {
                            launchSingleTop = true
                        }
                    },
                    onPendingOrdersClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) {
                            launchSingleTop = true
                        }
                    },
                    onReviewsClick = {
                        navController.navigate(AppRoute.AdminStoreReviews) {
                            launchSingleTop = true
                        }
                    },
                    onNewProductClick = productsViewModel::openCreateProduct,
                    onCloseProductFormClick = productsViewModel::closeProductForm,
                    onEditProductClick = productsViewModel::editProduct,
                    onOpenProductClick = { productId ->
                        navController.navigate(AppRoute.productDetail(productId)) {
                            launchSingleTop = true
                        }
                    },
                    onToggleProductActiveClick = productsViewModel::toggleProductActive,
                    onCategoryClick = productsViewModel::selectCategory,
                    onNameChange = productsViewModel::updateName,
                    onCategoryChange = productsViewModel::updateCategory,
                    onDescriptionChange = productsViewModel::updateDescription,
                    onImageChange = productsViewModel::updateImage,
                    onPriceChange = productsViewModel::updatePrice,
                    onOldPriceChange = productsViewModel::updateOldPrice,
                    onStatusChange = productsViewModel::updateStatus,
                    onStockChange = productsViewModel::updateStock,
                    onLotChange = productsViewModel::updateLot,
                    onTagLabelChange = productsViewModel::updateTagLabel,
                    onTagColorChange = productsViewModel::updateTagColor,
                    onTagEffectChange = productsViewModel::updateTagEffect,
                    onColorsTextChange = productsViewModel::updateColorsText,
                    onFeaturesTextChange = productsViewModel::updateFeaturesText,
                    onSaveProductClick = {
                        productsViewModel.saveProduct(
                            tenantName = tenantName,
                            tenantLogoUrl = tenantLogoUrl,
                        )
                    },
                    onRefreshClick = {
                        productsViewModel.load(
                            inactiveOnly = true,
                            forceRefresh = true,
                        )
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStorePendingOrders) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Pedidos Pendentes",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val ordersViewModel: AdminStoreOrdersViewModel = viewModel()
                val ordersState by ordersViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    ordersViewModel.load(AdminStoreOrdersMode.Pending, categoryLabel = null, page = 1)
                }
                AdminStoreOrdersScreen(
                    state = ordersState,
                    onPendingModeClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) { launchSingleTop = true }
                    },
                    onApprovedModeClick = {
                        navController.navigate(AppRoute.AdminStoreApprovedOrders) { launchSingleTop = true }
                    },
                    onCategoryClick = { category ->
                        navController.navigate(AppRoute.adminStorePendingOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onCategoryModeClick = { mode, category ->
                        val target = if (mode == AdminStoreOrdersMode.Pending) {
                            AppRoute.adminStorePendingOrdersByCategory(category)
                        } else {
                            AppRoute.adminStoreApprovedOrdersByCategory(category)
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    },
                    onAllCategoriesClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) { launchSingleTop = true }
                    },
                    onApproveClick = { order -> ordersViewModel.approve(order, user.id) },
                    onRejectClick = ordersViewModel::reject,
                    onEditApprovalClick = ordersViewModel::toggleEditing,
                    onReturnToPendingClick = ordersViewModel::returnToPending,
                    onMarkDeliveredClick = ordersViewModel::markDelivered,
                    onPreviousPageClick = ordersViewModel::previousPage,
                    onNextPageClick = ordersViewModel::nextPage,
                    onRefreshClick = { ordersViewModel.load(AdminStoreOrdersMode.Pending, null, ordersState.page) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStoreApprovedOrders) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Pedidos Aprovados",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val ordersViewModel: AdminStoreOrdersViewModel = viewModel()
                val ordersState by ordersViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    ordersViewModel.load(AdminStoreOrdersMode.Approved, categoryLabel = null, page = 1)
                }
                AdminStoreOrdersScreen(
                    state = ordersState,
                    onPendingModeClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) { launchSingleTop = true }
                    },
                    onApprovedModeClick = {
                        navController.navigate(AppRoute.AdminStoreApprovedOrders) { launchSingleTop = true }
                    },
                    onCategoryClick = { category ->
                        navController.navigate(AppRoute.adminStoreApprovedOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onCategoryModeClick = { mode, category ->
                        val target = if (mode == AdminStoreOrdersMode.Pending) {
                            AppRoute.adminStorePendingOrdersByCategory(category)
                        } else {
                            AppRoute.adminStoreApprovedOrdersByCategory(category)
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    },
                    onAllCategoriesClick = {
                        navController.navigate(AppRoute.AdminStoreApprovedOrders) { launchSingleTop = true }
                    },
                    onApproveClick = { order -> ordersViewModel.approve(order, user.id) },
                    onRejectClick = ordersViewModel::reject,
                    onEditApprovalClick = ordersViewModel::toggleEditing,
                    onReturnToPendingClick = ordersViewModel::returnToPending,
                    onMarkDeliveredClick = ordersViewModel::markDelivered,
                    onPreviousPageClick = ordersViewModel::previousPage,
                    onNextPageClick = ordersViewModel::nextPage,
                    onRefreshClick = { ordersViewModel.load(AdminStoreOrdersMode.Approved, null, ordersState.page) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(
            route = AppRoute.AdminStorePendingOrdersByCategory,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { backStackEntry ->
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Pedidos Pendentes",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val category = Uri.decode(backStackEntry.arguments?.getString("category").orEmpty())
                val ordersViewModel: AdminStoreOrdersViewModel = viewModel()
                val ordersState by ordersViewModel.uiState.collectAsState()
                LaunchedEffect(category) {
                    ordersViewModel.load(AdminStoreOrdersMode.Pending, categoryLabel = category, page = 1)
                }
                AdminStoreOrdersScreen(
                    state = ordersState,
                    onPendingModeClick = {
                        navController.navigate(AppRoute.adminStorePendingOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onApprovedModeClick = {
                        navController.navigate(AppRoute.adminStoreApprovedOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onCategoryClick = { nextCategory ->
                        navController.navigate(AppRoute.adminStorePendingOrdersByCategory(nextCategory)) { launchSingleTop = true }
                    },
                    onCategoryModeClick = { mode, nextCategory ->
                        val target = if (mode == AdminStoreOrdersMode.Pending) {
                            AppRoute.adminStorePendingOrdersByCategory(nextCategory)
                        } else {
                            AppRoute.adminStoreApprovedOrdersByCategory(nextCategory)
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    },
                    onAllCategoriesClick = {
                        navController.navigate(AppRoute.AdminStorePendingOrders) { launchSingleTop = true }
                    },
                    onApproveClick = { order -> ordersViewModel.approve(order, user.id) },
                    onRejectClick = ordersViewModel::reject,
                    onEditApprovalClick = ordersViewModel::toggleEditing,
                    onReturnToPendingClick = ordersViewModel::returnToPending,
                    onMarkDeliveredClick = ordersViewModel::markDelivered,
                    onPreviousPageClick = ordersViewModel::previousPage,
                    onNextPageClick = ordersViewModel::nextPage,
                    onRefreshClick = { ordersViewModel.load(AdminStoreOrdersMode.Pending, category, ordersState.page) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(
            route = AppRoute.AdminStoreApprovedOrdersByCategory,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { backStackEntry ->
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Pedidos Aprovados",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val category = Uri.decode(backStackEntry.arguments?.getString("category").orEmpty())
                val ordersViewModel: AdminStoreOrdersViewModel = viewModel()
                val ordersState by ordersViewModel.uiState.collectAsState()
                LaunchedEffect(category) {
                    ordersViewModel.load(AdminStoreOrdersMode.Approved, categoryLabel = category, page = 1)
                }
                AdminStoreOrdersScreen(
                    state = ordersState,
                    onPendingModeClick = {
                        navController.navigate(AppRoute.adminStorePendingOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onApprovedModeClick = {
                        navController.navigate(AppRoute.adminStoreApprovedOrdersByCategory(category)) { launchSingleTop = true }
                    },
                    onCategoryClick = { nextCategory ->
                        navController.navigate(AppRoute.adminStoreApprovedOrdersByCategory(nextCategory)) { launchSingleTop = true }
                    },
                    onCategoryModeClick = { mode, nextCategory ->
                        val target = if (mode == AdminStoreOrdersMode.Pending) {
                            AppRoute.adminStorePendingOrdersByCategory(nextCategory)
                        } else {
                            AppRoute.adminStoreApprovedOrdersByCategory(nextCategory)
                        }
                        navController.navigate(target) { launchSingleTop = true }
                    },
                    onAllCategoriesClick = {
                        navController.navigate(AppRoute.AdminStoreApprovedOrders) { launchSingleTop = true }
                    },
                    onApproveClick = { order -> ordersViewModel.approve(order, user.id) },
                    onRejectClick = ordersViewModel::reject,
                    onEditApprovalClick = ordersViewModel::toggleEditing,
                    onReturnToPendingClick = ordersViewModel::returnToPending,
                    onMarkDeliveredClick = ordersViewModel::markDelivered,
                    onPreviousPageClick = ordersViewModel::previousPage,
                    onNextPageClick = ordersViewModel::nextPage,
                    onRefreshClick = { ordersViewModel.load(AdminStoreOrdersMode.Approved, category, ordersState.page) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminStoreReviews) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Reviews",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val reviewsViewModel: AdminStoreReviewsViewModel = viewModel()
                val reviewsState by reviewsViewModel.uiState.collectAsState()

                AdminStoreReviewsScreen(
                    state = reviewsState,
                    onApproveClick = reviewsViewModel::approve,
                    onRejectClick = reviewsViewModel::reject,
                    onProductClick = { productId ->
                        navController.navigate(AppRoute.productDetail(productId)) {
                            launchSingleTop = true
                        }
                    },
                    onPreviousPageClick = reviewsViewModel::previousPage,
                    onNextPageClick = reviewsViewModel::nextPage,
                    onRefreshClick = { reviewsViewModel.load(forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminPartners) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Parceiros",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                AdminPartnersHubScreen(
                    onActiveClick = {
                        navController.navigate(AppRoute.AdminPartnersActive) { launchSingleTop = true }
                    },
                    onCompaniesClick = {
                        navController.navigate(AppRoute.AdminPartnersCompanies) { launchSingleTop = true }
                    },
                    onBiClick = {
                        navController.navigate(AppRoute.AdminPartnersData) { launchSingleTop = true }
                    },
                    onHistoryClick = {
                        navController.navigate(AppRoute.AdminPartnersHistory) { launchSingleTop = true }
                    },
                    onCreatePartnerClick = {
                        navController.navigate(AppRoute.AdminPartnersCompanies) { launchSingleTop = true }
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminPartnersActive) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Parceiros Ativos",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val partnersViewModel: AdminPartnersViewModel = viewModel()
                val partnersState by partnersViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    partnersViewModel.loadActive()
                }
                AdminPartnersActiveScreen(
                    state = partnersState,
                    onPreviousPageClick = partnersViewModel::previousPage,
                    onNextPageClick = partnersViewModel::nextPage,
                    onRefreshClick = { partnersViewModel.loadActive(page = partnersState.page, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminPartners) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPartnersCompanies) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Empresas Parceiras",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val partnersViewModel: AdminPartnersViewModel = viewModel()
                val partnersState by partnersViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    partnersViewModel.loadCompanies()
                }
                AdminPartnersCompaniesScreen(
                    state = partnersState,
                    onSearchChange = partnersViewModel::setSearch,
                    onStatusFilterClick = partnersViewModel::setStatusFilter,
                    onCreateClick = partnersViewModel::openCreateForm,
                    onEditClick = partnersViewModel::openEditForm,
                    onResetPasswordClick = partnersViewModel::requestPasswordReset,
                    onToggleStatusClick = partnersViewModel::toggleStatus,
                    onFormChange = partnersViewModel::updateForm,
                    onCloseFormClick = partnersViewModel::closeForm,
                    onSaveFormClick = partnersViewModel::saveForm,
                    onPreviousPageClick = partnersViewModel::previousPage,
                    onNextPageClick = partnersViewModel::nextPage,
                    onRefreshClick = { partnersViewModel.loadCompanies(page = partnersState.page, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminPartners) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPartnersData) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "BI de parceiros",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val partnersViewModel: AdminPartnersViewModel = viewModel()
                val partnersState by partnersViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    partnersViewModel.loadBi(forceRefresh = true)
                }
                AdminPartnersBiScreen(
                    state = partnersState,
                    onMetricClick = partnersViewModel::toggleBiMetric,
                    onRefreshClick = { partnersViewModel.loadBi(forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminPartners) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPartnersHistory) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Histórico de scans",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val partnersViewModel: AdminPartnersViewModel = viewModel()
                val partnersState by partnersViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    partnersViewModel.loadHistory()
                }
                AdminPartnersHistoryScreen(
                    state = partnersState,
                    onPreviousPageClick = partnersViewModel::previousPage,
                    onNextPageClick = partnersViewModel::nextPage,
                    onRefreshClick = { partnersViewModel.loadHistory(page = partnersState.page, forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminPartners) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlans) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Planos",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                AdminPlansHubScreen(
                    onHistoryClick = { navController.navigate(AppRoute.AdminPlansHistory) { launchSingleTop = true } },
                    onAuditClick = { navController.navigate(AppRoute.AdminPlansAudit) { launchSingleTop = true } },
                    onEditCatalogClick = { navController.navigate(AppRoute.AdminPlansEdit) { launchSingleTop = true } },
                    onBichoSoltoClick = { navController.navigate(AppRoute.AdminPlansBichoSolto) { launchSingleTop = true } },
                    onCardumeLivreClick = { navController.navigate(AppRoute.AdminPlansCardumeLivre) { launchSingleTop = true } },
                    onAtletaClick = { navController.navigate(AppRoute.AdminPlansAtleta) { launchSingleTop = true } },
                    onLendaClick = { navController.navigate(AppRoute.AdminPlansLenda) { launchSingleTop = true } },
                    onPublicPlansClick = { navController.navigate(AppRoute.Plans) { launchSingleTop = true } },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminPlansHistory) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Histórico", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Histórico",
                    source = "web-reference/src/app/admin/planos/historico/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansAudit) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Auditoria", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                val auditViewModel: AdminPlanAuditViewModel = viewModel()
                val auditState by auditViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    auditViewModel.load(forceRefresh = true)
                }
                AdminPlanAuditScreen(
                    state = auditState,
                    onRefreshClick = { auditViewModel.load(forceRefresh = true) },
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansEdit) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Editar catálogo", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Editar catálogo",
                    source = "web-reference/src/app/admin/planos/editar/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansBichoSolto) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Lista Bicho Solto", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                val subscriptionsViewModel: AdminPlanSubscriptionsViewModel = viewModel()
                val subscriptionsState by subscriptionsViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    subscriptionsViewModel.load(AdminPlanSubscriptionListKind.BichoSolto, forceRefresh = true)
                }
                AdminPlanSubscriptionsScreen(
                    state = subscriptionsState,
                    onRefreshClick = { subscriptionsViewModel.load(AdminPlanSubscriptionListKind.BichoSolto, forceRefresh = true) },
                    onPreviousPageClick = subscriptionsViewModel::previousPage,
                    onNextPageClick = subscriptionsViewModel::nextPage,
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansCardumeLivre) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Lista Cardume Livre", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                val subscriptionsViewModel: AdminPlanSubscriptionsViewModel = viewModel()
                val subscriptionsState by subscriptionsViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    subscriptionsViewModel.load(AdminPlanSubscriptionListKind.CardumeLivre, forceRefresh = true)
                }
                AdminPlanSubscriptionsScreen(
                    state = subscriptionsState,
                    onRefreshClick = { subscriptionsViewModel.load(AdminPlanSubscriptionListKind.CardumeLivre, forceRefresh = true) },
                    onPreviousPageClick = subscriptionsViewModel::previousPage,
                    onNextPageClick = subscriptionsViewModel::nextPage,
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansAtleta) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Lista Atleta", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                val subscriptionsViewModel: AdminPlanSubscriptionsViewModel = viewModel()
                val subscriptionsState by subscriptionsViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    subscriptionsViewModel.load(AdminPlanSubscriptionListKind.Atleta, forceRefresh = true)
                }
                AdminPlanSubscriptionsScreen(
                    state = subscriptionsState,
                    onRefreshClick = { subscriptionsViewModel.load(AdminPlanSubscriptionListKind.Atleta, forceRefresh = true) },
                    onPreviousPageClick = subscriptionsViewModel::previousPage,
                    onNextPageClick = subscriptionsViewModel::nextPage,
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminPlansLenda) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Lista Lenda", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                val subscriptionsViewModel: AdminPlanSubscriptionsViewModel = viewModel()
                val subscriptionsState by subscriptionsViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    subscriptionsViewModel.load(AdminPlanSubscriptionListKind.Lenda, forceRefresh = true)
                }
                AdminPlanSubscriptionsScreen(
                    state = subscriptionsState,
                    onRefreshClick = { subscriptionsViewModel.load(AdminPlanSubscriptionListKind.Lenda, forceRefresh = true) },
                    onPreviousPageClick = subscriptionsViewModel::previousPage,
                    onNextPageClick = subscriptionsViewModel::nextPage,
                    onBackClick = { navController.navigate(AppRoute.AdminPlans) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminManagement) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Gestão",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                AdminManagementHubScreen(
                    onEventsClick = { navController.navigate(AppRoute.AdminManagementEvents) { launchSingleTop = true } },
                    onStoreClick = { navController.navigate(AppRoute.AdminManagementStore) { launchSingleTop = true } },
                    onTrainingClick = { navController.navigate(AppRoute.AdminManagementTraining) { launchSingleTop = true } },
                    onFinanceClick = { navController.navigate(AppRoute.AdminManagementFinance) { launchSingleTop = true } },
                    onCommercialBiClick = { navController.navigate(AppRoute.AdminBiCommercial) { launchSingleTop = true } },
                    onOperationalBiClick = { navController.navigate(AppRoute.AdminBiOperational) { launchSingleTop = true } },
                    onGateBiClick = { navController.navigate(AppRoute.AdminBiGate) { launchSingleTop = true } },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminManagementEvents) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Eventos", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Eventos",
                    source = "web-reference/src/app/admin/gestao/eventos/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminManagementStore) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "BI Loja", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "BI Loja",
                    source = "web-reference/src/app/admin/gestao/loja/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminManagementTraining) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Treinos", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Treinos",
                    source = "web-reference/src/app/admin/gestao/treinos/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminManagementFinance) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Financeiro", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Financeiro",
                    source = "web-reference/src/app/admin/gestao/financeiro/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminBiCommercial) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "BI Comercial", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "BI Comercial",
                    source = "web-reference/src/app/admin/bi/comercial/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminBiOperational) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "BI Operacional", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "BI Operacional",
                    source = "web-reference/src/app/admin/bi/operacional/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminBiGate) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "BI Portaria", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "BI Portaria",
                    source = "web-reference/src/app/admin/bi/portaria/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminManagement) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminAlbum) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Álbum",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val albumViewModel: AdminAlbumViewModel = viewModel()
                val albumState by albumViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    albumViewModel.load()
                }
                AdminAlbumScreen(
                    state = albumState,
                    onTitleChange = { value -> albumViewModel.updateConfig { it.copy(title = value) } },
                    onSubtitleChange = { value -> albumViewModel.updateConfig { it.copy(subtitle = value) } },
                    onCoverChange = { value -> albumViewModel.updateConfig { it.copy(cover = value) } },
                    onSaveClick = albumViewModel::save,
                    onAddClassClick = { navController.navigate(AppRoute.AdminClass) { launchSingleTop = true } },
                    onCacaCalouroClick = { navController.navigate(AppRoute.AdminAlbumCacaCalouro) { launchSingleTop = true } },
                    onPontuaCalouroClick = { navController.navigate(AppRoute.AdminAlbumPontuaCalouro) { launchSingleTop = true } },
                    onPontuaGeralClick = { navController.navigate(AppRoute.AdminAlbumPontuaGeral) { launchSingleTop = true } },
                    onCustomizationClick = { navController.navigate(AppRoute.AdminAlbumCustomization) { launchSingleTop = true } },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminAlbumCacaCalouro) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Caça Calouro", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Caça Calouro",
                    source = "web-reference/src/app/admin/album/caca_calouro/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminAlbum) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminAlbumPontuaCalouro) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Pontuação Calouro", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Pontuação Calouro",
                    source = "web-reference/src/app/admin/album/pontua_calouro/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminAlbum) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminAlbumPontuaGeral) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Pontuação Geral", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Pontuação Geral",
                    source = "web-reference/src/app/admin/album/pontua_geral/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminAlbum) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminAlbumCustomization) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Customização", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Customização",
                    source = "web-reference/src/app/admin/album/customizacao/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminAlbum) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminClass) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(title = "Turma", subtitle = "Use uma conta com permissão administrativa neste tenant.")
            } else {
                AdminPendingRouteScreen(
                    title = "Turma",
                    source = "web-reference/src/app/admin/turma/page.tsx",
                    onBackClick = { navController.navigate(AppRoute.AdminAlbum) { launchSingleTop = true } },
                )
            }
        }

        composable(AppRoute.AdminGames) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Arena",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val gamesViewModel: AdminGamesViewModel = viewModel()
                val gamesState by gamesViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    gamesViewModel.load()
                }
                AdminGamesScreen(
                    state = gamesState,
                    onSearchChange = gamesViewModel::setSearchTerm,
                    onUserClick = gamesViewModel::selectUser,
                    onCloseUserClick = { gamesViewModel.selectUser(null) },
                    onRefreshClick = { gamesViewModel.load(forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminDatabaseScanner) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Scanner de Campos do Banco",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val scannerViewModel: AdminDatabaseScannerViewModel = viewModel()
                val scannerState by scannerViewModel.uiState.collectAsState()
                AdminDatabaseScannerScreen(
                    state = scannerState,
                    onScanClick = { scannerViewModel.scanDatabase(forceRefresh = true) },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminMentorship) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Admin Apadrinhamento",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val mentorshipViewModel: AdminMentorshipViewModel = viewModel()
                val mentorshipState by mentorshipViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    mentorshipViewModel.load(forceRefresh = true)
                }
                AdminMentorshipScreen(
                    state = mentorshipState,
                    onLabelsChange = mentorshipViewModel::updateLabels,
                    onSaveClick = mentorshipViewModel::save,
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminLogs) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Centro de Auditoria",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val logsViewModel: AdminActivityLogsViewModel = viewModel()
                val logsState by logsViewModel.uiState.collectAsState()
                LaunchedEffect(authState.session.tenant?.id) {
                    logsViewModel.loadInitial()
                }
                AdminActivityLogsScreen(
                    state = logsState,
                    onSearchChange = logsViewModel::setSearchTerm,
                    onLoadMoreClick = logsViewModel::loadMore,
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminMiniVendors) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Mini Vendor Admin",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                AdminMiniVendorsHubScreen(
                    onApprovalsClick = {
                        navController.navigate(AppRoute.AdminMiniVendorApprovals) {
                            launchSingleTop = true
                        }
                    },
                    onVendorsClick = {
                        navController.navigate(AppRoute.AdminMiniVendorVendors) {
                            launchSingleTop = true
                        }
                    },
                    onExitAdminClick = {
                        navController.navigate(AppRoute.Dashboard) {
                            launchSingleTop = true
                        }
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminMiniVendorApprovals) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Pendentes de Aprovacao",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val miniVendorsViewModel: AdminMiniVendorsViewModel = viewModel()
                val miniVendorsState by miniVendorsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    miniVendorsViewModel.load(AdminMiniVendorDirectoryMode.Approvals, forceRefresh = true)
                }
                AdminMiniVendorsScreen(
                    state = miniVendorsState,
                    onApproveClick = { row -> miniVendorsViewModel.approve(row, user.id) },
                    onRejectClick = { row -> miniVendorsViewModel.reject(row, user.id) },
                    onDisableClick = { row -> miniVendorsViewModel.disable(row, user.id) },
                    onToggleCategoryVisibilityClick = miniVendorsViewModel::toggleCategoryVisibility,
                    onRefreshClick = {
                        miniVendorsViewModel.load(AdminMiniVendorDirectoryMode.Approvals, forceRefresh = true)
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
        }

        composable(AppRoute.AdminMiniVendorVendors) {
            val user = authState.session.user
            if (user?.role?.canManageTenant != true) {
                PermissionDeniedScreen(
                    title = "Todos os Mini Vendors",
                    subtitle = "Use uma conta com permissão administrativa neste tenant.",
                )
            } else {
                val miniVendorsViewModel: AdminMiniVendorsViewModel = viewModel()
                val miniVendorsState by miniVendorsViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) {
                    miniVendorsViewModel.load(AdminMiniVendorDirectoryMode.Vendors, forceRefresh = true)
                }
                AdminMiniVendorsScreen(
                    state = miniVendorsState,
                    onApproveClick = { row -> miniVendorsViewModel.approve(row, user.id) },
                    onRejectClick = { row -> miniVendorsViewModel.reject(row, user.id) },
                    onDisableClick = { row -> miniVendorsViewModel.disable(row, user.id) },
                    onToggleCategoryVisibilityClick = miniVendorsViewModel::toggleCategoryVisibility,
                    onRefreshClick = {
                        miniVendorsViewModel.load(AdminMiniVendorDirectoryMode.Vendors, forceRefresh = true)
                    },
                    onBackClick = { navController.navigateUp() },
                )
            }
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
        ) {
            EventCheckoutUnavailableScreen(
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.EventTickets) {
            EventFlowUnavailableScreen(
                title = "Meus ingressos",
                subtitle = "Ingressos não carregados",
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.EventTicketDetail,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
        ) {
            EventFlowUnavailableScreen(
                title = "Ingresso",
                subtitle = "Detalhe de ingresso não carregado",
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.EventOrders) {
            EventFlowUnavailableScreen(
                title = "Pedidos de evento",
                subtitle = "Pedidos não carregados",
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.EventOrderDetail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            EventFlowUnavailableScreen(
                title = "Pedido de evento",
                subtitle = "Detalhe de pedido não carregado",
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Store) {
            val storeViewModel: StoreViewModel = viewModel()
            val storeState by storeViewModel.uiState.collectAsState()

            StoreScreen(
                state = storeState,
                onProductClick = { product ->
                    navController.navigate(AppRoute.productDetail(product.id)) {
                        launchSingleTop = true
                    }
                },
                onCategoryClick = storeViewModel::selectCategory,
                onCartClick = {
                    navController.navigate(AppRoute.Cart) {
                        launchSingleTop = true
                    }
                },
                onOrdersClick = {
                    navController.navigate(AppRoute.StoreOrders) {
                        launchSingleTop = true
                    }
                },
                onRetryClick = storeViewModel::refresh,
            )
        }

        composable(
            route = AppRoute.ProductDetail,
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val productDetailViewModel: ProductDetailViewModel = viewModel()
            val productDetailState by productDetailViewModel.uiState.collectAsState()

            LaunchedEffect(productId) {
                productDetailViewModel.loadProduct(productId)
            }

            ProductDetailStateScreen(
                state = productDetailState,
                onAddToCartClick = {
                    navController.navigate(AppRoute.Cart) {
                        launchSingleTop = true
                    }
                },
                onCartClick = {
                    navController.navigate(AppRoute.Cart) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
                onRetryClick = { productDetailViewModel.loadProduct(productId) },
            )
        }

        composable(AppRoute.Cart) {
            val cartViewModel: CartViewModel = viewModel()
            val cartState by cartViewModel.uiState.collectAsState()

            CartScreen(
                state = cartState,
                onCheckoutClick = {
                    navController.navigate(AppRoute.Checkout) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Checkout) {
            val cartViewModel: CartViewModel = viewModel()
            val cartState by cartViewModel.uiState.collectAsState()

            CheckoutScreen(
                state = cartState,
                onConfirmClick = {
                    navController.navigate(AppRoute.StoreOrders) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.StoreOrders) {
            val storeOrdersViewModel: StoreOrdersViewModel = viewModel()
            val storeOrdersState by storeOrdersViewModel.uiState.collectAsState()

            StoreOrdersScreen(
                state = storeOrdersState,
                onStatusClick = storeOrdersViewModel::selectStatus,
                onOrderClick = { order ->
                    navController.navigate(AppRoute.storeOrderDetail(order.id)) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.StoreOrderDetail,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            StoreOrderDetailUnavailableScreen(
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Plans) {
            val plansViewModel: PlansViewModel = viewModel()
            val planState by plansViewModel.uiState.collectAsState()

            PlansScreen(
                state = planState,
                onPlanClick = { plan ->
                    navController.navigate(AppRoute.planDetail(plan.id)) {
                        launchSingleTop = true
                    }
                },
                onStatusClick = {
                    navController.navigate(AppRoute.UserPlanStatus) {
                        launchSingleTop = true
                    }
                },
                onOrdersClick = {
                    navController.navigate(AppRoute.PlanOrders) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = AppRoute.PlanDetail,
            arguments = listOf(navArgument("planId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId").orEmpty()
            val plan = remember(planId) { PlansMockData.planById(planId) }

            PlanDetailScreen(
                plan = plan,
                onSubscribeClick = {
                    navController.navigate(AppRoute.PlanOrders) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.UserPlanStatus) {
            val plansViewModel: PlansViewModel = viewModel()
            val planState by plansViewModel.uiState.collectAsState()

            UserPlanStatusScreen(
                state = planState,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.PlanOrders) {
            val plansViewModel: PlansViewModel = viewModel()
            val planState by plansViewModel.uiState.collectAsState()

            PlanOrdersScreen(
                state = planState,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Training) {
            val trainingViewModel: TrainingViewModel = viewModel()
            val trainingState by trainingViewModel.uiState.collectAsState()

            TrainingScreen(
                state = trainingState,
                onSessionClick = {
                    navController.navigate(AppRoute.TrainingCheckIn) {
                        launchSingleTop = true
                    }
                },
                onCheckInClick = {
                    navController.navigate(AppRoute.TrainingCheckIn) {
                        launchSingleTop = true
                    }
                },
                onFrequencyClick = {
                    navController.navigate(AppRoute.TrainingFrequency) {
                        launchSingleTop = true
                    }
                },
                onHistoryClick = {
                    navController.navigate(AppRoute.TrainingHistory) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoute.Gym) {
            val trainingViewModel: TrainingViewModel = viewModel()
            val trainingState by trainingViewModel.uiState.collectAsState()

            TrainingScreen(
                state = trainingState,
                onSessionClick = {
                    navController.navigate(AppRoute.TrainingCheckIn) {
                        launchSingleTop = true
                    }
                },
                onCheckInClick = {
                    navController.navigate(AppRoute.TrainingCheckIn) {
                        launchSingleTop = true
                    }
                },
                onFrequencyClick = {
                    navController.navigate(AppRoute.TrainingFrequency) {
                        launchSingleTop = true
                    }
                },
                onHistoryClick = {
                    navController.navigate(AppRoute.TrainingHistory) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoute.TrainingCheckIn) {
            val trainingViewModel: TrainingViewModel = viewModel()
            val trainingState by trainingViewModel.uiState.collectAsState()

            TrainingCheckInScreen(
                state = trainingState,
                onConfirmClick = {
                    navController.navigate(AppRoute.trainingCheckInDetail(trainingState.checkIn.id)) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.TrainingCheckInDetail,
            arguments = listOf(navArgument("checkInId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val checkInId = backStackEntry.arguments?.getString("checkInId").orEmpty()
            val checkIn = remember(checkInId) {
                TrainingMockData.history.firstOrNull { it.id == checkInId } ?: TrainingMockData.checkIn
            }

            TrainingCheckInDetailScreen(
                checkIn = checkIn,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.TrainingFrequency) {
            val trainingViewModel: TrainingViewModel = viewModel()
            val trainingState by trainingViewModel.uiState.collectAsState()

            TrainingFrequencyScreen(
                state = trainingState,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.TrainingHistory) {
            val trainingViewModel: TrainingViewModel = viewModel()
            val trainingState by trainingViewModel.uiState.collectAsState()

            TrainingHistoryScreen(
                state = trainingState,
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(AppRoute.Partners) {
            val partnersViewModel: PartnersViewModel = viewModel()
            val partnerState by partnersViewModel.uiState.collectAsState()

            LaunchedEffect(authState.session.tenant?.id) {
                partnersViewModel.load()
            }

            PartnersScreen(
                state = partnerState,
                onPartnerClick = { partner ->
                    navController.navigate(AppRoute.partnerDetail(partner.id)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = AppRoute.PartnerDetail,
            arguments = listOf(navArgument("partnerId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val partnerId = backStackEntry.arguments?.getString("partnerId").orEmpty()
            val partnersViewModel: PartnersViewModel = viewModel()
            val partnerDetailState by partnersViewModel.detailState.collectAsState()

            LaunchedEffect(partnerId, authState.session.tenant?.id) {
                partnersViewModel.loadPartner(partnerId)
            }

            PartnerDetailScreen(
                state = partnerDetailState,
                onBenefitsClick = { partner ->
                    navController.navigate(AppRoute.partnerBenefits(partner.id)) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.navigateUp() },
            )
        }

        composable(
            route = AppRoute.PartnerBenefits,
            arguments = listOf(navArgument("partnerId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val partnerId = backStackEntry.arguments?.getString("partnerId").orEmpty()
            val partnersViewModel: PartnersViewModel = viewModel()
            val partnerDetailState by partnersViewModel.detailState.collectAsState()

            LaunchedEffect(partnerId, authState.session.tenant?.id) {
                partnersViewModel.loadPartner(partnerId)
            }

            PartnerBenefitsScreen(
                state = partnerDetailState,
                onBackClick = { navController.navigateUp() },
            )
        }

        remainingNativeRoutes(
            navController = navController,
            authState = authState,
            onGuestTenantSelected = authViewModel::selectGuestTenant,
        )

        modules.filterNot { module -> module.route in concreteModuleRoutes }.forEach { module ->
            composable(module.route) {
                ModulePlaceholderScreen(
                    module = module,
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }

    LaunchedEffect(guardedRoute, currentRoute) {
        val targetRoute = guardedRoute ?: return@LaunchedEffect
        if (currentRoute == targetRoute) return@LaunchedEffect

        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = targetRoute != AppRoute.Login
            }
            launchSingleTop = true
        }
    }
}

private fun TenantPalette?.toCategoryColor(): String {
    return when (this) {
        TenantPalette.Yellow -> "#eab308"
        TenantPalette.Red -> "#ef4444"
        TenantPalette.Blue -> "#3b82f6"
        TenantPalette.Orange -> "#f97316"
        TenantPalette.Purple -> "#a855f7"
        TenantPalette.Pink -> "#ec4899"
        TenantPalette.Green, null -> "#10b981"
    }
}
