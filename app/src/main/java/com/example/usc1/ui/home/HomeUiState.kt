package com.example.usc1.ui.home

import com.example.usc1.core.session.UserSession
import com.example.usc1.core.permissions.PermissionPolicy
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.domain.model.HomeDashboardBundle
import com.example.usc1.navigation.AppRoute

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "",
    val tenantName: String = "",
    val planName: String = "",
    val accountStatus: String = "",
    val membershipCode: String = "",
    val userAvatarUrl: String? = null,
    val tenantLogoUrl: String? = null,
    val tenantContext: TenantContext? = null,
    val className: String = "",
    val canManageTenant: Boolean = false,
    val canUseAdministrativeScanner: Boolean = false,
    val isGuest: Boolean = false,
    val dashboard: HomeDashboardBundle = HomeDashboardBundle(),
    val quickActions: List<QuickActionUiModel> = defaultQuickActions,
    val upcomingEvents: List<HomeEventUiModel> = emptyList(),
    val orderSummary: HomeOrderSummaryUiModel = HomeOrderSummaryUiModel(),
    val mainModules: List<HomeModuleUiModel> = defaultModules,
) {
    companion object {
        fun loading() = HomeUiState(isLoading = true)

        fun error() = HomeUiState(
            errorMessage = "Não foi possível carregar a Home.",
            quickActions = emptyList(),
            upcomingEvents = emptyList(),
            mainModules = emptyList(),
        )
    }
}

fun HomeUiState.withSession(session: UserSession): HomeUiState {
    val user = session.user
    val tenant = session.tenant
    return copy(
        userName = user?.name.orEmpty(),
        tenantName = tenant?.name.orEmpty(),
        planName = dashboard.profile.planName,
        accountStatus = user?.status?.remoteValue.orEmpty(),
        membershipCode = user?.id.orEmpty().take(8).uppercase(),
        userAvatarUrl = dashboard.profile.avatarUrl ?: user?.avatarUrl,
        tenantLogoUrl = tenant?.logoUrl,
        tenantContext = tenant,
        className = dashboard.profile.className,
        canManageTenant = user?.role?.canManageTenant == true,
        canUseAdministrativeScanner = user?.role?.let { it in PermissionPolicy.scannerRoles } == true,
        isGuest = user?.id?.startsWith("guest_virtual_") == true,
    )
}

data class QuickActionUiModel(
    val title: String,
    val subtitle: String,
    val route: String,
    val kind: QuickActionKind,
)

enum class QuickActionKind {
    MembershipCard,
    Events,
    Store,
    Training,
    Community,
    Leagues,
    Profile,
}

data class HomeEventUiModel(
    val title: String,
    val dateLabel: String,
    val location: String,
    val status: String,
)

data class HomeOrderSummaryUiModel(
    val pendingOrders: Int = 0,
    val activeTickets: Int = 0,
    val lastOrderLabel: String = "",
)

data class HomeModuleUiModel(
    val title: String,
    val description: String,
    val route: String,
    val kind: QuickActionKind,
)

private val defaultQuickActions = listOf(
    QuickActionUiModel("Carteirinha", "Status e QR", AppRoute.MembershipCard, QuickActionKind.MembershipCard),
    QuickActionUiModel("Eventos", "Ingressos e festas", AppRoute.Events, QuickActionKind.Events),
    QuickActionUiModel("Loja", "Produtos da atlética", AppRoute.Store, QuickActionKind.Store),
    QuickActionUiModel("Treinos", "Agenda e presença", AppRoute.Training, QuickActionKind.Training),
    QuickActionUiModel("Comunidade", "Feed da turma", AppRoute.Community, QuickActionKind.Community),
    QuickActionUiModel("Perfil", "Dados e histórico", AppRoute.Profile, QuickActionKind.Profile),
)

private val defaultModules = listOf(
    HomeModuleUiModel("Ligas", "Membros, agenda, loja e eventos das ligas.", AppRoute.Leagues, QuickActionKind.Leagues),
    HomeModuleUiModel("Comunidade", "Publicações, comentários e interações.", AppRoute.Community, QuickActionKind.Community),
    HomeModuleUiModel("Loja", "Produtos, categorias e pedidos.", AppRoute.Store, QuickActionKind.Store),
    HomeModuleUiModel("Treinos", "Frequência e check-in de treino.", AppRoute.Training, QuickActionKind.Training),
    HomeModuleUiModel("Planos", "Plano ativo, adesões e benefícios.", AppRoute.Plans, QuickActionKind.Profile),
    HomeModuleUiModel("Parceiros", "Empresas, cupons e descontos.", AppRoute.Partners, QuickActionKind.Community),
)
