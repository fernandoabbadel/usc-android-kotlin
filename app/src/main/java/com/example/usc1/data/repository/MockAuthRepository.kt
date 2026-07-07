package com.example.usc1.data.repository

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepository : AuthRepository {
    private val _session = MutableStateFlow(UserSession())
    override val session: StateFlow<UserSession> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): UserSession {
        delay(MockDelayMillis)
        val normalizedEmail = email.trim().lowercase()
        val nextSession = when {
            normalizedEmail.contains("ban") -> bannedSession(email)
            normalizedEmail.contains("convite") -> inviteRequiredSession(email)
            normalizedEmail.contains("pend") -> waitingApprovalSession(email)
            else -> authenticatedSession(email)
        }
        _session.value = nextSession
        return nextSession
    }

    override suspend fun register(
        fullName: String,
        email: String,
        inviteCode: String?,
    ): UserSession {
        delay(MockDelayMillis)
        val nextSession = waitingApprovalSession(
            email = email,
            name = fullName.ifBlank { "Novo membro USC" },
        )
        _session.value = nextSession
        return nextSession
    }

    override suspend fun refreshSession(): UserSession {
        delay(MockDelayMillis / 2)
        return _session.value
    }

    override suspend fun requireInvite(): UserSession {
        delay(MockDelayMillis / 2)
        val nextSession = inviteRequiredSession()
        _session.value = nextSession
        return nextSession
    }

    override suspend fun markBanned(): UserSession {
        delay(MockDelayMillis / 2)
        val nextSession = bannedSession()
        _session.value = nextSession
        return nextSession
    }

    override suspend fun signOut(): UserSession {
        delay(MockDelayMillis / 2)
        val nextSession = UserSession()
        _session.value = nextSession
        return nextSession
    }

    private fun authenticatedSession(email: String): UserSession {
        return UserSession(
            user = AuthUser(
                id = "mock-user-1",
                name = "Fernando USC",
                email = email.ifBlank { "membro@usc.app" },
                role = UserRole.User,
                status = UserStatus.Ativo,
            ),
            tenant = approvedTenant,
            status = AuthStatus.Authenticated,
        )
    }

    private fun waitingApprovalSession(
        email: String = "pendente@usc.app",
        name: String = "Membro em análise",
    ): UserSession {
        return UserSession(
            user = AuthUser(
                id = "mock-user-pending",
                name = name,
                email = email.ifBlank { "pendente@usc.app" },
                role = UserRole.Visitante,
                status = UserStatus.Pendente,
            ),
            tenant = approvedTenant.copy(membershipStatus = TenantMembershipStatus.Pending),
            status = AuthStatus.WaitingApproval,
        )
    }

    private fun inviteRequiredSession(email: String = "semconvite@usc.app"): UserSession {
        return UserSession(
            user = AuthUser(
                id = "mock-user-invite",
                name = "Visitante USC",
                email = email.ifBlank { "semconvite@usc.app" },
                role = UserRole.Visitante,
                status = UserStatus.Ativo,
            ),
            tenant = approvedTenant.copy(membershipStatus = TenantMembershipStatus.Unlinked),
            status = AuthStatus.InviteRequired,
        )
    }

    private fun bannedSession(email: String = "banido@usc.app"): UserSession {
        return UserSession(
            user = AuthUser(
                id = "mock-user-banned",
                name = "Usuário bloqueado",
                email = email.ifBlank { "banido@usc.app" },
                role = UserRole.User,
                status = UserStatus.Banned,
            ),
            tenant = approvedTenant,
            status = AuthStatus.Banned,
        )
    }

    private companion object {
        const val MockDelayMillis = 450L

        val approvedTenant = TenantContext(
            id = "tenant-aaakn",
            slug = "aaakn",
            name = "AAAKN USC",
            membershipStatus = TenantMembershipStatus.Approved,
        )
    }
}
