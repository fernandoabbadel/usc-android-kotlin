package com.example.usc1.ui.profile

import com.example.usc1.navigation.AppRoute

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profile: ProfileUserUiModel = ProfileUserUiModel(),
    val shortcuts: List<ProfileShortcutUiModel> = defaultProfileShortcuts,
)

data class ProfileUserUiModel(
    val name: String = "Fernando USC",
    val email: String = "membro@usc.app",
    val initials: String = "FU",
    val course: String = "Medicina",
    val className: String = "T9",
    val tenantName: String = "AAAKN USC",
    val role: String = "Membro",
    val accountStatus: String = "Ativo",
    val activePlan: String = "Cardume Livre",
    val memberSince: String = "2026",
)

data class ProfileShortcutUiModel(
    val title: String,
    val description: String,
    val route: String,
)

private val defaultProfileShortcuts = listOf(
    ProfileShortcutUiModel("Pedidos da Loja", "Produtos, retirada e status", AppRoute.StoreOrders),
    ProfileShortcutUiModel("Pedidos Planos", "Adesões e histórico", AppRoute.PlanOrders),
    ProfileShortcutUiModel("Ingressos", "QR Codes e histórico", AppRoute.EventTickets),
    ProfileShortcutUiModel("Carteirinha", "Status e validação", AppRoute.MembershipCard),
    ProfileShortcutUiModel("Segurança", "Sessão e recuperação", AppRoute.AccountSecurity),
    ProfileShortcutUiModel("Configurações", "Preferências e suporte", AppRoute.Settings),
)
