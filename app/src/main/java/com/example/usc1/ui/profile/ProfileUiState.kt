package com.example.usc1.ui.profile

import com.example.usc1.core.session.UserSession

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profile: ProfileUserUiModel = ProfileUserUiModel(),
    val shortcuts: List<ProfileShortcutUiModel> = emptyList(),
)

data class ProfileUserUiModel(
    val name: String = "",
    val email: String = "",
    val initials: String = "",
    val course: String = "",
    val className: String = "",
    val tenantName: String = "",
    val role: String = "",
    val accountStatus: String = "",
    val activePlan: String = "",
    val memberSince: String = "",
)

data class ProfileShortcutUiModel(
    val title: String,
    val description: String,
    val route: String,
)

fun ProfileUiState.withSession(session: UserSession): ProfileUiState {
    val user = session.user ?: return copy(profile = ProfileUserUiModel())
    val name = user.name.ifBlank { user.email.substringBefore("@") }
    return copy(
        profile = ProfileUserUiModel(
            name = name,
            email = user.email,
            initials = name.initials(),
            tenantName = session.tenant?.name.orEmpty(),
            role = user.role.remoteValue,
            accountStatus = user.status.remoteValue,
        ),
    )
}

private fun String.initials(): String {
    return trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "US" }
}
