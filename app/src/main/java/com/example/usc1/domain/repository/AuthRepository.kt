package com.example.usc1.domain.repository

import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantContext
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<UserSession>

    suspend fun signIn(email: String, password: String): UserSession
    suspend fun signInWithGoogle(): UserSession
    suspend fun loginAsGuest(): UserSession
    suspend fun selectGuestTenant(tenant: TenantContext): UserSession
    suspend fun register(fullName: String, email: String, inviteCode: String?): UserSession
    suspend fun refreshSession(): UserSession
    suspend fun requireInvite(): UserSession
    suspend fun markBanned(): UserSession
    suspend fun signOut(): UserSession
}
