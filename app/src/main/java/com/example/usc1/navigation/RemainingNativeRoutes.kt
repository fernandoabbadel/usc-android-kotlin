package com.example.usc1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab
import com.example.usc1.core.permissions.Permission
import com.example.usc1.core.permissions.PermissionBlockReason
import com.example.usc1.core.permissions.PermissionPolicy
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.ui.PermissionDeniedScreen
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.ui.album.AlbumScreen
import com.example.usc1.ui.album.AlbumTurmaScreen
import com.example.usc1.ui.album.AlbumUnavailableScreen
import com.example.usc1.ui.album.AlbumViewModel
import com.example.usc1.ui.album.CacaCalouroScreen
import com.example.usc1.ui.album.CalouroRankingScreen
import com.example.usc1.ui.auth.AuthUiState
import com.example.usc1.ui.collectives.CollectiveCatalogScreen
import com.example.usc1.ui.collectives.CollectiveCatalogViewModel
import com.example.usc1.ui.collectives.CollectiveDetailScreen
import com.example.usc1.ui.collectives.CollectiveDetailViewModel
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.CollectiveTab
import com.example.usc1.ui.collectives.PrimaryDirectoryUnavailableScreen
import com.example.usc1.domain.repository.CommunityReactionField
import com.example.usc1.ui.community.CommunityPostDetailScreen
import com.example.usc1.ui.community.CommunityPostUnavailableScreen
import com.example.usc1.ui.community.CommunityScreen
import com.example.usc1.ui.community.CommunityViewModel
import com.example.usc1.ui.events.EventSalesModeMenuScreen
import com.example.usc1.ui.events.EventSalesModeScreen
import com.example.usc1.ui.events.SalesModeEventsViewModel
import com.example.usc1.ui.games.AchievementsScreen
import com.example.usc1.ui.games.BoardroundRankingScreen
import com.example.usc1.ui.games.BoardroundScreen
import com.example.usc1.ui.games.BoardroundStatsScreen
import com.example.usc1.ui.games.GameRulesScreen
import com.example.usc1.ui.games.GamesScreen
import com.example.usc1.ui.games.GamesViewModel
import com.example.usc1.ui.games.LoyaltyScreen
import com.example.usc1.ui.company.CompanyDashboardScreen
import com.example.usc1.ui.company.CompanyDashboardViewModel
import com.example.usc1.ui.company.CompanyEditScreen
import com.example.usc1.ui.company.CompanyEditViewModel
import com.example.usc1.ui.company.CompanyHistoryScreen
import com.example.usc1.ui.company.CompanyHistoryViewModel
import com.example.usc1.ui.company.CompanyLoginScreen
import com.example.usc1.ui.company.CompanyLoginViewModel
import com.example.usc1.ui.company.CompanyRegisterScreen
import com.example.usc1.ui.company.CompanyRegisterViewModel
import com.example.usc1.ui.guide.ContactUscScreen
import com.example.usc1.ui.guide.FaqScreen
import com.example.usc1.ui.guide.FaqViewModel
import com.example.usc1.ui.guide.GuideScreen
import com.example.usc1.ui.guide.GuideViewModel
import com.example.usc1.ui.guide.LegalDocumentScreen
import com.example.usc1.ui.guide.LgpdRequestScreen
import com.example.usc1.ui.guide.PrivacyLgpdScreen
import com.example.usc1.ui.guide.TermsScreen
import com.example.usc1.ui.history.OrganogramScreen
import com.example.usc1.ui.history.OrganogramViewModel
import com.example.usc1.ui.history.TenantHistoryScreen
import com.example.usc1.ui.history.TenantHistoryViewModel
import com.example.usc1.ui.ranking.RankingClassScreen
import com.example.usc1.ui.ranking.RankingClassViewModel
import com.example.usc1.ui.ranking.RankingScreen
import com.example.usc1.ui.ranking.RankingViewModel
import com.example.usc1.ui.scanner.EventCheckInScannerScreen
import com.example.usc1.ui.scanner.PartyScannerScreen
import com.example.usc1.ui.scanner.ProductWithdrawalScannerScreen
import com.example.usc1.ui.scanner.ScannerPermissionDeniedScreen
import com.example.usc1.ui.scanner.ScannerResultErrorScreen
import com.example.usc1.ui.scanner.ScannerResultSuccessScreen
import com.example.usc1.ui.scanner.ScannerScreen
import com.example.usc1.ui.orders.UserOrderDetailScreen
import com.example.usc1.ui.orders.UserOrderTicketDetailScreen
import com.example.usc1.ui.orders.UserOrdersByTabScreen
import com.example.usc1.ui.orders.UserOrdersHubScreen
import com.example.usc1.ui.orders.UserOrdersViewModel
import com.example.usc1.ui.scanner.ScannerViewModel
import com.example.usc1.ui.settings.SettingsInvitesHistoryScreen
import com.example.usc1.ui.settings.SettingsInvitesScreen
import com.example.usc1.ui.settings.SettingsMentorshipScreen
import com.example.usc1.ui.settings.SettingsSupportScreen
import com.example.usc1.ui.settings.SettingsSupportViewModel
import com.example.usc1.ui.settings.SettingsTurmaLeaderScreen
import com.example.usc1.ui.settings.SettingsViewModel
import com.example.usc1.ui.settings.withSession
import com.example.usc1.ui.tenant.TenantSwitcherScreen
import com.example.usc1.ui.tenant.TenantViewModel
import com.example.usc1.ui.vendor.MiniVendorApprovedOrdersScreen
import com.example.usc1.ui.vendor.MiniVendorEditableProfileScreen
import com.example.usc1.ui.vendor.MiniVendorFinanceScreen
import com.example.usc1.ui.vendor.MiniVendorManagementScreen
import com.example.usc1.ui.vendor.MiniVendorPendingOrdersScreen
import com.example.usc1.ui.vendor.MiniVendorProductsScreen
import com.example.usc1.ui.vendor.MiniVendorScreen
import com.example.usc1.ui.vendor.MiniVendorViewModel

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
            onPostDraftChange = viewModel::onPostDraftChange,
            onSubmitPost = { viewModel.createPost(authState.session) },
            onPostClick = { post -> navController.navigate(AppRoute.communityPostDetail(post.id)) },
            onAuthorClick = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(AppRoute.profileDetail(userId)) { launchSingleTop = true }
                }
            },
            onCommentClick = { postId -> viewModel.openComments(authState.session, postId) },
            onLikeClick = { postId ->
                viewModel.toggleReaction(authState.session, postId, CommunityReactionField.Likes)
            },
            onHypeClick = { postId ->
                viewModel.toggleReaction(authState.session, postId, CommunityReactionField.Hype)
            },
            onCommentDraftChange = viewModel::onCommentDraftChange,
            onSubmitComment = { viewModel.submitComment(authState.session) },
            onCloseComments = viewModel::closeComments,
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

    collectiveRoutes(navController, authState)

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
    rankingRoutes(navController, authState)
    tenantHistoryRoutes(navController, authState)
    companyRoutes(navController)
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
            onRevokeInviteClick = { entry ->
                viewModel.revokeInvite(authState.session, entry.id)
            },
            onRequestMoreClick = { viewModel.requestMoreInvites(authState.session) },
            onOpenHistoryClick = {
                navController.navigate(AppRoute.SettingsInvitesHistory) { launchSingleTop = true }
            },
        )
    }

    composable(AppRoute.SettingsInvitesHistory) {
        val viewModel: SettingsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.loadInviteDashboard(authState.session)
        }

        SettingsInvitesHistoryScreen(
            state = state.withSession(authState.session),
            session = authState.session,
            onBackClick = { navController.navigateUp() },
            onRefreshClick = { viewModel.loadInviteDashboard(authState.session, forceRefresh = true) },
        )
    }

    composable(AppRoute.SettingsMentorship) {
        val viewModel: SettingsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.loadMentorshipHub(authState.session)
            viewModel.loadMentorshipCandidates(authState.session)
        }

        SettingsMentorshipScreen(
            state = state.withSession(authState.session),
            session = authState.session,
            onBackClick = { navController.navigateUp() },
            onRefreshClick = {
                viewModel.loadMentorshipHub(authState.session, forceRefresh = true)
                viewModel.loadMentorshipCandidates(authState.session, forceRefresh = true)
            },
            onSendInviteClick = { targetUserId, targetIsMentor ->
                viewModel.sendMentorshipInvite(authState.session, targetUserId, targetIsMentor)
            },
            onRespondClick = { relationshipId, action, roleLabel ->
                viewModel.respondToMentorshipInvite(
                    session = authState.session,
                    relationshipId = relationshipId,
                    action = action,
                    selectedRoleLabel = roleLabel,
                )
            },
            onEditRoleLabelClick = { relationshipId, side, label ->
                viewModel.updateMentorshipRoleLabel(authState.session, relationshipId, side, label)
            },
        )
    }

    composable(AppRoute.SettingsTurmaLeader) {
        val viewModel: SettingsViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val resolvedState = state.withSession(authState.session)

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.loadTurmaLeaderPending(authState.session)
        }

        // Mesmo gate do web: líder de turma ou papel de gestão do tenant.
        if (!resolvedState.isTurmaLeader && !resolvedState.canManageTurmaRequests) {
            PermissionDeniedScreen(
                title = "Líder da Turma",
                subtitle = "Área exclusiva de líderes de turma e da gestão da atlética.",
            )
        } else {
            SettingsTurmaLeaderScreen(
                state = resolvedState,
                onBackClick = { navController.navigateUp() },
                onRefreshClick = { viewModel.loadTurmaLeaderPending(authState.session, forceRefresh = true) },
            )
        }
    }
}

/**
 * Área pública dos coletivos.
 *
 * Web: `/ligas_usc` e `/ligas_usc/[leagueId]/{,membros,agenda,loja}`,
 * `/comissoes` e `/comissoes/[leagueId]/{,membros,agenda,loja}`,
 * `/diretorio/{,membros,agenda,loja}` (registro primário) e
 * `/diretorio/[leagueId]/{,membros,agenda,loja}`.
 */
private fun NavGraphBuilder.collectiveRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    catalogRoute(navController, authState, AppRoute.Leagues, CollectiveKind.League)
    catalogRoute(navController, authState, AppRoute.Commissions, CollectiveKind.Commission)

    detailRoute(navController, authState, AppRoute.LeagueDetail, "leagueId", CollectiveKind.League, CollectiveTab.Overview)
    detailRoute(navController, authState, AppRoute.LeagueMembers, "leagueId", CollectiveKind.League, CollectiveTab.Members)
    detailRoute(navController, authState, AppRoute.LeagueAgenda, "leagueId", CollectiveKind.League, CollectiveTab.Agenda)
    detailRoute(navController, authState, AppRoute.LeagueStore, "leagueId", CollectiveKind.League, CollectiveTab.Store)

    detailRoute(navController, authState, AppRoute.CommissionDetail, "commissionId", CollectiveKind.Commission, CollectiveTab.Overview)
    detailRoute(navController, authState, AppRoute.CommissionMembers, "commissionId", CollectiveKind.Commission, CollectiveTab.Members)
    detailRoute(navController, authState, AppRoute.CommissionAgenda, "commissionId", CollectiveKind.Commission, CollectiveTab.Agenda)
    detailRoute(navController, authState, AppRoute.CommissionStore, "commissionId", CollectiveKind.Commission, CollectiveTab.Store)

    detailRoute(navController, authState, AppRoute.DirectoryDetail, "directoryId", CollectiveKind.Directory, CollectiveTab.Overview)
    detailRoute(navController, authState, AppRoute.DirectoryMembers, "directoryId", CollectiveKind.Directory, CollectiveTab.Members)
    detailRoute(navController, authState, AppRoute.DirectoryAgenda, "directoryId", CollectiveKind.Directory, CollectiveTab.Agenda)
    detailRoute(navController, authState, AppRoute.DirectoryStore, "directoryId", CollectiveKind.Directory, CollectiveTab.Store)

    // `PrimaryDirectoryPage` do web: a raiz usa o registro primário do diretório.
    primaryDirectoryRoute(navController, authState, AppRoute.Directory, CollectiveTab.Overview)
    primaryDirectoryRoute(navController, authState, AppRoute.DirectoryRootMembers, CollectiveTab.Members)
    primaryDirectoryRoute(navController, authState, AppRoute.DirectoryRootAgenda, CollectiveTab.Agenda)
    primaryDirectoryRoute(navController, authState, AppRoute.DirectoryRootStore, CollectiveTab.Store)
}

private fun NavGraphBuilder.catalogRoute(
    navController: NavHostController,
    authState: AuthUiState,
    route: String,
    kind: CollectiveKind,
) {
    composable(route) {
        val viewModel: CollectiveCatalogViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session, kind)
        }
        CollectiveCatalogScreen(
            state = state,
            onGroupClick = { navController.navigate(collectiveDetailRoute(kind, it.id)) },
            onBackClick = { navController.navigateUp() },
            onLikeClick = { viewModel.toggleLike(authState.session, it) },
            onFollowClick = { viewModel.toggleFollow(authState.session, it) },
            onQuizToggleOption = viewModel::toggleQuizOption,
            onQuizAdvance = { viewModel.advanceQuiz(authState.session) },
            onQuizReset = viewModel::resetQuiz,
        )
    }
}

private fun NavGraphBuilder.detailRoute(
    navController: NavHostController,
    authState: AuthUiState,
    route: String,
    argument: String,
    kind: CollectiveKind,
    tab: CollectiveTab,
) {
    composable(route, listOf(navArgument(argument) { type = NavType.StringType })) { entry ->
        val collectiveId = entry.arguments?.getString(argument).orEmpty()
        CollectiveDetailRoute(
            navController = navController,
            authState = authState,
            kind = kind,
            tab = tab,
            collectiveId = collectiveId,
        )
    }
}

private fun NavGraphBuilder.primaryDirectoryRoute(
    navController: NavHostController,
    authState: AuthUiState,
    route: String,
    tab: CollectiveTab,
) {
    composable(route) {
        CollectiveDetailRoute(
            navController = navController,
            authState = authState,
            kind = CollectiveKind.Directory,
            tab = tab,
            collectiveId = "",
            usePrimaryRecord = true,
        )
    }
}

@Composable
private fun CollectiveDetailRoute(
    navController: NavHostController,
    authState: AuthUiState,
    kind: CollectiveKind,
    tab: CollectiveTab,
    collectiveId: String,
    usePrimaryRecord: Boolean = false,
) {
    val viewModel: CollectiveDetailViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id, collectiveId, tab, usePrimaryRecord) {
        viewModel.load(
            session = authState.session,
            kind = kind,
            tab = tab,
            collectiveId = collectiveId,
            usePrimaryRecord = usePrimaryRecord,
        )
    }

    if (usePrimaryRecord && !state.isLoading && state.group == null && state.errorMessage == null) {
        PrimaryDirectoryUnavailableScreen(onBackClick = { navController.navigateUp() })
        return
    }

    CollectiveDetailScreen(
        state = state,
        onBackClick = { navController.navigateUp() },
        onTabClick = { nextTab ->
            val nextRoute = if (usePrimaryRecord) {
                primaryDirectoryTabRoute(nextTab)
            } else {
                collectiveTabRoute(kind, state.group?.id.orEmpty().ifBlank { collectiveId }, nextTab)
            }
            navController.navigate(nextRoute)
        },
        onLikeClick = { viewModel.toggleLike(authState.session) },
        onFollowClick = { viewModel.toggleFollow(authState.session) },
        onRoleClick = viewModel::selectRequestRole,
        onProductClick = { navController.navigate(AppRoute.productDetail(it.id)) },
        onEventClick = { event ->
            val eventId = event.resolvedEventId
            if (eventId.isNotBlank()) navController.navigate(AppRoute.eventDetail(eventId))
        },
    )
}

private fun collectiveDetailRoute(kind: CollectiveKind, collectiveId: String): String = when (kind) {
    CollectiveKind.League -> AppRoute.leagueDetail(collectiveId)
    CollectiveKind.Commission -> AppRoute.commissionDetail(collectiveId)
    CollectiveKind.Directory -> AppRoute.directoryDetail(collectiveId)
}

private fun collectiveTabRoute(kind: CollectiveKind, collectiveId: String, tab: CollectiveTab): String = when (kind) {
    CollectiveKind.League -> when (tab) {
        CollectiveTab.Overview -> AppRoute.leagueDetail(collectiveId)
        CollectiveTab.Members -> AppRoute.leagueMembers(collectiveId)
        CollectiveTab.Agenda -> AppRoute.leagueAgenda(collectiveId)
        CollectiveTab.Store -> AppRoute.leagueStore(collectiveId)
    }

    CollectiveKind.Commission -> when (tab) {
        CollectiveTab.Overview -> AppRoute.commissionDetail(collectiveId)
        CollectiveTab.Members -> AppRoute.commissionMembers(collectiveId)
        CollectiveTab.Agenda -> AppRoute.commissionAgenda(collectiveId)
        CollectiveTab.Store -> AppRoute.commissionStore(collectiveId)
    }

    CollectiveKind.Directory -> when (tab) {
        CollectiveTab.Overview -> AppRoute.directoryDetail(collectiveId)
        CollectiveTab.Members -> AppRoute.directoryMembers(collectiveId)
        CollectiveTab.Agenda -> AppRoute.directoryAgenda(collectiveId)
        CollectiveTab.Store -> AppRoute.directoryStore(collectiveId)
    }
}

private fun primaryDirectoryTabRoute(tab: CollectiveTab): String = when (tab) {
    CollectiveTab.Overview -> AppRoute.Directory
    CollectiveTab.Members -> AppRoute.DirectoryRootMembers
    CollectiveTab.Agenda -> AppRoute.DirectoryRootAgenda
    CollectiveTab.Store -> AppRoute.DirectoryRootStore
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
                onEditClick = { navController.navigate(AppRoute.MiniVendorEdit) },
                onProductsClick = { navController.navigate(AppRoute.MiniVendorProducts) },
                onManagementClick = { navController.navigate(AppRoute.MiniVendorManagement) },
                // A categoria pública do mini vendor é o próprio nome da loja (`categoria: storeName`).
                onPendingOrdersClick = {
                    navController.navigate(
                        AppRoute.miniVendorPendingOrdersByCategory(state.storeName),
                    ) { launchSingleTop = true }
                },
                onApprovedOrdersClick = {
                    navController.navigate(
                        AppRoute.miniVendorApprovedOrdersByCategory(state.storeName),
                    ) { launchSingleTop = true }
                },
                onFinanceClick = { navController.navigate(AppRoute.MiniVendorFinance) },
            )
        }
    }
    composable(AppRoute.MiniVendorEdit) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Editar mini-vendor") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorEditableProfileScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSaveProfile = viewModel::saveProfile,
            )
        }
    }
    composable(AppRoute.MiniVendorManagement) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Gestão mini-vendor") {
            miniVendorState(authState) { MiniVendorManagementScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.MiniVendorProducts) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Produtos mini-vendor") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorProductsScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSaveProduct = viewModel::saveProduct,
                onToggleProductActive = viewModel::setProductActive,
            )
        }
    }
    composable(AppRoute.MiniVendorPendingOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos pendentes") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorPendingOrdersScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSetOrderStatus = viewModel::setOrderStatus,
            )
        }
    }
    composable(AppRoute.MiniVendorApprovedOrders) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos aprovados") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorApprovedOrdersScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSetOrderStatus = viewModel::setOrderStatus,
            )
        }
    }
    composable(
        AppRoute.MiniVendorPendingOrdersByCategory,
        listOf(navArgument("category") { type = NavType.StringType }),
    ) { entry ->
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos pendentes") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            val category = entry.arguments?.getString("category").orEmpty()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorPendingOrdersScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSetOrderStatus = viewModel::setOrderStatus,
                categoryLabel = category,
                onAllCategoriesClick = {
                    navController.navigate(AppRoute.MiniVendorPendingOrders) { launchSingleTop = true }
                },
            )
        }
    }
    composable(
        AppRoute.MiniVendorApprovedOrdersByCategory,
        listOf(navArgument("category") { type = NavType.StringType }),
    ) { entry ->
        PermissionGate(authState, Permission.ManageMiniVendor, "Pedidos aprovados") {
            val viewModel: MiniVendorViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            val category = entry.arguments?.getString("category").orEmpty()
            LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
                viewModel.load(authState.session)
            }
            MiniVendorApprovedOrdersScreen(
                state = state,
                onBackClick = { navController.navigateUp() },
                onSetOrderStatus = viewModel::setOrderStatus,
                categoryLabel = category,
                onAllCategoriesClick = {
                    navController.navigate(AppRoute.MiniVendorApprovedOrders) { launchSingleTop = true }
                },
            )
        }
    }
    composable(AppRoute.MiniVendorFinance) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Financeiro mini-vendor") {
            miniVendorState(authState) { MiniVendorFinanceScreen(it, { navController.navigateUp() }) }
        }
    }
    composable(AppRoute.SalesMode) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Modo vendas") {
            val viewModel: SalesModeEventsViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            EventSalesModeScreen(
                state = state,
                onEventClick = { eventId -> navController.navigate(AppRoute.eventDetail(eventId)) },
                onEventMenuClick = { navController.navigate(AppRoute.SalesModeEventMenu) },
                onOrdersClick = { navController.navigate(AppRoute.EventOrders) },
                onScannerClick = { navController.navigate(AppRoute.ProductWithdrawalScanner) },
                onRetryClick = viewModel::load,
            )
        }
    }
    composable(AppRoute.SalesModeEventMenu) {
        PermissionGate(authState, Permission.ManageMiniVendor, "Menu do evento") {
            val viewModel: SalesModeEventsViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            EventSalesModeMenuScreen(
                state = state,
                onProductClick = { eventId -> navController.navigate(AppRoute.eventDetail(eventId)) },
                onOrdersClick = { navController.navigate(AppRoute.EventOrders) },
                onBackClick = { navController.navigateUp() },
                onRetryClick = viewModel::load,
            )
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
        val viewModel: FaqViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { viewModel.load() }
        FaqScreen(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onSectionClick = viewModel::selectSection,
            onQuestionClick = viewModel::toggleQuestion,
            onDoubtToggle = viewModel::toggleDoubt,
            onDoubtTextChange = viewModel::onDoubtTextChange,
            onSendDoubt = { section, questionId ->
                viewModel.sendDoubt(authState.session, section, questionId)
            },
            onSupportClick = { navController.navigate(AppRoute.ContactUsc) { launchSingleTop = true } },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.ContactUsc) { ContactUscScreen(onBackClick = { navController.navigateUp() }) }
    composable(AppRoute.Support) {
        val viewModel: SettingsSupportViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        SettingsSupportScreen(
            state = state,
            onBackClick = { navController.navigateUp() },
            onSubmitClick = { category, subject, message ->
                viewModel.submit(authState.session, category, subject, message)
            },
        )
    }
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
    composable(
        route = AppRoute.LegalDocumentBySlug,
        arguments = listOf(navArgument("slug") { type = NavType.StringType }),
    ) { backStackEntry ->
        val slug = backStackEntry.arguments?.getString("slug").orEmpty()
        val viewModel: GuideViewModel = viewModel()
        val state by viewModel.legalState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        LegalDocumentScreen(
            state = state,
            onBackClick = { navController.navigateUp() },
            slug = slug,
        )
    }
}

/** `/ranking` e `/ranking/[turmaId]`. */
private fun NavGraphBuilder.rankingRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.Ranking) {
        val viewModel: RankingViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        RankingScreen(
            state = state,
            onTabClick = viewModel::selectTab,
            onUserClick = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(AppRoute.profileDetail(userId)) { launchSingleTop = true }
                }
            },
            onClassClick = { turmaId ->
                if (turmaId.isNotBlank()) {
                    navController.navigate(AppRoute.rankingTurma(turmaId)) { launchSingleTop = true }
                }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(
        route = AppRoute.RankingTurma,
        arguments = listOf(navArgument("turmaId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val turmaId = backStackEntry.arguments?.getString("turmaId").orEmpty()
        val viewModel: RankingClassViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, turmaId) {
            viewModel.load(authState.session, turmaId)
        }
        RankingClassScreen(
            state = state,
            onUserClick = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(AppRoute.profileDetail(userId)) { launchSingleTop = true }
                }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
}

/** `/historico` e `/historico/organograma`. */
private fun NavGraphBuilder.tenantHistoryRoutes(
    navController: NavHostController,
    authState: AuthUiState,
) {
    composable(AppRoute.History) {
        val viewModel: TenantHistoryViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id) {
            viewModel.load(authState.session)
        }
        TenantHistoryScreen(
            state = state,
            onOrganogramClick = {
                navController.navigate(AppRoute.HistoryOrganogram) { launchSingleTop = true }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.HistoryOrganogram) {
        val viewModel: OrganogramViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id) {
            viewModel.load(authState.session)
        }
        OrganogramScreen(
            state = state,
            onMemberClick = { userId ->
                if (userId.isNotBlank()) {
                    navController.navigate(AppRoute.profileDetail(userId)) { launchSingleTop = true }
                }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
}

/** `/empresa`, `/empresa/cadastro`, `/empresa/{id}` e sub-rotas. */
private fun NavGraphBuilder.companyRoutes(navController: NavHostController) {
    composable(AppRoute.Company) {
        val viewModel: CompanyLoginViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(state.authenticatedPartnerId) {
            val partnerId = state.authenticatedPartnerId
            if (partnerId.isNotBlank()) {
                viewModel.consumeNavigation()
                navController.navigate(AppRoute.companyDashboard(partnerId)) { launchSingleTop = true }
            }
        }
        CompanyLoginScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::submit,
            onRegisterClick = {
                navController.navigate(AppRoute.CompanyRegister) { launchSingleTop = true }
            },
            onSupportClick = {
                navController.navigate(AppRoute.ContactUsc) { launchSingleTop = true }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(AppRoute.CompanyRegister) {
        val viewModel: CompanyRegisterViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        CompanyRegisterScreen(
            state = state,
            onSelectTier = viewModel::selectTier,
            onFormChange = viewModel::updateForm,
            onBackToPlans = viewModel::backToPlans,
            onSubmit = viewModel::submit,
            onLoginClick = {
                navController.navigate(AppRoute.Company) {
                    launchSingleTop = true
                    popUpTo(AppRoute.Company) { inclusive = true }
                }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(
        route = AppRoute.CompanyDashboard,
        arguments = listOf(navArgument("companyId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val companyId = backStackEntry.arguments?.getString("companyId").orEmpty()
        val viewModel: CompanyDashboardViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(companyId) { viewModel.load(companyId) }
        CompanyDashboardScreen(
            state = state,
            onEditClick = {
                navController.navigate(AppRoute.companyEdit(companyId)) { launchSingleTop = true }
            },
            onHistoryClick = {
                navController.navigate(AppRoute.companyHistory(companyId)) { launchSingleTop = true }
            },
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(
        route = AppRoute.CompanyEdit,
        arguments = listOf(navArgument("companyId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val companyId = backStackEntry.arguments?.getString("companyId").orEmpty()
        val viewModel: CompanyEditViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(companyId) { viewModel.load(companyId) }
        CompanyEditScreen(
            state = state,
            onWhatsAppChange = viewModel::onWhatsAppChange,
            onInstagramChange = viewModel::onInstagramChange,
            onSiteChange = viewModel::onSiteChange,
            onToggleVisibility = viewModel::toggleVisibility,
            onCouponChange = viewModel::updateCoupon,
            onAddCoupon = viewModel::addCoupon,
            onRemoveCoupon = viewModel::removeCoupon,
            onSave = viewModel::save,
            onBackClick = { navController.navigateUp() },
        )
    }
    composable(
        route = AppRoute.CompanyHistory,
        arguments = listOf(navArgument("companyId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val companyId = backStackEntry.arguments?.getString("companyId").orEmpty()
        val viewModel: CompanyHistoryViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(companyId) { viewModel.load(companyId) }
        CompanyHistoryScreen(
            state = state,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onBackClick = { navController.navigateUp() },
        )
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
            turma != null -> AlbumTurmaScreen(
                turma = turma,
                state = state,
                onBackClick = { navController.navigateUp() },
                onOpenProfile = { userId ->
                    navController.navigate(AppRoute.profileDetail(userId)) { launchSingleTop = true }
                },
            )
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
        UserOrdersHubScreen(
            onTabClick = { tab ->
                navController.navigate(AppRoute.ordersByType(tab.slug)) { launchSingleTop = true }
            },
            onBackClick = { navController.navigateUp() },
        )
    }

    composable(
        AppRoute.OrdersByType,
        listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("status") {
                type = NavType.StringType
                defaultValue = "pendentes"
            },
        ),
    ) { entry ->
        val viewModel: UserOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val tab = UserOrderTab.fromSlug(entry.arguments?.getString("type"))
        val initialStatus = UserOrderStatus.fromSlug(entry.arguments?.getString("status"))

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id, tab) {
            viewModel.selectStatus(initialStatus)
            viewModel.load(authState.session, tab)
        }

        UserOrdersByTabScreen(
            state = state,
            onStatusClick = viewModel::selectStatus,
            onOrderClick = { order ->
                navController.navigate(
                    AppRoute.userOrderDetail(tab.slug, order.status.slug, order.id),
                ) { launchSingleTop = true }
            },
            onPageChange = viewModel::goToPage,
            onBackClick = { navController.navigateUp() },
        )
    }

    composable(
        AppRoute.UserOrderDetail,
        listOf(
            navArgument("tab") { type = NavType.StringType },
            navArgument("status") { type = NavType.StringType },
            navArgument("orderId") { type = NavType.StringType },
        ),
    ) { entry ->
        val viewModel: UserOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val tab = UserOrderTab.fromSlug(entry.arguments?.getString("tab"))
        val statusSlug = entry.arguments?.getString("status").orEmpty()
        val orderId = entry.arguments?.getString("orderId").orEmpty()
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id, tab, orderId) {
            viewModel.load(authState.session, tab)
        }

        val order = state.orderById(orderId)
        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando pedido")
            order == null -> PremiumEmptyState(
                title = "Pedido não encontrado",
                subtitle = "Não encontrei esse pedido no seu histórico real.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )

            else -> UserOrderDetailScreen(
                state = state,
                order = order,
                onBackClick = { navController.navigateUp() },
                onCopyPixClick = { key -> clipboardManager.setText(AnnotatedString(key)) },
                onSendReceiptClick = { url -> context.openExternalUrl(url) },
                onTicketClick = { ticket ->
                    navController.navigate(
                        AppRoute.userOrderTicketDetail(tab.slug, statusSlug, orderId, ticket.token),
                    ) { launchSingleTop = true }
                },
            )
        }
    }

    composable(
        AppRoute.UserOrderTicketDetail,
        listOf(
            navArgument("tab") { type = NavType.StringType },
            navArgument("status") { type = NavType.StringType },
            navArgument("orderId") { type = NavType.StringType },
            navArgument("ticketToken") { type = NavType.StringType },
        ),
    ) { entry ->
        val viewModel: UserOrdersViewModel = viewModel()
        val state by viewModel.uiState.collectAsState()
        val tab = UserOrderTab.fromSlug(entry.arguments?.getString("tab"))
        val orderId = entry.arguments?.getString("orderId").orEmpty()
        val ticketToken = entry.arguments?.getString("ticketToken").orEmpty()
        val context = LocalContext.current

        LaunchedEffect(authState.session.tenant?.id, authState.session.user?.id, tab, orderId) {
            viewModel.load(authState.session, tab)
        }

        val order = state.orderById(orderId)
        val ticket = order?.paymentConfig?.ticketEntries?.firstOrNull { it.token == ticketToken }
        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando ingresso")
            order == null || ticket == null -> PremiumEmptyState(
                title = "Ingresso não encontrado",
                subtitle = "Não encontrei esse ingresso para este pedido.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )

            else -> UserOrderTicketDetailScreen(
                state = state,
                order = order,
                ticket = ticket,
                onBackClick = { navController.navigateUp() },
                onOpenQrClick = {
                    context.openExternalUrl(
                        buildEventTicketPublicUrl(
                            tenantSlug = authState.session.tenant?.slug.orEmpty(),
                            orderId = order.id,
                            ticketToken = ticket.token,
                        ),
                    )
                },
            )
        }
    }
}

/** Mesmo caminho público de `buildEventTicketPublicPath` do web. */
private fun buildEventTicketPublicUrl(
    tenantSlug: String,
    orderId: String,
    ticketToken: String,
): String {
    val base = "https://usc-atleticas.vercel.app"
    val prefix = tenantSlug.trim().takeIf(String::isNotBlank)?.let { "/${Uri.encode(it)}" }.orEmpty()
    return "$base$prefix/public/ingressos/${Uri.encode(orderId)}/${Uri.encode(ticketToken)}"
}

private fun android.content.Context.openExternalUrl(url: String) {
    runCatching {
        startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

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
