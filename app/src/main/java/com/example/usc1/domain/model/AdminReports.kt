package com.example.usc1.domain.model

enum class AdminReportsSection(
    val routeName: String,
    val title: String,
    val subtitle: String,
    val emptyText: String,
) {
    Banned(
        routeName = "banidos",
        title = "Banidos",
        subtitle = "Recursos e devolutivas do fluxo de bloqueio",
        emptyText = "Sem recursos de banidos.",
    ),
    Community(
        routeName = "comunidade",
        title = "Comunidade",
        subtitle = "Mensagens denunciadas",
        emptyText = "Sem denúncias de comunidade.",
    ),
    Gym(
        routeName = "gym",
        title = "Gym",
        subtitle = "Denúncias do módulo de treino e check-in",
        emptyText = "Sem denúncias de gym no período.",
    ),
    Support(
        routeName = "suporte",
        title = "Suporte",
        subtitle = "Integrado com /configuracoes/suporte",
        emptyText = "Sem chamados de suporte.",
    );
}

enum class AdminReportStatus(val remoteValue: String, val label: String) {
    Pending("pending", "pendente"),
    Resolved("resolved", "resolvida");

    companion object {
        fun fromRemote(value: String?): AdminReportStatus {
            return when (value?.trim()?.lowercase()) {
                "resolved", "resolvida" -> Resolved
                else -> Pending
            }
        }
    }
}

enum class AdminReportOrigin(val tableName: String) {
    BannedAppeals("banned_appeals"),
    SupportRequests("support_requests"),
    CommunityReports("denuncias"),
}

data class AdminReportItem(
    val id: String,
    val section: AdminReportsSection,
    val origin: AdminReportOrigin,
    val author: String,
    val reason: String,
    val description: String,
    val dateLabel: String,
    val createdAtMs: Long,
    val status: AdminReportStatus,
    val adminResponse: String,
    val reporterId: String,
    val targetId: String,
    val targetType: String,
) {
    val isResolved: Boolean
        get() = status == AdminReportStatus.Resolved
}

data class AdminReportsPage(
    val tenantId: String,
    val section: AdminReportsSection,
    val rows: List<AdminReportItem>,
)
