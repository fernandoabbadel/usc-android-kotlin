package com.example.usc1.ui.auth

import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.UserSession

data class AuthUiState(
    val session: UserSession = UserSession(),
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val status: AuthStatus = when {
        isLoading -> AuthStatus.Loading
        else -> session.status
    }

    val canSubmitLogin: Boolean
        get() = email.isNotBlank() && password.length >= 4 && !isLoading

    val canSubmitRegister: Boolean
        get() = fullName.isNotBlank() && email.isNotBlank() && !isLoading
}
