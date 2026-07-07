package com.example.usc1.ui.home

import com.example.usc1.navigation.AppRoute

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "Fernando USC",
    val tenantName: String = "AAAKN USC",
    val planName: String = "Cardume Livre",
    val accountStatus: String = "Membro ativo",
    val membershipCode: String = "USC-2026-042",
    val quickActions: List<QuickActionUiModel> = defaultQuickActions,
    val upcomingEvents: List<HomeEventUiModel> = defaultEvents,
    val orderSummary: HomeOrderSummaryUiModel = HomeOrderSummaryUiModel(),
    val mainModules: List<HomeModuleUiModel> = defaultModules,
) {
    companion object {
        fun loading() = HomeUiState(isLoading = true)

        fun error() = HomeUiState(
            errorMessage = "Não foi possível carregar a Home mockada.",
            quickActions = emptyList(),
            upcomingEvents = emptyList(),
            mainModules = emptyList(),
        )
    }
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
    val pendingOrders: Int = 2,
    val activeTickets: Int = 3,
    val lastOrderLabel: String = "Ingresso Intermed 2026 aguardando aprovação",
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
    QuickActionUiModel("Loja", "Produtos da atlética", "store", QuickActionKind.Store),
    QuickActionUiModel("Treinos", "Agenda e presença", "training", QuickActionKind.Training),
    QuickActionUiModel("Comunidade", "Feed da turma", "community", QuickActionKind.Community),
    QuickActionUiModel("Perfil", "Dados e histórico", AppRoute.Profile, QuickActionKind.Profile),
)

private val defaultEvents = listOf(
    HomeEventUiModel(
        title = "Intermed USC",
        dateLabel = "Sábado, 18:00",
        location = "Ginásio principal",
        status = "Vendas abertas",
    ),
    HomeEventUiModel(
        title = "Treino aberto",
        dateLabel = "Terça, 07:30",
        location = "Quadra externa",
        status = "Confirmar presença",
    ),
)

private val defaultModules = listOf(
    HomeModuleUiModel("Ligas", "Membros, agenda, loja e eventos das ligas.", "leagues", QuickActionKind.Leagues),
    HomeModuleUiModel("Comunidade", "Publicações, comentários e interações.", "community", QuickActionKind.Community),
    HomeModuleUiModel("Loja", "Produtos, categorias e pedidos.", "store", QuickActionKind.Store),
    HomeModuleUiModel("Treinos", "Frequência e check-in de treino.", "training", QuickActionKind.Training),
)
