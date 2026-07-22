package com.example.usc1.ui.settings

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.navigation.AppRoute

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isAccountActionLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userInitials: String = "",
    val avatarUrl: String? = null,
    val classLabel: String = "",
    val classLogoUrl: String? = null,
    val tenantName: String = "",
    val tenantPalette: TenantPalette? = null,
    val roleLabel: String = "",
    val planLabel: String = "",
    val planColorKey: String = "zinc",
    val statusLabel: String = "",
    val isAccountActive: Boolean = true,
    val userIdLabel: String = "",
    val notificationsEnabled: Boolean = true,
    val invitePanel: SettingsInviteUiModel = SettingsInviteUiModel(),
    val sections: List<SettingsSectionUiModel> = defaultSettingsSections,
)

data class SettingsInviteUiModel(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val remainingToday: Int = 5,
    val generatedLink: String = "",
    val isCopied: Boolean = false,
) {
    val canCreate: Boolean
        get() = !isLoading && remainingToday > 0
}

data class SettingsSectionUiModel(
    val title: String,
    val items: List<SettingsItemUiModel>,
)

data class SettingsItemUiModel(
    val title: String,
    val description: String = "",
    val action: SettingsAction,
    val badge: String? = null,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
)

enum class SettingsAction(val route: String?) {
    Profile(AppRoute.Profile),
    Membership(AppRoute.MembershipCard),
    Security(AppRoute.AccountSecurity),
    Orders(AppRoute.OrdersHub),
    Tickets(AppRoute.EventTickets),
    Plans(AppRoute.Plans),
    Community(AppRoute.Community),
    Leagues(AppRoute.Leagues),
    Directory(AppRoute.Directory),
    Commissions(AppRoute.Commissions),
    Tenant(AppRoute.Tenant),
    Album(AppRoute.Album),
    Games(AppRoute.Games),
    Boardround(AppRoute.Boardround),
    Achievements(AppRoute.Achievements),
    Loyalty(AppRoute.Loyalty),
    MiniVendor(AppRoute.MiniVendor),
    SalesMode(AppRoute.SalesMode),
    Scanner(AppRoute.Scanner),
    Guide(AppRoute.Guide),
    Invites(AppRoute.Register),
    Mentorship(null),
    Notifications(null),
    Support(AppRoute.Support),
    TermsPrivacy(AppRoute.Terms),
    Lgpd(AppRoute.PrivacyLgpd),
    SignOut(null),
}

fun SettingsUiState.withSession(session: UserSession): SettingsUiState {
    val user = session.user
    val tenant = session.tenant
    val name = user?.name.orEmpty()
    val resolvedPlanLabel = user
        ?.let { it.planBadge.ifBlank { it.planName } }
        .orEmpty()
        .ifBlank { "Bicho Solto" }
    val canGenerateInvite = user != null &&
        tenant != null &&
        user.role != UserRole.Guest &&
        tenant.membershipStatus == TenantMembershipStatus.Approved

    return copy(
        userName = name,
        userEmail = user?.email.orEmpty(),
        userInitials = name.initials(),
        avatarUrl = user?.avatarUrl?.trim()?.takeIf(String::isNotBlank),
        classLabel = user?.classCode.orEmpty(),
        classLogoUrl = user?.classPhotoUrl?.trim()?.takeIf(String::isNotBlank),
        tenantName = tenant?.name.orEmpty(),
        tenantPalette = tenant?.palette,
        roleLabel = user?.role?.settingsLabel().orEmpty(),
        planLabel = resolvedPlanLabel,
        planColorKey = user?.planColorKey.orEmpty().ifBlank { "zinc" },
        statusLabel = user?.status?.remoteValue.orEmpty(),
        isAccountActive = user?.status == UserStatus.Ativo,
        userIdLabel = user?.id?.take(8)?.uppercase().orEmpty(),
        invitePanel = invitePanel.copy(isVisible = canGenerateInvite),
    )
}

internal fun String.initials(): String = trim()
    .split(" ")
    .filter(String::isNotBlank)
    .take(2)
    .joinToString("") { it.first().uppercase() }

internal fun UserRole.settingsLabel(): String = when (this) {
    UserRole.Guest -> "Visitante"
    UserRole.Visitante -> "Visitante"
    UserRole.User -> "Membro"
    UserRole.MiniVendor -> "Mini Vendor"
    UserRole.Treinador -> "Treinador"
    UserRole.Empresa -> "Parceiro"
    UserRole.AdminTreino -> "Admin Treino"
    UserRole.GestorLiga -> "Gestor de Liga"
    UserRole.GestorDiretorio -> "Gestor de Diretório"
    UserRole.GestorComissao -> "Gestor de Comissão"
    UserRole.AdminGeral -> "Administrador"
    UserRole.AdminGestor -> "Gestor"
    UserRole.MasterTenant -> "Master"
    UserRole.Master -> "Master Plataforma"
    UserRole.Vendas -> "Vendas"
}

internal val defaultSettingsSections = listOf(
    SettingsSectionUiModel(
        title = "Minha Conta",
        items = listOf(
            SettingsItemUiModel("Dados Pessoais", "Atualizar cadastro", SettingsAction.Profile),
            SettingsItemUiModel(
                title = "Meus Ingressos e Compras",
                description = "Acompanhar meus pedidos de compra (convites, ingressos, produtos, planos, etc...)",
                action = SettingsAction.Orders,
                badge = "Novo",
            ),
            SettingsItemUiModel("Planos da Atlética", "Ver níveis e benefícios", SettingsAction.Plans),
            SettingsItemUiModel("Meus Convites", "Tabela completa dos links gerados", SettingsAction.Invites),
            SettingsItemUiModel(
                title = "Apadrinhamento",
                description = "Aceitar convites e ver seu vínculo",
                action = SettingsAction.Mentorship,
            ),
            SettingsItemUiModel("Mini Vendor", "Cadastrar ou editar sua lojinha", SettingsAction.MiniVendor),
            SettingsItemUiModel(
                title = "Segurança & Senha",
                description = "Proteger conta",
                action = SettingsAction.Security,
                badge = "Bloqueado",
                isEnabled = false,
            ),
        ),
    ),
    SettingsSectionUiModel(
        title = "Preferências",
        items = listOf(
            SettingsItemUiModel("Notificações", action = SettingsAction.Notifications),
        ),
    ),
    SettingsSectionUiModel(
        title = "Suporte",
        items = listOf(
            SettingsItemUiModel("Denúncias & Ajuda", "Reportar problemas", SettingsAction.Support),
            SettingsItemUiModel("Termos e Privacidade", action = SettingsAction.TermsPrivacy),
        ),
    ),
)
