package com.example.usc1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.usc1.ui.album.AlbumMockData
import com.example.usc1.ui.album.AlbumScreen
import com.example.usc1.ui.album.AlbumTurmaScreen
import com.example.usc1.ui.album.AlbumViewModel
import com.example.usc1.ui.album.CacaCalouroScreen
import com.example.usc1.ui.album.CalouroRankingScreen
import com.example.usc1.ui.auth.AuthUiState
import com.example.usc1.ui.collectives.CollectiveMockData
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
import com.example.usc1.ui.community.CommunityMockData
import com.example.usc1.ui.community.CommunityPostDetailScreen
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
import com.example.usc1.ui.generalorders.GeneralOrdersMockData
import com.example.usc1.ui.generalorders.GeneralOrdersViewModel
import com.example.usc1.ui.generalorders.OrdersByTypeScreen
import com.example.usc1.ui.generalorders.OrdersHubScreen
import com.example.usc1.ui.guide.ContactUscScreen
import com.example.usc1.ui.guide.FaqScreen
import com.example.usc1.ui.guide.GuideScreen
import com.example.usc1.ui.guide.GuideUiState
import com.example.usc1.ui.guide.LegalDocumentScreen
import com.example.usc1.ui.guide.LegalUiState
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
        CommunityScreen(
            state = state,
            onTabClick = viewModel::selectTab,
            onPostClick = { post -> navController.navigate(AppRoute.communityPostDetail(post.id)) },
        )
    }

    composable(
        route = AppRoute.CommunityPostDetail,
        arguments = listOf(navArgument("postId") { type = NavType.StringType }),
    ) { entry ->
        val postId = entry.arguments?.getString("postId").orEmpty()
        val post = remember(postId) { CommunityMockData.postById(postId) }
        CommunityPostDetailScreen(post = post, onBackClick = { navController.navigateUp() })
    }

    composable(AppRoute.Leagues) {
        val viewModel: LeaguesViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LeaguesScreen(state = state, onLeagueClick = { navController.navigate(AppRoute.leagueDetail(it.id)) })
    }
    collectiveLeagueRoutes(navController)

    composable(AppRoute.Directory) {
        val viewModel: DirectoryViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        DirectoryScreen(state = state, onDirectoryClick = { navController.navigate(AppRoute.directoryDetail(it.id)) })
    }
    directoryRoutes(navController)

    composable(AppRoute.Commissions) {
        val viewModel: CommissionsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        CommissionsScreen(state = state, onCommissionClick = { navController.navigate(AppRoute.commissionDetail(it.id)) })
    }
    commissionRoutes(navController)

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
    scannerRoutes(navController, authState)
    guideRoutes(navController)
    albumRoutes(navController)
    gamesRoutes(navController)
    generalOrderRoutes(navController)
}

private fun NavGraphBuilder.collectiveLeagueRoutes(navController: NavHostController) {
    composable(AppRoute.LeagueDetail, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("leagueId").orEmpty()
        val group = remember(id) { CollectiveMockData.leagueById(id) }
        LeagueDetailScreen(
            league = group,
            onMembersClick = { navController.navigate(AppRoute.leagueMembers(group.id)) },
            onAgendaClick = { navController.navigate(AppRoute.leagueAgenda(group.id)) },
            onStoreClick = { navController.navigate(AppRoute.leagueStore(group.id)) },
            onEventsClick = { navController.navigate(AppRoute.leagueEvents(group.id)) },
            onInfoClick = { navController.navigate(AppRoute.leagueInfo(group.id)) },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.LeagueMembers, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry -> LeagueMembersScreen(CollectiveMockData.leagueById(entry.arguments?.getString("leagueId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.LeagueAgenda, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry -> LeagueAgendaScreen(CollectiveMockData.leagueById(entry.arguments?.getString("leagueId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.LeagueStore, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry -> LeagueStoreScreen(CollectiveMockData.leagueById(entry.arguments?.getString("leagueId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.LeagueEvents, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry -> LeagueEventsScreen(CollectiveMockData.leagueById(entry.arguments?.getString("leagueId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.LeagueInfo, listOf(navArgument("leagueId") { type = NavType.StringType })) { entry -> LeagueInfoScreen(CollectiveMockData.leagueById(entry.arguments?.getString("leagueId").orEmpty()), { navController.navigateUp() }) }
}

private fun NavGraphBuilder.directoryRoutes(navController: NavHostController) {
    composable(AppRoute.DirectoryDetail, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("directoryId").orEmpty()
        val group = remember(id) { CollectiveMockData.directoryById(id) }
        DirectoryDetailScreen(
            directory = group,
            onMembersClick = { navController.navigate(AppRoute.directoryMembers(group.id)) },
            onAgendaClick = { navController.navigate(AppRoute.directoryAgenda(group.id)) },
            onStoreClick = { navController.navigate(AppRoute.directoryStore(group.id)) },
            onEventsClick = { navController.navigate(AppRoute.directoryEvents(group.id)) },
            onInfoClick = { navController.navigate(AppRoute.directoryInfo(group.id)) },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.DirectoryMembers, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry -> DirectoryMembersScreen(CollectiveMockData.directoryById(entry.arguments?.getString("directoryId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.DirectoryAgenda, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry -> DirectoryAgendaScreen(CollectiveMockData.directoryById(entry.arguments?.getString("directoryId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.DirectoryStore, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry -> DirectoryStoreScreen(CollectiveMockData.directoryById(entry.arguments?.getString("directoryId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.DirectoryEvents, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry -> DirectoryEventsScreen(CollectiveMockData.directoryById(entry.arguments?.getString("directoryId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.DirectoryInfo, listOf(navArgument("directoryId") { type = NavType.StringType })) { entry -> DirectoryInfoScreen(CollectiveMockData.directoryById(entry.arguments?.getString("directoryId").orEmpty()), { navController.navigateUp() }) }
}

private fun NavGraphBuilder.commissionRoutes(navController: NavHostController) {
    composable(AppRoute.CommissionDetail, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("commissionId").orEmpty()
        val group = remember(id) { CollectiveMockData.commissionById(id) }
        CommissionDetailScreen(
            commission = group,
            onMembersClick = { navController.navigate(AppRoute.commissionMembers(group.id)) },
            onAgendaClick = { navController.navigate(AppRoute.commissionAgenda(group.id)) },
            onStoreClick = { navController.navigate(AppRoute.commissionStore(group.id)) },
            onEventsClick = { navController.navigate(AppRoute.commissionEvents(group.id)) },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.CommissionMembers, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry -> CommissionMembersScreen(CollectiveMockData.commissionById(entry.arguments?.getString("commissionId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.CommissionAgenda, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry -> CommissionAgendaScreen(CollectiveMockData.commissionById(entry.arguments?.getString("commissionId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.CommissionStore, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry -> CommissionStoreScreen(CollectiveMockData.commissionById(entry.arguments?.getString("commissionId").orEmpty()), { navController.navigateUp() }) }
    composable(AppRoute.CommissionEvents, listOf(navArgument("commissionId") { type = NavType.StringType })) { entry -> CommissionEventsScreen(CollectiveMockData.commissionById(entry.arguments?.getString("commissionId").orEmpty()), { navController.navigateUp() }) }
}

private fun NavGraphBuilder.miniVendorRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.MiniVendor) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Mini-vendor") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
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
            miniVendorState { MiniVendorProductsScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorPendingOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos pendentes") {
            miniVendorState { MiniVendorPendingOrdersScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorApprovedOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos aprovados") {
            miniVendorState { MiniVendorApprovedOrdersScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorFinance) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Financeiro mini-vendor") {
            miniVendorState { MiniVendorFinanceScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.SalesMode) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Modo vendas") {
            miniVendorState {
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
            miniVendorState { SalesModeEventMenuScreen(it, { navController.navigateUp() }) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun miniVendorState(content: @androidx.compose.runtime.Composable (com.example.usc1.ui.vendor.MiniVendorUiState) -> Unit) {
    val viewModel: MiniVendorViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
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

private fun NavGraphBuilder.guideRoutes(navController: NavHostController) {
    composable(AppRoute.Guide) { GuideScreen(GuideUiState(), { navController.navigate(AppRoute.Faq) }, { navController.navigate(AppRoute.Support) }) }
    composable(AppRoute.Legal) { LegalDocumentScreen(LegalUiState(), { navController.navigateUp() }) }
    composable(AppRoute.Faq) { FaqScreen(GuideUiState(), { navController.navigateUp() }) }
    composable(AppRoute.ContactUsc) { ContactUscScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.Support) { SupportScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.Terms) { TermsScreen(LegalUiState(), { navController.navigateUp() }) }
    composable(AppRoute.PrivacyLgpd) { PrivacyLgpdScreen(LegalUiState(), { navController.navigate(AppRoute.LgpdRequest) }, { navController.navigateUp() }) }
    composable(AppRoute.LgpdRequest) { LgpdRequestScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.LegalDocument) { LegalDocumentScreen(LegalUiState(), { navController.navigateUp() }) }
}

private fun NavGraphBuilder.albumRoutes(navController: NavHostController) {
    composable(AppRoute.Album) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        AlbumScreen(state, { turma -> navController.navigate(AppRoute.albumTurma(turma.id)) }, { navController.navigate(AppRoute.CacaCalouro) })
    }
    composable(AppRoute.AlbumTurma, listOf(navArgument("turmaId") { type = NavType.StringType })) { entry ->
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val id = entry.arguments?.getString("turmaId").orEmpty()
        AlbumTurmaScreen(AlbumMockData.turmaById(id), state, { navController.navigateUp() })
    }
    composable(AppRoute.CacaCalouro) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        CacaCalouroScreen(state, { navController.navigate(AppRoute.CalouroRanking) }, { navController.navigateUp() })
    }
    composable(AppRoute.CalouroRanking) {
        val viewModel: AlbumViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        CalouroRankingScreen(state, { navController.navigateUp() })
    }
}

private fun NavGraphBuilder.gamesRoutes(navController: NavHostController) {
    composable(AppRoute.Games) {
        val viewModel: GamesViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        GamesScreen(state, { navController.navigate(AppRoute.Boardround) }, { navController.navigate(AppRoute.Achievements) }, { navController.navigate(AppRoute.Loyalty) })
    }
    composable(AppRoute.Boardround) { gamesState { BoardroundScreen(it, { navController.navigate(AppRoute.BoardroundRanking) }, { navController.navigate(AppRoute.BoardroundStats) }, { navController.navigate(AppRoute.GameRules) }) } }
    composable(AppRoute.BoardroundRanking) { gamesState { BoardroundRankingScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.BoardroundStats) { gamesState { BoardroundStatsScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.Achievements) { gamesState { AchievementsScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.Loyalty) { gamesState { LoyaltyScreen(it, { navController.navigateUp() }) } }
    composable(AppRoute.GameRules) { GameRulesScreen(onBackClick = { navController.navigateUp() }) }
}

@androidx.compose.runtime.Composable
private fun gamesState(content: @androidx.compose.runtime.Composable (com.example.usc1.ui.games.GamesUiState) -> Unit) {
    val viewModel: GamesViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    content(state)
}

private fun NavGraphBuilder.generalOrderRoutes(navController: NavHostController) {
    composable(AppRoute.OrdersHub) {
        val viewModel: GeneralOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        OrdersHubScreen(state, { type -> navController.navigate(AppRoute.ordersByType(type.name)) }, { order -> navController.navigate(AppRoute.generalOrderDetail(order.id)) })
    }
    composable(AppRoute.OrdersByType, listOf(navArgument("type") { type = NavType.StringType })) { entry ->
        val viewModel: GeneralOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val type = generalOrderType(entry.arguments?.getString("type"))
        OrdersByTypeScreen(state, type, viewModel::selectStatus, { order -> navController.navigate(AppRoute.generalOrderDetail(order.id)) }, { navController.navigateUp() })
    }
    composable(AppRoute.GeneralOrderDetail, listOf(navArgument("orderId") { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString("orderId").orEmpty()
        GeneralOrderDetailScreen(GeneralOrdersMockData.orderById(id), { navController.navigateUp() })
    }
}

private fun generalOrderType(value: String?): GeneralOrderType? =
    GeneralOrderType.values().firstOrNull { it.name == value }

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
