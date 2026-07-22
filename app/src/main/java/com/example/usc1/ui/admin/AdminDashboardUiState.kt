package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminDashboardActivityLog
import com.example.usc1.domain.model.AdminDashboardBundle
import com.example.usc1.domain.model.AdminDashboardRecentUser

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val tenantName: String = "",
    val tenantSigla: String = "",
    val stats: List<AdminStatUiModel> = emptyList(),
    val recentUsers: List<AdminRecentUserUiModel> = emptyList(),
    val recentActivity: List<AdminActivityUiModel> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && recentUsers.isEmpty() && recentActivity.isEmpty()
}

data class AdminStatUiModel(
    val title: String,
    val value: String,
    val trend: String,
    val kind: AdminStatKind,
)

enum class AdminStatKind {
    Users,
    Events,
    Sales,
    Engagement,
}

data class AdminRecentUserUiModel(
    val id: String,
    val name: String,
    val className: String,
    val role: String,
    val createdLabel: String,
)

data class AdminActivityUiModel(
    val id: String,
    val userName: String,
    val action: String,
    val resource: String,
    val timeLabel: String,
)

fun AdminDashboardBundle.toUiState(
    tenantName: String,
    tenantSigla: String,
): AdminDashboardUiState {
    return AdminDashboardUiState(
        isLoading = false,
        tenantName = tenantName,
        tenantSigla = tenantSigla,
        stats = listOf(
            AdminStatUiModel(
                title = "Total de Atletas",
                value = stats.totalUsers.toString(),
                trend = "+12% essa semana",
                kind = AdminStatKind.Users,
            ),
            AdminStatUiModel(
                title = "Eventos Criados",
                value = stats.totalEvents.toString(),
                trend = "3 ativos agora",
                kind = AdminStatKind.Events,
            ),
            AdminStatUiModel(
                title = "Vendas Loja",
                value = stats.totalSales?.let { "R$ $it" } ?: "R$ --",
                trend = "Meta batida!",
                kind = AdminStatKind.Sales,
            ),
            AdminStatUiModel(
                title = "Engajamento",
                value = "98.5%",
                trend = "Recorde histórico",
                kind = AdminStatKind.Engagement,
            ),
        ),
        recentUsers = recentUsers.map { it.toUiModel() },
        recentActivity = recentActivity.map { it.toUiModel() },
    )
}

private fun AdminDashboardRecentUser.toUiModel(): AdminRecentUserUiModel {
    return AdminRecentUserUiModel(
        id = id,
        name = name,
        className = className,
        role = role.ifBlank { "user" },
        createdLabel = if (createdAt.isNullOrBlank()) "Veterano" else "Novo",
    )
}

private fun AdminDashboardActivityLog.toUiModel(): AdminActivityUiModel {
    return AdminActivityUiModel(
        id = id,
        userName = userName,
        action = action,
        resource = resource,
        timeLabel = timestamp?.takeIf { it.isNotBlank() } ?: "Agora",
    )
}
