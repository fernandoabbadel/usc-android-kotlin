package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminPermissionRole
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUsersFilters
import com.example.usc1.domain.model.AdminUsersLetterGroup
import com.example.usc1.domain.model.AdminUsersPage

data class AdminPermissionUsersUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val users: List<AdminUserListItem> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val filters: AdminUsersFilters = AdminUsersFilters(letterGroup = AdminUsersLetterGroup.AF),
    val pendingRoles: Map<String, String> = emptyMap(),
    val savingUserIds: Set<String> = emptySet(),
    val mutatingLeaderUserId: String? = null,
) {
    val pendingRolesCount: Int
        get() = pendingRoles.size

    val isSavingAnyRole: Boolean
        get() = savingUserIds.isNotEmpty()
}

fun AdminUsersPage.toPermissionUsersUiState(
    current: AdminPermissionUsersUiState,
    append: Boolean,
): AdminPermissionUsersUiState {
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
        pendingRoles = current.pendingRoles.filterKeys { userId ->
            mergedUsers.any { it.id == userId }
        },
    )
}

val adminPermissionRoles = AdminPermissionRole.entries.toList()

val adminPermissionUsersLetterFilters = listOf(
    AdminUsersLetterGroup.AF,
    AdminUsersLetterGroup.GK,
    AdminUsersLetterGroup.LQ,
    AdminUsersLetterGroup.RZ,
    AdminUsersLetterGroup.Todos,
)
