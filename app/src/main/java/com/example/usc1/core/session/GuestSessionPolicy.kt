package com.example.usc1.core.session

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus

object GuestSessionPolicy {
    fun attachTenant(
        session: UserSession,
        tenant: TenantContext,
    ): UserSession {
        val guest = requireNotNull(session.user) {
            "Inicie a sessão de visitante antes de selecionar uma atlética."
        }
        require(session.status == AuthStatus.Authenticated) {
            "A sessão de visitante não está ativa."
        }
        require(guest.role == UserRole.Guest && guest.id.startsWith("guest_virtual_")) {
            "Somente visitantes podem usar o diretório público para trocar de atlética."
        }

        val cleanTenantId = tenant.id.trim()
        val cleanTenantSlug = tenant.slug.trim().lowercase()
        require(cleanTenantId.isNotBlank() && cleanTenantSlug.isNotBlank()) {
            "A atlética selecionada é inválida."
        }

        return session.copy(
            tenant = tenant.copy(
                id = cleanTenantId,
                slug = cleanTenantSlug,
                membershipStatus = TenantMembershipStatus.Unlinked,
            ),
            status = AuthStatus.Authenticated,
        )
    }
}
