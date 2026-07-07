package com.example.usc1.domain.model

import com.example.usc1.core.roles.UserRole

data class AppModule(
    val key: AppModuleKey,
    val title: String,
    val description: String,
    val route: String,
    val phase: AppModulePhase,
    val requiresAuthentication: Boolean = true,
    val allowedRoles: Set<UserRole> = emptySet(),
) {
    val remoteKey: String = key.remoteKey
}

enum class AppModulePhase(val label: String) {
    EssentialV1("Android v1"),
    ImportantV2("Android v2"),
    WebAdminOnly("Somente web/admin"),
    NotNow("Não migrar agora"),
}

enum class AppModuleKey(val remoteKey: String) {
    Auth("auth"),
    Dashboard("dashboard"),
    Profile("perfil"),
    Settings("configuracoes"),
    Orders("pedidos"),
    Store("loja"),
    Events("eventos"),
    Plans("planos"),
    MembershipCard("carteirinha"),
    Training("treinos"),
    Gym("gym_rats"),
    Partners("parceiros"),
    Company("empresa"),
    Community("comunidade"),
    Leagues("ligas"),
    Directory("diretorio"),
    Commissions("comissoes"),
    Tenant("tenant"),
    MiniVendor("mini_vendor"),
    Scanner("scanner"),
    Guide("guia"),
    Album("album"),
    Ranking("ranking"),
    Games("arena_games"),
    Boardround("sharkround"),
    Achievements("conquistas"),
    Fidelity("fidelidade"),
    Legal("legal"),
    History("historico"),
    AdminPanel("admin"),
    MasterPanel("master"),
}
