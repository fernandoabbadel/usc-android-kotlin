package com.example.usc1.core.permissions

enum class PermissionBlockReason {
    NotAuthenticated,
    Banned,
    TenantPending,
    InviteRequired,
    MissingRole,
    ModuleHidden,
    FeatureNotNativeYet,
}

data class PermissionDecision(
    val allowed: Boolean,
    val reason: PermissionBlockReason? = null,
) {
    companion object {
        val Allowed = PermissionDecision(allowed = true)

        fun blocked(reason: PermissionBlockReason) = PermissionDecision(
            allowed = false,
            reason = reason,
        )
    }
}
