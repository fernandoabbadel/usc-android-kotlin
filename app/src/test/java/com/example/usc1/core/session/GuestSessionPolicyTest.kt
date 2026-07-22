package com.example.usc1.core.session

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class GuestSessionPolicyTest {
    @Test
    fun `anexa tenant validado e preserva identidade guest`() {
        val guest = guestUser()
        val session = UserSession(
            user = guest,
            status = AuthStatus.Authenticated,
        )

        val result = GuestSessionPolicy.attachTenant(
            session = session,
            tenant = TenantContext(
                id = " 9b2a5d0f ",
                slug = " AAAKN ",
                name = "AAAKN",
                membershipStatus = TenantMembershipStatus.Approved,
            ),
        )

        assertSame(guest, result.user)
        assertEquals(AuthStatus.Authenticated, result.status)
        assertEquals("9b2a5d0f", result.tenant?.id)
        assertEquals("aaakn", result.tenant?.slug)
        assertEquals(TenantMembershipStatus.Unlinked, result.tenant?.membershipStatus)
    }

    @Test
    fun `rejeita usuário que não é guest virtual`() {
        val memberSession = UserSession(
            user = guestUser().copy(id = "member-1", role = UserRole.User),
            status = AuthStatus.Authenticated,
        )

        assertThrows(IllegalArgumentException::class.java) {
            GuestSessionPolicy.attachTenant(memberSession, validTenant())
        }
    }

    @Test
    fun `rejeita guest cuja sessão não está ativa`() {
        val blockedSession = UserSession(
            user = guestUser(),
            status = AuthStatus.Banned,
        )

        assertThrows(IllegalArgumentException::class.java) {
            GuestSessionPolicy.attachTenant(blockedSession, validTenant())
        }
    }

    @Test
    fun `rejeita tenant sem identidade real`() {
        val guestSession = UserSession(
            user = guestUser(),
            status = AuthStatus.Authenticated,
        )

        assertThrows(IllegalArgumentException::class.java) {
            GuestSessionPolicy.attachTenant(
                guestSession,
                validTenant().copy(id = "", slug = ""),
            )
        }
    }

    private fun guestUser() = AuthUser(
        id = "guest_virtual_test",
        name = "Visitante USC",
        email = "visitante@usc.app",
        role = UserRole.Guest,
    )

    private fun validTenant() = TenantContext(
        id = "tenant-1",
        slug = "atletica",
        name = "Atlética",
    )
}
