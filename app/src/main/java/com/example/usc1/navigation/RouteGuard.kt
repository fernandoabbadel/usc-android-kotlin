package com.example.usc1.navigation

import com.example.usc1.core.session.AuthStatus
import com.example.usc1.ui.auth.AuthUiState

object RouteGuard {
    fun destinationFor(state: AuthUiState): String? {
        if (state.isLoading || state.status == AuthStatus.Loading) return null

        return when (state.status) {
            AuthStatus.Loading -> null
            AuthStatus.Unauthenticated -> AppRoute.Login
            AuthStatus.Authenticated -> AppRoute.Dashboard
            AuthStatus.WaitingApproval -> AppRoute.WaitingApproval
            AuthStatus.InviteRequired -> AppRoute.InviteRequired
            AuthStatus.Banned -> AppRoute.BannedUser
        }
    }
}
