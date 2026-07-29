package com.example.usc1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.usc1.core.permissions.Permission
import com.example.usc1.core.permissions.PermissionBlockReason
import com.example.usc1.core.permissions.PermissionPolicy
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.ui.PermissionDeniedScreen
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.ui.album.AlbumScreen
import com.example.usc1.ui.album.AlbumTurmaScreen
import com.example.usc1.ui.album.AlbumUnavailableScreen
import com.example.usc1.ui.album.AlbumViewModel
import com.example.usc1.ui.album.CacaCalouroScreen
import com.example.usc1.ui.album.CalouroRankingScreen
import com.example.usc1.ui.auth.AuthUiState
import com.example.usc1.ui.collectives.CollectiveMockData
import com.example.usc1.ui.collectives.CollectiveUnavailableScreen
import com.example.usc1.ui.collectives.CommissionAgendaScreen
import com.example.usc1.ui.collectives.CommissionDetailScreen
import com.example.usc1.ui.collectives.CommissionEventsScreen
import com.example.usc1.ui.collectives.CommissionMembersScreen
import com.example.usc1.ui.collectives.CommissionStoreScreen
import com.example.usc1.ui.collectives.CommissionsScreen
import com.example.usc1.ui.collectives.CommissionsViewModel
import com.example.usc1.ui.collectives.DirectoryAgendaScreen
import com.example.usc1.ui.collectives.DirectoryDetailScreen
import com.example.usc1.ui.collectives.DirectoryEventsScreen
import com.example.usc1.ui.collectives.DirectoryInfoScreen
import com.example.usc1.ui.collectives.DirectoryMembersScreen
import com.example.usc1.ui.collectives.DirectoryScreen
import com.example.usc1.ui.collectives.DirectoryStoreScreen
import com.example.usc1.ui.collectives.DirectoryViewModel
import com.example.usc1.ui.collectives.LeagueAgendaScreen
import com.example.usc1.ui.collectives.LeagueDetailScreen
import com.example.usc1.ui.collectives.LeagueEventsScreen
import com.example.usc1.ui.collectives.LeagueInfoScreen
import com.example.usc1.ui.collectives.LeagueMembersScreen
import com.example.usc1.ui.collectives.LeagueStoreScreen
import com.example.usc1.ui.collectives.LeaguesScreen
import com.example.usc1.ui.collectives.LeaguesViewModel
import com.example.usc1.ui.community.CommunityPostDetailScreen
import com.example.usc1.ui.community.CommunityPostUnavailableScreen
import com.example.usc1.ui.community.CommunityScreen
import com.example.usc1.ui.community.CommunityViewModel
import com.example.usc1.ui.games.AchievementsScreen
import com.example.usc1.ui.games.BoardroundRankingScreen
import com.example.usc1.ui.games.BoardroundScreen
import com.example.usc1.ui.games.BoardroundStatsScreen
import com.example.usc1.ui.games.GameRulesScreen
import com.example.usc1.ui.games.GamesScreen
import com.example.usc1.ui.games.GamesViewModel
import com.example.usc1.ui.games.LoyaltyScreen
import com.example.usc1.ui.generalorders.GeneralOrderDetailScreen
import com.example.usc1.ui.generalorders.GeneralOrderType
import com.example.usc1.ui.generalorders.GeneralOrderUnavailableScreen
import com.example.usc1.ui.generalorders.GeneralOrdersViewModel
import com.example.usc1.ui.generalorders.OrdersByTypeScreen
import com.example.usc1.ui.generalorders.OrdersHubScreen
import com.example.usc1.ui.guide.ContactUscScreen
import com.example.usc1.ui.guide.FaqScreen
import com.example.usc1.ui.guide.GuideScreen
import com.example.usc1.ui.guide.GuideViewModel
import com.example.usc1.ui.guide.LegalDocumentScreen
import com.example.usc1.ui.guide.LgpdRequestScreen
import com.example.usc1.ui.guide.PrivacyLgpdScreen
import com.example.usc1.ui.guide.SupportScreen
import com.example.usc1.ui.guide.TermsScreen
import com.example.usc1.ui.scanner.EventCheckInScannerScreen
import com.example.usc1.ui.scanner.PartyScannerScreen
import com.example.usc1.ui.scanner.ProductWithdrawalScannerScreen
import com.example.usc1.ui.scanner.ScannerPermissionDeniedScreen
import com.example.usc1.ui.scanner.ScannerResultErrorScreen
import com.example.usc1.ui.scanner.ScannerResultSuccessScreen
import com.example.usc1.ui.scanner.ScannerScreen
import com.example.usc1.ui.scanner.ScannerViewModel
import com.example.usc1.ui.settings.SettingsInvitesScreen
import com.example.usc1.ui.settings.SettingsMentorshipScreen
import com.example.usc1.ui.settings.SettingsViewModel
import com.example.usc1.ui.settings.withSession
import com.example.usc1.ui.tenant.TenantSwitcherScreen
import com.example.usc1.ui.tenant.TenantViewModel
import com.example.usc1.ui.vendor.MiniVendorApprovedOrdersScreen
import com.example.usc1.ui.vendor.MiniVendorFinanceScreen
import com.example.usc1.ui.vendor.MiniVendorPendingOrdersScreen
import com.example.usc1.ui.vendor.MiniVendorProductsScreen
import com.example.usc1.ui.vendor.MiniVendorScreen
import com.example.usc1.ui.vendor.MiniVendorViewModel
import com.example.usc1.ui.vendor.SalesModeEventMenuScreen
import com.example.usc1.ui.vendor.SalesModeScreen

fun NavGraphBuilder.remainingNativeRoutes(
    navController: NavHostController,
    authState: AuthUiState,
    onGuestTenantSelected: (TenantContext) -> Unit,
) {
    composable(AppRoute.Community) {
        val viewModel: CommunityViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session) {
            viewModel.load(authState.session)
        }
        CommunityScreen(
            state = state,
            onTabClick = viewModel::selectTab,
            onFilterClick = viewModel::selectFilter,
            onPostClick = { post -> navController.navigate(AppRoute.communityPostDetail(post.id)) },
        )
    }

    composable(
        route = AppRoute.CommunityPostDetail,
        arguments = listOf(navArgument("postId") { type = NavType.StringType }),
    ) { entry ->
        val postId = entry.arguments?.getString("postId").orEmpty()
        val viewModel: CommunityViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session) {
            viewModel.load(authState.session)
        }
        val post = state.allPosts.firstOrNull { it.id == postId } ?: viewModel.findPost(postId)
        if (post == null && state.isLoading) {
            PremiumLoadingState(text = "Carregando publicação")
        } else if (post == null) {
            CommunityPostUnavailableScreen(onBackClick = { navController.navigateUp() })
        } else {
            CommunityPostDetailScreen(post = post, onBackClick = { navController.navigateUp() })
        }
    }

    composable(AppRoute.Leagues) {
        val viewModel: LeaguesViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        LeaguesScreen(state = state, onLeagueClick = { navController.navigate(AppRoute.leagueDetail(it.id)) })
    }
    collectiveLeagueRoutes(navController, authState)

    composable(AppRoute.Directory) {
        val viewModel: DirectoryViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        DirectoryScreen(state = state, onDirectoryClick = { navController.navigate(AppRoute.directoryDetail(it.id)) })
    }
    directoryRoutes(navController, authState)

    composable(AppRoute.Commissions) {
        val viewModel: CommissionsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        CommissionsScreen(state = state, onCommissionClick = { navController.navigate(AppRoute.commissionDetail(it.id)) })
    }
    commissionRoutes(navController, authState)

    composable(AppRoute.Tenant) {
        val viewModel: TenantViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        TenantSwitcherScreen(
            state = state,
            onTenantClick = { tenant ->
                viewModel.selectTenant(
                    tenant = tenant,
                    onResolved = onGuestTenantSelected,
                )
            },
            onRetryClick = viewModel::loadDirectory,
            selectionEnabled = authState.session.user?.role == UserRole.Guest,
        )
    }

    miniVendorRoutes(navController, authState)
    settingsSubpageRoutes(navController, authState)
    scannerRoutes(navController, authState)
    guideRoutes(navController, authState)
    albumRoutes(navController, authState)
    gamesRoutes(navController, authState)
    generalOrderRoutes(navController, authState)
}

private fun NavGraphBuilder.settingsSubpageRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.SettingsInvites) {
        val viewModel: SettingsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val clipboardManager = LocalClipboardManager.current

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.loadInviteDashboard(authState.session)
        }

        SettingsInvitesScreen(
            state = state.withSession(authState.session),
            session = authState.session,
            onBackClick = { navController.navigateUp() },
            onRefreshClick = { viewModel.loadInviteDashboard(authState.session, forceRefresh = true) },
            onCopyInviteClick = { link ->
                clipboardManager.setText(AnnotatedString(link))
            },
        )
    }

    composable(AppRoute.SettingsMentorship) {
        val viewModel: SettingsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.loadMentorshipHub(authState.session)
        }

        SettingsMentorshipScreen(
            state = state.withSession(authState.session),
            session = authState.session,
            onBackClick = { navController.navigateUp() },
            onRefreshClick = { viewModel.loadMentorshipHub(authState.session, forceRefresh = true) },
        )
    }
}

private fun NavGraphBuilder.collectiveLeagueRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.LeagueDetail, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("leagueId").orEmpty()
        val viewModel: LeaguesViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        val group = state.leagues.firstOrNull { it.id == id } ?: viewModel.find(id)
        when {
            state.isLoading && group == null -> PremiumLoadingState(text = "Carregando liga")
            group != null -> LeagueDetailScreen(
                league = group,
                onMembersClick = { navController.navigate(AppRoute.leagueMembers(group.id)) },
                onAgendaClick = { navController.navigate(AppRoute.leagueAgenda(group.id)) },
                onStoreClick = { navController.navigate(AppRoute.leagueStore(group.id)) },
                onEventsClick = { navController.navigate(AppRoute.leagueEvents(group.id)) },
                onInfoClick = { navController.navigate(AppRoute.leagueInfo(group.id)) },
                onBackClick = { navController.navigateUp() },
            )
            else -> CollectiveUnavailableScreen(
                title = "Liga não encontrada",
                subtitle = state.errorMessage ?: "A liga pode ter sido removida ou ainda não está publicada nesta atlética.",
                onBackClick = { navController.navigateUp() },
            )
        }
    }
    composable(AppRoute.LeagueMembers, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        leagueRouteState(authState, entry.arguments?.getString("leagueId").orEmpty(), navController, LeagueSubPage.Members)
    }
    composable(AppRoute.LeagueAgenda, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        leagueRouteState(authState, entry.arguments?.getString("leagueId").orEmpty(), navController, LeagueSubPage.Agenda)
    }
    composable(AppRoute.LeagueStore, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        leagueRouteState(authState, entry.arguments?.getString("leagueId").orEmpty(), navController, LeagueSubPage.Store)
    }
    composable(AppRoute.LeagueEvents, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        leagueRouteState(authState, entry.arguments?.getString("leagueId").orEmpty(), navController, LeagueSubPage.Events)
    }
    composable(AppRoute.LeagueInfo, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        leagueRouteState(authState, entry.arguments?.getString("leagueId").orEmpty(), navController, LeagueSubPage.Info)
    }
}

private fun NavGraphBuilder.directoryRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.DirectoryDetail, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("directoryId").orEmpty()
        val viewModel: DirectoryViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        val group = state.directories.firstOrNull { it.id == id } ?: viewModel.find(id)
        when {
            state.isLoading && group == null -> PremiumLoadingState(text = "Carregando diretório")
            group != null -> DirectoryDetailScreen(
                directory = group,
                onMembersClick = { navController.navigate(AppRoute.directoryMembers(group.id)) },
                onAgendaClick = { navController.navigate(AppRoute.directoryAgenda(group.id)) },
                onStoreClick = { navController.navigate(AppRoute.directoryStore(group.id)) },
                onEventsClick = { navController.navigate(AppRoute.directoryEvents(group.id)) },
                onInfoClick = { navController.navigate(AppRoute.directoryInfo(group.id)) },
                onBackClick = { navController.navigateUp() },
            )
            else -> CollectiveUnavailableScreen(
                title = "Diretório não encontrado",
                subtitle = state.errorMessage ?: "O diretório pode ter sido removido ou ainda não está publicado nesta atlética.",
                onBackClick = { navController.navigateUp() },
            )
        }
    }
    composable(AppRoute.DirectoryMembers, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        directoryRouteState(authState, entry.arguments?.getString("directoryId").orEmpty(), navController, DirectorySubPage.Members)
    }
    composable(AppRoute.DirectoryAgenda, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        directoryRouteState(authState, entry.arguments?.getString("directoryId").orEmpty(), navController, DirectorySubPage.Agenda)
    }
    composable(AppRoute.DirectoryStore, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        directoryRouteState(authState, entry.arguments?.getString("directoryId").orEmpty(), navController, DirectorySubPage.Store)
    }
    composable(AppRoute.DirectoryEvents, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        directoryRouteState(authState, entry.arguments?.getString("directoryId").orEmpty(), navController, DirectorySubPage.Events)
    }
    composable(AppRoute.DirectoryInfo, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        directoryRouteState(authState, entry.arguments?.getString("directoryId").orEmpty(), navController, DirectorySubPage.Info)
    }
}

private fun NavGraphBuilder.commissionRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.CommissionDetail, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("commissionId").orEmpty()
        val viewModel: CommissionsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        val group = state.commissions.firstOrNull { it.id == id } ?: viewModel.find(id)
        when {
            state.isLoading && group == null -> PremiumLoadingState(text = "Carregando comissão")
            group != null -> CommissionDetailScreen(
                commission = group,
                onMembersClick = { navController.navigate(AppRoute.commissionMembers(group.id)) },
                onAgendaClick = { navController.navigate(AppRoute.commissionAgenda(group.id)) },
                onStoreClick = { navController.navigate(AppRoute.commissionStore(group.id)) },
                onEventsClick = { navController.navigate(AppRoute.commissionEvents(group.id)) },
                onBackClick = { navController.navigateUp() },
            )
            else -> CollectiveUnavailableScreen(
                title = "Comissão não encontrada",
                subtitle = state.errorMessage ?: "A comissão pode ter sido removida ou ainda não está publicada nesta atlética.",
                onBackClick = { navController.navigateUp() },
            )
        }
    }
    composable(AppRoute.CommissionMembers, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        commissionRouteState(authState, entry.arguments?.getString("commissionId").orEmpty(), navController, CommissionSubPage.Members)
    }
    composable(AppRoute.CommissionAgenda, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        commissionRouteState(authState, entry.arguments?.getString("commissionId").orEmpty(), navController, CommissionSubPage.Agenda)
    }
    composable(AppRoute.CommissionStore, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        commissionRouteState(authState, entry.arguments?.getString("commissionId").orEmpty(), navController, CommissionSubPage.Store)
    }
    composable(AppRoute.CommissionEvents, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        commissionRouteState(authState, entry.arguments?.getString("commissionId").orEmpty(), navController, CommissionSubPage.Events)
    }
}

@Composable
private fun leagueRouteState(
    authState: AuthUiState,
    id: String,
    navController: NavHostController,
    page: LeagueSubPage,
) {
    val viewModel: LeaguesViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(authState.session.tenant?.id) {
        viewModel.load(authState.session)
    }

    val group = state.leagues.firstOrNull { it.id == id } ?: viewModel.find(id)
    when {
        state.isLoading && group == null -> PremiumLoadingState(text = "Carregando liga")
        group != null -> when (page) {
            LeagueSubPage.Members -> LeagueMembersScreen(group, onBackClick = { navController.navigateUp() })
            LeagueSubPage.Agenda -> LeagueAgendaScreen(group, onBackClick = { navController.navigateUp() })
            LeagueSubPage.Store -> LeagueStoreScreen(group, onBackClick = { navController.navigateUp() })
            LeagueSubPage.Events -> LeagueEventsScreen(group, onBackClick = { navController.navigateUp() })
            LeagueSubPage.Info -> LeagueInfoScreen(group, onBackClick = { navController.navigateUp() })
        }
        else -> CollectiveUnavailableScreen(
            title = "Liga não encontrada",
            subtitle = state.errorMessage ?: "A liga pode ter sido removida ou ainda não está publicada nesta atlética.",
            onBackClick = { navController.navigateUp() },
        )
    }
}

@Composable
private fun directoryRouteState(
    authState: AuthUiState,
    id: String,
    navController: NavHostController,
    page: DirectorySubPage,
) {
    val viewModel: DirectoryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(authState.session.tenant?.id) {
        viewModel.load(authState.session)
    }

    val group = state.directories.firstOrNull { it.id == id } ?: viewModel.find(id)
    when {
        state.isLoading && group == null -> PremiumLoadingState(text = "Carregando diretório")
        group != null -> when (page) {
            DirectorySubPage.Members -> DirectoryMembersScreen(group, onBackClick = { navController.navigateUp() })
            DirectorySubPage.Agenda -> DirectoryAgendaScreen(group, onBackClick = { navController.navigateUp() })
            DirectorySubPage.Store -> DirectoryStoreScreen(group, onBackClick = { navController.navigateUp() })
            DirectorySubPage.Events -> DirectoryEventsScreen(group, onBackClick = { navController.navigateUp() })
            DirectorySubPage.Info -> DirectoryInfoScreen(group, onBackClick = { navController.navigateUp() })
        }
        else -> CollectiveUnavailableScreen(
            title = "Diretório não encontrado",
            subtitle = state.errorMessage ?: "O diretório pode ter sido removido ou ainda não está publicado nesta atlética.",
            onBackClick = { navController.navigateUp() },
        )
    }
}

@Composable
private fun commissionRouteState(
    authState: AuthUiState,
    id: String,
    navController: NavHostController,
    page: CommissionSubPage,
) {
    val viewModel: CommissionsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(authState.session.tenant?.id) {
        viewModel.load(authState.session)
    }

    val group = state.commissions.firstOrNull { it.id == id } ?: viewModel.find(id)
    when {
        state.isLoading && group == null -> PremiumLoadingState(text = "Carregando comissão")
        group != null -> when (page) {
            CommissionSubPage.Members -> CommissionMembersScreen(group, onBackClick = { navController.navigateUp() })
            CommissionSubPage.Agenda -> CommissionAgendaScreen(group, onBackClick = { navController.navigateUp() })
            CommissionSubPage.Store -> CommissionStoreScreen(group, onBackClick = { navController.navigateUp() })
            CommissionSubPage.Events -> CommissionEventsScreen(group, onBackClick = { navController.navigateUp() })
        }
        else -> CollectiveUnavailableScreen(
            title = "Comissão não encontrada",
            subtitle = state.errorMessage ?: "A comissão pode ter sido removida ou ainda não está publicada nesta atlética.",
            onBackClick = { navController.navigateUp() },
        )
    }
}

private enum class LeagueSubPage {
    Members,
    Agenda,
    Store,
    Events,
    Info,
}

private enum class DirectorySubPage {
    Members,
    Agenda,
    Store,
    Events,
    Info,
}

private enum class CommissionSubPage {
    Members,
    Agenda,
    Store,
    Events,
}

private fun NavGraphBuilder.miniVendorRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.MiniVendor) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Mini-vendor") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorScreen(
                state = state,
                onProductsClick = { navController.navigate(AppRoute.MiniVendorProducts) },
                onPendingOrdersClick = { navController.navigate(AppRoute.MiniVendorPendingOrders) },
                onApprovedOrdersClick = { navController.navigate(AppRoute.MiniVendorApprovedOrders) },
                onFinanceClick = { navController.navigate(AppRoute.MiniVendorFinance) },
            )
        }
    }
    composable(AppRoute.MiniVendorProducts) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Produtos mini-vendor") {
            miniVendorState(authState) { MiniVendorProductsScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorPendingOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos pendentes") {
            miniVendorState(authState) { MiniVendorPendingOrdersScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorApprovedOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos aprovados") {
            miniVendorState(authState) { MiniVendorApprovedOrdersScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorFinance) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Financeiro mini-vendor") {
            miniVendorState(authState) { MiniVendorFinanceScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.SalesMode) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Modo vendas") {
            miniVendorState(authState) {
                SalesModeScreen(
                    it,
                    { navController.navigate(AppRoute.SalesModeEventMenu) },
                    { navController.navigate(AppRoute.ProductWithdrawalScanner) },
                )
            }
        }
    }
    composable(AppRoute.SalesModeEventMenu) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Menu do evento") {
            miniVendorState(authState) { SalesModeEventMenuScreen(it, { navController.navigateUp() }) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun miniVendorState(
    authState: AuthUiState,
    content: @androidx.compose.runtime.Composable (com.example.usc1.ui.vendor.MiniVendorUiState) -> Unit,
) {
    val viewModel: MiniVendorViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
        viewModel.load(authState.session)
    }
    content(state)
}

private fun NavGraphBuilder.scannerRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.Scanner) {
        PermissionGate(authState, Permission.UseScanner, "Scanner") {
            val viewModel: ScannerViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            ScannerScreen(
                state = state,
                onEventScannerClick = { navController.navigate(AppRoute.EventCheckInScanner) },
                onPartyScannerClick = { navController.navigate(AppRoute.PartyScanner) },
                onProductScannerClick = { navController.navigate(AppRoute.ProductWithdrawalScanner) },
            )
        }
    }
    composable(AppRoute.EventCheckInScanner) {
        PermissionGate(authState, Permission.UseScanner, "Scanner Eventos") {
            scannerState { EventCheckInScannerScreen(it, { navController.navigate(AppRoute.ScannerSuccess) }, { navController.navigate(AppRoute.ScannerError) }, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.PartyScanner) {
        PermissionGate(authState, Permission.UseScanner, "Scanner Festas") {
            scannerState { PartyScannerScreen(it, { navController.navigate(AppRoute.ScannerSuccess) }, { navController.navigate(AppRoute.ScannerError) }, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.ProductWithdrawalScanner) {
        PermissionGate(authState, Permission.UseScanner, "Scanner Produtos") {
            scannerState { ProductWithdrawalScannerScreen(it, { navController.navigate(AppRoute.ScannerSuccess) }, { navController.navigate(AppRoute.ScannerError) }, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.ScannerSuccess) {
        PermissionGate(authState, Permission.UseScanner, "Scanner") {
            scannerState { ScannerResultSuccessScreen(it.successResult, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.ScannerError) {
        PermissionGate(authState, Permission.UseScanner, "Scanner") {
            scannerState { ScannerResultErrorScreen(it.errorResult, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.ScannerPermissionDenied) { ScannerPermissionDeniedScreen() }
}

@androidx.compose.runtime.Composable
private fun scannerState(content: @androidx.compose.runtime.Composable (com.example.usc1.ui.scanner.ScannerUiState) -> Unit) {
    val viewModel: ScannerViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    content(state)
}

private fun NavGraphBuilder.guideRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.Guide) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.guideState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        GuideScreen(
            state = state,
            onFaqClick = { navController.navigate(AppRoute.Faq) { launchSingleTop = true } },
            onSupportClick = { navController.navigate(AppRoute.Support) { launchSingleTop = true } },
        )
    }
    composable(AppRoute.Legal) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.legalState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        LegalDocumentScreen(state, { navController.navigateUp() })
    }
    composable(AppRoute.Faq) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.guideState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        FaqScreen(state, { navController.navigateUp() })
    }
    composable(AppRoute.ContactUsc) { ContactUscScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.Support) { SupportScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.Terms) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.legalState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        TermsScreen(state, { navController.navigateUp() })
    }
    composable(AppRoute.PrivacyLgpd) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.legalState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        PrivacyLgpdScreen(state, { navController.navigate(AppRoute.LgpdRequest) }, { navController.navigateUp() })
    }
    composable(AppRoute.LgpdRequest) { LgpdRequestScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.LegalDocument) {
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.legalState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        LegalDocumentScreen(state, { navController.navigateUp() })
    }
}

private fun NavGraphBuilder.albumRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.Album) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        AlbumScreen(state, { turma -> navController.navigate(AppRoute.albumTurma(turma.id)) }, { navController.navigate(AppRoute.CacaCalouro) })
    }
    composable(AppRoute.AlbumTurma, listOf(navArgument("turmaId") { type = NavType.StringType })) { entry ->
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val id = entry.arguments?.getString("turmaId").orEmpty()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        val turma = viewModel.findTurma(id)
        when {
            state.isLoading && turma == null -> PremiumLoadingState(text = "Carregando turma")
            turma != null -> AlbumTurmaScreen(turma, state, { navController.navigateUp() })
            else -> AlbumUnavailableScreen(onBackClick = { navController.navigateUp() })
        }
    }
    composable(AppRoute.CacaCalouro) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        CacaCalouroScreen(state, { navController.navigate(AppRoute.CalouroRanking) }, { navController.navigateUp() })
    }
    composable(AppRoute.CalouroRanking) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        CalouroRankingScreen(state, { navController.navigateUp() })
    }
}

private fun NavGraphBuilder.gamesRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.Games) {
        val viewModel: GamesViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        GamesScreen(state, { navController.navigate(AppRoute.Boardround) }, { navController.navigate(AppRoute.Achievements) }, { navController.navigate(AppRoute.Loyalty) })
    }
    composable(AppRoute.Boardround) { gamesState(authState) { BoardroundScreen(it, { navController.navigate(AppRoute.BoardroundRanking) }, { navController.navigate(AppRoute.BoardroundStats) }, { navController.navigate(AppRoute.GameRules) }) } }
    composable(AppRoute.BoardroundRanking) { gamesState(authState) { BoardroundRankingScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.BoardroundStats) { gamesState(authState) { BoardroundStatsScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.Achievements) { gamesState(authState) { AchievementsScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.Loyalty) { gamesState(authState) { LoyaltyScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.GameRules) { GameRulesScreen(onBackClick = { navController.navigateUp() }) }
}

@androidx.compose.runtime.Composable
private fun gamesState(
    authState: AuthUiState,
    content: @androidx.compose.runtime.Composable (com.example.usc1.ui.games.GamesUiState) -> Unit,
) {
    val viewModel: GamesViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
        viewModel.load(authState.session)
    }
    content(state)
}

private fun NavGraphBuilder.generalOrderRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.OrdersHub) {
        val viewModel: GeneralOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        OrdersHubScreen(
            state = state,
            onTypeClick = { type -> navController.navigate(AppRoute.ordersByType(type.name)) },
            onOrderClick = { order -> navController.navigate(AppRoute.generalOrderDetail(order.id)) },
        )
    }
    composable(AppRoute.OrdersByType, listOf(navArgument("type") { type = NavType.StringType })) { entry ->
        val viewModel: GeneralOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val type = generalOrderType(entry.arguments?.getString("type"))
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        OrdersByTypeScreen(
            state = state,
            type = type,
            onStatusClick = viewModel::selectStatus,
            onOrderClick = { order -> navController.navigate(AppRoute.generalOrderDetail(order.id)) },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.GeneralOrderDetail, listOf(navArgument("orderId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("orderId").orEmpty()
        val viewModel: GeneralOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id, id) {
            viewModel.load(authState.session)
        }
        val order = state.orders.firstOrNull { it.id == id }
        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando pedido")
            order != null -> GeneralOrderDetailScreen(order, { navController.navigateUp() })
            else -> GeneralOrderUnavailableScreen({ navController.navigateUp() })
        }
    }
}

private fun generalOrderType(value: String?): GeneralOrderType? =
    GeneralOrderType.entries.firstOrNull { it.name == value }

@Composable
private fun PermissionGate(
    authState: AuthUiState,
    permission: Permission,
    title: String,
    content: @Composable () -> Unit,
) {
    val decision = remember(authState.session, permission) {
        PermissionPolicy().canUsePermission(authState.session, permission)
    }

    if (decision.allowed) {
        content()
    } else {
        PermissionDeniedScreen(
            title = title,
            subtitle = permissionMessage(permission, decision.reason),
        )
    }
}

private fun permissionMessage(
    permission: Permission,
    reason: PermissionBlockReason?,
): String {
    val module = when (permission) {
        Permission.UseScanner -> "Scanner e check-in"
        Permission.ManageMiniVendor -> "Mini-vendor e modo vendas"
        Permission.ManageTenant -> "Gestão da atlética"
        else -> "Este módulo"
    }
    val cause = when (reason) {
        PermissionBlockReason.NotAuthenticated -> "Entre novamente para continuar."
        PermissionBlockReason.Banned -> "Usuário bloqueado não pode acessar operações."
        PermissionBlockReason.TenantPending -> "A associação à atlética ainda está pendente."
        PermissionBlockReason.MissingRole -> "Use uma conta mockada com role admin, vendas, mini-vendor ou master."
        PermissionBlockReason.ModuleHidden -> "O módulo não está habilitado para este tenant."
        PermissionBlockReason.InviteRequired -> "O convite da atlética ainda é obrigatório."
        PermissionBlockReason.FeatureNotNativeYet -> "A funcionalidade ainda não foi migrada para nativo."
        null -> "Permissão insuficiente para abrir esta área."
    }
    return "$module exige uma role autorizada. $cause"
}
