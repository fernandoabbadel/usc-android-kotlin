package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminArenaUser

data class AdminGamesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val users: List<AdminArenaUser> = emptyList(),
    val selectedUser: AdminArenaUser? = null,
    val searchTerm: String = "",
) {
    val filteredUsers: List<AdminArenaUser>
        get() {
            val term = searchTerm.trim().lowercase()
            if (term.isBlank()) return users
            return users.filter { it.name.lowercase().contains(term) }
        }
}
