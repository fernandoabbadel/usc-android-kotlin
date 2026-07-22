package com.example.usc1.navigation

import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.roles.UserRole
import com.example.usc1.ui.auth.AuthUiState

object RouteGuard {
    fun destinationFor(state: AuthUiState): String? {
        if (state.isLoading || state.status == AuthStatus.Loading) return null

        return when (state.status) {
            AuthStatus.Loading -> null
            AuthStatus.Unauthenticated -> AppRoute.Login
            AuthStatus.Authenticated -> {
                if (state.session.user?.role == UserRole.Guest && state.session.tenant == null) {
                    AppRoute.Tenant
                } else {
                    AppRoute.Dashboard
                }
            }
            AuthStatus.WaitingApproval -> AppRoute.WaitingApproval
            AuthStatus.InviteRequired -> AppRoute.InviteRequired
            AuthStatus.Banned -> AppRoute.BannedUser
        }
    }

    fun redirectFor(
        state: AuthUiState,
        currentRoute: String?,
    ): String? {
        if (state.isLoading || state.status == AuthStatus.Loading) return null

        val route = currentRoute?.substringBefore('?')
        return when (state.status) {
            AuthStatus.Loading -> null
            AuthStatus.Unauthenticated -> AppRoute.Login.takeUnless { route == it }
            AuthStatus.WaitingApproval -> AppRoute.WaitingApproval.takeUnless { route == it }
            AuthStatus.InviteRequired -> AppRoute.InviteRequired.takeUnless { route == it }
            AuthStatus.Banned -> AppRoute.BannedUser.takeUnless { route == it }
            AuthStatus.Authenticated -> authenticatedRedirect(state, route)
        }
    }

    fun isGuestAllowedRoute(route: String?): Boolean {
        val normalizedRoute = route?.substringBefore('?') ?: return false
        return normalizedRoute in GuestAllowedRoutes
    }

    private fun authenticatedRedirect(
        state: AuthUiState,
        currentRoute: String?,
    ): String? {
        val user = state.session.user ?: return AppRoute.Login
        if (user.role != UserRole.Guest) {
            return if (currentRoute == null || currentRoute in AuthenticationEntryRoutes) {
                AppRoute.Dashboard
            } else {
                null
            }
        }

        if (state.session.tenant == null) {
            return AppRoute.Tenant.takeUnless { currentRoute == it }
        }

        return when {
            currentRoute == null -> AppRoute.Dashboard
            currentRoute == AppRoute.Tenant -> AppRoute.Dashboard
            currentRoute in AuthenticationEntryRoutes -> AppRoute.Dashboard
            isGuestAllowedRoute(currentRoute) -> null
            else -> AppRoute.Dashboard
        }
    }

    private val AuthenticationEntryRoutes = setOf(
        AppRoute.Login,
        AppRoute.Register,
        AppRoute.WaitingApproval,
        AppRoute.InviteRequired,
        AppRoute.BannedUser,
    )

    private val GuestAllowedRoutes = setOf(
        AppRoute.Dashboard,
        AppRoute.Legal,
        AppRoute.Faq,
        AppRoute.ContactUsc,
        AppRoute.Terms,
        AppRoute.PrivacyLgpd,
        AppRoute.LgpdRequest,
        AppRoute.LegalDocument,
    )
}
