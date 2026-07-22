package com.example.usc1.navigation

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.ui.auth.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteGuardTest {
    @Test
    fun `guest sem tenant é enviado ao diretório`() {
        val state = authenticatedState(role = UserRole.Guest, tenant = null)

        assertEquals(AppRoute.Tenant, RouteGuard.destinationFor(state))
        assertEquals(AppRoute.Tenant, RouteGuard.redirectFor(state, AppRoute.Login))
        assertNull(RouteGuard.redirectFor(state, AppRoute.Tenant))
    }

    @Test
    fun `guest com tenant entra no dashboard e não acessa rotas privadas`() {
        val state = authenticatedState(role = UserRole.Guest, tenant = tenant())

        assertEquals(AppRoute.Dashboard, RouteGuard.destinationFor(state))
        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Tenant))
        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Store))
        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Events))
        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Login))
        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Register))
        assertNull(RouteGuard.redirectFor(state, AppRoute.Dashboard))
        assertNull(RouteGuard.redirectFor(state, AppRoute.Legal))
    }

    @Test
    fun `membro autenticado preserva navegação privada`() {
        val state = authenticatedState(role = UserRole.User, tenant = tenant())

        assertEquals(AppRoute.Dashboard, RouteGuard.redirectFor(state, AppRoute.Login))
        assertNull(RouteGuard.redirectFor(state, AppRoute.Store))
        assertNull(RouteGuard.redirectFor(state, AppRoute.Events))
    }

    @Test
    fun `estados de autenticação continuam protegidos`() {
        assertEquals(
            AppRoute.Login,
            RouteGuard.redirectFor(AuthUiState(), AppRoute.Dashboard),
        )
        assertEquals(
            AppRoute.WaitingApproval,
            RouteGuard.redirectFor(
                AuthUiState(session = UserSession(status = AuthStatus.WaitingApproval)),
                AppRoute.Dashboard,
            ),
        )
        assertEquals(
            AppRoute.InviteRequired,
            RouteGuard.redirectFor(
                AuthUiState(session = UserSession(status = AuthStatus.InviteRequired)),
                AppRoute.Dashboard,
            ),
        )
        assertEquals(
            AppRoute.BannedUser,
            RouteGuard.redirectFor(
                AuthUiState(session = UserSession(status = AuthStatus.Banned)),
                AppRoute.Dashboard,
            ),
        )
        assertNull(RouteGuard.redirectFor(AuthUiState(isLoading = true), AppRoute.Dashboard))
    }

    private fun authenticatedState(
        role: UserRole,
        tenant: TenantContext?,
    ) = AuthUiState(
        session = UserSession(
            user = AuthUser(
                id = if (role == UserRole.Guest) "guest_virtual_test" else "member-1",
                name = "Usuário",
                email = "usuario@usc.app",
                role = role,
            ),
            tenant = tenant,
            status = AuthStatus.Authenticated,
        ),
    )

    private fun tenant() = TenantContext(
        id = "tenant-1",
        slug = "atletica",
        name = "Atlética",
    )
}
