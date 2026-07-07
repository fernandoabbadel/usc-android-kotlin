package com.example.usc1.ui.settings

import com.example.usc1.navigation.AppRoute

data class SettingsUiState(
    val isLoading: Boolean = false,
    val userName: String = "Fernando USC",
    val userEmail: String = "membro@usc.app",
    val sections: List<SettingsSectionUiModel> = defaultSettingsSections,
)

data class SettingsSectionUiModel(
    val title: String,
    val items: List<SettingsItemUiModel>,
)

data class SettingsItemUiModel(
    val title: String,
    val description: String,
    val action: SettingsAction,
)

enum class SettingsAction(val route: String?) {
    Profile(AppRoute.Profile),
    Membership(AppRoute.MembershipCard),
    Security(AppRoute.AccountSecurity),
    Orders(AppRoute.OrdersHub),
    Tickets(AppRoute.EventTickets),
    Plans(AppRoute.PlanOrders),
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
    Support(AppRoute.Support),
    TermsPrivacy(AppRoute.Terms),
    Lgpd(AppRoute.PrivacyLgpd),
    SignOut(null),
}

private val defaultSettingsSections = listOf(
    SettingsSectionUiModel(
        title = "Conta",
        items = listOf(
            SettingsItemUiModel("Perfil", "Dados pessoais, curso, turma e avatar.", SettingsAction.Profile),
            SettingsItemUiModel("Segurança da conta", "Sessão, recuperação e proteção de acesso.", SettingsAction.Security),
            SettingsItemUiModel("Convites", "Links e solicitações de entrada na atlética.", SettingsAction.Invites),
        ),
    ),
    SettingsSectionUiModel(
        title = "Pedidos e benefícios",
        items = listOf(
            SettingsItemUiModel("Pedidos gerais", "Eventos, loja, planos e status de pagamento.", SettingsAction.Orders),
            SettingsItemUiModel("Ingressos", "Tickets ativos, QR Codes e transferências futuras.", SettingsAction.Tickets),
            SettingsItemUiModel("Pedidos Planos", "Plano ativo, adesões e benefícios.", SettingsAction.Plans),
        ),
    ),
    SettingsSectionUiModel(
        title = "Módulos USC",
        items = listOf(
            SettingsItemUiModel("Comunidade", "Feed, publicações e interações da atlética.", SettingsAction.Community),
            SettingsItemUiModel("Ligas", "Membros, agenda, eventos e loja das ligas.", SettingsAction.Leagues),
            SettingsItemUiModel("Diretório", "Central do diretório com agenda e membros.", SettingsAction.Directory),
            SettingsItemUiModel("Comissões", "Comissões, eventos, loja e operação.", SettingsAction.Commissions),
            SettingsItemUiModel("Atlética", "Tenant atual, logo, cores e módulos habilitados.", SettingsAction.Tenant),
            SettingsItemUiModel("Álbum", "Turmas, caça-calouro e ranking.", SettingsAction.Album),
            SettingsItemUiModel("Games", "Boardround, conquistas, fidelidade e ranking.", SettingsAction.Games),
        ),
    ),
    SettingsSectionUiModel(
        title = "Operação",
        items = listOf(
            SettingsItemUiModel("Mini-vendor", "Produtos, pedidos e financeiro simplificado.", SettingsAction.MiniVendor),
            SettingsItemUiModel("Modo vendas", "Menu do evento, retirada e venda assistida.", SettingsAction.SalesMode),
            SettingsItemUiModel("Scanner", "Check-in, festas, ingressos e retirada de produtos.", SettingsAction.Scanner),
        ),
    ),
    SettingsSectionUiModel(
        title = "Ajuda e privacidade",
        items = listOf(
            SettingsItemUiModel("Guia", "FAQ, suporte, contato USC e orientações.", SettingsAction.Guide),
            SettingsItemUiModel("Suporte", "Canais de contato e dúvidas frequentes.", SettingsAction.Support),
            SettingsItemUiModel("Termos e privacidade", "Documentos legais e aceites.", SettingsAction.TermsPrivacy),
            SettingsItemUiModel("Preferências LGPD", "Privacidade, consentimentos e solicitações.", SettingsAction.Lgpd),
            SettingsItemUiModel("Sair da conta", "Encerrar a sessão mockada atual.", SettingsAction.SignOut),
        ),
    ),
)
