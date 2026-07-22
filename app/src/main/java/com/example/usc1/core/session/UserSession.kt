package com.example.usc1.core.session

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.tenant.TenantContext

data class UserSession(
    val user: AuthUser? = null,
    val tenant: TenantContext? = null,
    val status: AuthStatus = AuthStatus.Unauthenticated,
) {
    val isAuthenticated: Boolean = status == AuthStatus.Authenticated && user != null
}

data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val registrationNumber: String = "",
    val classCode: String = "",
    val classPhotoUrl: String? = null,
    val planName: String = "",
    val planBadge: String = "",
    val planColorKey: String = "zinc",
    val planIconKey: String = "ghost",
    val planStatus: String = "",
    val role: UserRole = UserRole.Visitante,
    val status: UserStatus = UserStatus.Ativo,
)

typealias SessionUser = AuthUser

enum class AuthStatus {
    Loading,
    Unauthenticated,
    Authenticated,
    WaitingApproval,
    InviteRequired,
    Banned,
}

enum class UserStatus(val remoteValue: String) {
    Ativo("ativo"),
    Inadimplente("inadimplente"),
    Banned("banned"),
    Pendente("pendente"),
    Paused("paused"),
    Bloqueado("bloqueado");

    val isBlocked: Boolean
        get() = this == Banned || this == Bloqueado

    companion object {
        fun fromRemote(value: String?): UserStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Ativo
        }
    }
}
