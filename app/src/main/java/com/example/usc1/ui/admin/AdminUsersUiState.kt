package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUsersFilters
import com.example.usc1.domain.model.AdminUsersLetterGroup
import com.example.usc1.domain.model.AdminUsersPage

data class AdminUsersUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val users: List<AdminUserListItem> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val filters: AdminUsersFilters = AdminUsersFilters(),
    val pendingDeleteUser: AdminUserListItem? = null,
    val mutatingUserId: String? = null,
) {
    val visibleUsers: List<AdminUserListItem>
        get() = users.filter { user ->
            filters.plan == AdminUserPlan.Todos || user.plano == filters.plan
        }

    val total: Int
        get() = users.size

    val active: Int
        get() = users.count { it.status == AdminUserStatus.Ativo }

    val blocked: Int
        get() = users.count { it.status == AdminUserStatus.Bloqueado }
}

fun AdminUsersPage.toUiState(
    current: AdminUsersUiState,
    append: Boolean,
): AdminUsersUiState {
    val mergedUsers = if (append) {
        val known = current.users.map { it.id }.toMutableSet()
        current.users + users.filter { known.add(it.id) }
    } else {
        users
    }
    return current.copy(
        isLoading = false,
        isLoadingMore = false,
        errorMessage = null,
        users = mergedUsers,
        hasMore = hasMore,
        nextCursor = nextCursor,
    )
}

val adminUserPlanFilters = listOf(
    AdminUserPlan.Todos,
    AdminUserPlan.Lenda,
    AdminUserPlan.Atleta,
    AdminUserPlan.Cardume,
    AdminUserPlan.Bicho,
)

val adminUsersLetterFilters = listOf(
    AdminUsersLetterGroup.Todos,
    AdminUsersLetterGroup.AF,
    AdminUsersLetterGroup.GK,
    AdminUsersLetterGroup.LQ,
    AdminUsersLetterGroup.RZ,
)
