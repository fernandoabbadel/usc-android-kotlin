package com.example.usc1.ui.collectives

import com.example.usc1.R

enum class CollectiveKind(val label: String) {
    League("Liga"),
    Directory("Diretório"),
    Commission("Comissão"),
}

data class CollectiveMember(
    val name: String,
    val role: String,
    val status: String,
)

data class CollectiveAgendaItem(
    val title: String,
    val dateLabel: String,
    val place: String,
)

data class CollectiveStoreItem(
    val name: String,
    val priceLabel: String,
    val status: String,
)

data class CollectiveEvent(
    val title: String,
    val dateLabel: String,
    val status: String,
)

data class CollectiveGroup(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val kind: CollectiveKind,
    val status: String,
    val memberCount: Int,
    val imageRes: Int,
    val accentName: String,
    val members: List<CollectiveMember>,
    val agenda: List<CollectiveAgendaItem>,
    val store: List<CollectiveStoreItem>,
    val events: List<CollectiveEvent>,
)

data class LeagueUiState(
    val leagues: List<CollectiveGroup> = CollectiveMockData.leagues,
)

data class DirectoryUiState(
    val directories: List<CollectiveGroup> = CollectiveMockData.directories,
)

data class CommissionUiState(
    val commissions: List<CollectiveGroup> = CollectiveMockData.commissions,
)

object CollectiveMockData {
    private val defaultMembers = listOf(
        CollectiveMember("Ana Costa", "Presidência", "Ativa"),
        CollectiveMember("Fernando USC", "Operação", "Ativo"),
        CollectiveMember("Lívia Martins", "Comunicação", "Ativa"),
    )

    private val defaultAgenda = listOf(
        CollectiveAgendaItem("Reunião de alinhamento", "Hoje • 19:00", "Sala USC"),
        CollectiveAgendaItem("Ação com calouros", "Qui • 18:30", "Bloco B"),
        CollectiveAgendaItem("Entrega de materiais", "Sáb • 10:00", "Sede AAAKN"),
    )

    private val defaultStore = listOf(
        CollectiveStoreItem("Camiseta oficial", "R$ 79,90", "Disponível"),
        CollectiveStoreItem("Adesivo premium", "R$ 9,90", "Últimas unidades"),
        CollectiveStoreItem("Kit evento", "R$ 34,90", "Pré-venda"),
    )

    private val defaultEvents = listOf(
        CollectiveEvent("Aulão Solidário", "12 JUL", "Inscrições abertas"),
        CollectiveEvent("Board Round interno", "18 JUL", "Em breve"),
    )

    val leagues = listOf(
        CollectiveGroup(
            id = "liga-medicina",
            name = "Liga Medicina Esportiva",
            subtitle = "Performance e saúde",
            description = "Liga acadêmica com agenda, membros, eventos e loja própria no ecossistema USC.",
            kind = CollectiveKind.League,
            status = "Ativa",
            memberCount = 42,
            imageRes = R.drawable.battle_forest,
            accentName = "Neon",
            members = defaultMembers,
            agenda = defaultAgenda,
            store = defaultStore,
            events = defaultEvents,
        ),
        CollectiveGroup(
            id = "liga-games",
            name = "Liga Games & Board",
            subtitle = "Arena e ranking",
            description = "Organiza desafios, boardround e rankings internos da comunidade.",
            kind = CollectiveKind.League,
            status = "Destaque",
            memberCount = 28,
            imageRes = R.drawable.logo_platform_web,
            accentName = "Dourado",
            members = defaultMembers,
            agenda = defaultAgenda,
            store = defaultStore,
            events = defaultEvents,
        ),
    )

    val directories = listOf(
        CollectiveGroup(
            id = "diretorio-atletica",
            name = "Diretório Atlético",
            subtitle = "Gestão e calendário",
            description = "Diretório com membros, agenda, eventos e produtos institucionais.",
            kind = CollectiveKind.Directory,
            status = "Ativo",
            memberCount = 18,
            imageRes = R.drawable.logo_usc_wide,
            accentName = "Azul",
            members = defaultMembers,
            agenda = defaultAgenda,
            store = defaultStore,
            events = defaultEvents,
        ),
    )

    val commissions = listOf(
        CollectiveGroup(
            id = "comissao-eventos",
            name = "Comissão de Eventos",
            subtitle = "Operação e vendas",
            description = "Comissão responsável por eventos, produtos, agenda operacional e pedidos.",
            kind = CollectiveKind.Commission,
            status = "Operando",
            memberCount = 24,
            imageRes = R.drawable.carteirinha_bg,
            accentName = "Âmbar",
            members = defaultMembers,
            agenda = defaultAgenda,
            store = defaultStore,
            events = defaultEvents,
        ),
        CollectiveGroup(
            id = "comissao-comunicacao",
            name = "Comissão de Comunicação",
            subtitle = "Conteúdo e comunidade",
            description = "Conteúdo, cobertura, comunidade e identidade visual da atlética.",
            kind = CollectiveKind.Commission,
            status = "Ativa",
            memberCount = 16,
            imageRes = R.drawable.logo_aaakn,
            accentName = "Neon",
            members = defaultMembers,
            agenda = defaultAgenda,
            store = defaultStore,
            events = defaultEvents,
        ),
    )

    fun leagueById(id: String): CollectiveGroup =
        leagues.firstOrNull { it.id == id } ?: leagues.first()

    fun directoryById(id: String): CollectiveGroup =
        directories.firstOrNull { it.id == id } ?: directories.first()

    fun commissionById(id: String): CollectiveGroup =
        commissions.firstOrNull { it.id == id } ?: commissions.first()
}
