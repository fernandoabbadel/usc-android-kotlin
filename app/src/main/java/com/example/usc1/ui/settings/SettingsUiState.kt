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
    Security(AppRoute.AccountSecurity),
    Orders("orders"),
    Tickets("events"),
    Plans("plans"),
    MiniVendor("mini-vendor"),
    SalesMode("scanner"),
    Invites("auth"),
    Support("guide"),
    TermsPrivacy("legal"),
    Lgpd("legal"),
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
            SettingsItemUiModel("Pedidos", "Loja, planos e eventos em um histórico único.", SettingsAction.Orders),
            SettingsItemUiModel("Ingressos", "Tickets ativos, QR Codes e transferências futuras.", SettingsAction.Tickets),
            SettingsItemUiModel("Planos", "Plano ativo, adesões e benefícios.", SettingsAction.Plans),
        ),
    ),
    SettingsSectionUiModel(
        title = "Operação",
        items = listOf(
            SettingsItemUiModel("Mini-vendor", "Produtos, pedidos e financeiro simplificado.", SettingsAction.MiniVendor),
            SettingsItemUiModel("Modo vendas", "Atalho futuro para vendas e retirada.", SettingsAction.SalesMode),
        ),
    ),
    SettingsSectionUiModel(
        title = "Ajuda e privacidade",
        items = listOf(
            SettingsItemUiModel("Suporte", "Canais de contato e dúvidas frequentes.", SettingsAction.Support),
            SettingsItemUiModel("Termos e privacidade", "Documentos legais e aceites.", SettingsAction.TermsPrivacy),
            SettingsItemUiModel("Preferências LGPD", "Privacidade, consentimentos e solicitações.", SettingsAction.Lgpd),
            SettingsItemUiModel("Sair da conta", "Encerrar a sessão mockada atual.", SettingsAction.SignOut),
        ),
    ),
)
