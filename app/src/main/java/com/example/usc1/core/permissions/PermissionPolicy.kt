package com.example.usc1.core.permissions

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.UserSession
import com.example.usc1.domain.model.AppModule

class PermissionPolicy {
    fun canOpenModule(
        session: UserSession,
        module: AppModule,
        visibleTenantModuleKeys: Set<String> = emptySet(),
    ): PermissionDecision {
        val user = session.user ?: return if (module.requiresAuthentication) {
            PermissionDecision.blocked(PermissionBlockReason.NotAuthenticated)
        } else {
            PermissionDecision.Allowed
        }

        if (user.status.isBlocked) {
            return PermissionDecision.blocked(PermissionBlockReason.Banned)
        }

        if (session.tenant?.membershipStatus?.isPending == true) {
            return PermissionDecision.blocked(PermissionBlockReason.TenantPending)
        }

        if (visibleTenantModuleKeys.isNotEmpty() && module.remoteKey !in visibleTenantModuleKeys) {
            return PermissionDecision.blocked(PermissionBlockReason.ModuleHidden)
        }

        if (module.allowedRoles.isNotEmpty() && user.role !in module.allowedRoles) {
            return PermissionDecision.blocked(PermissionBlockReason.MissingRole)
        }

        return PermissionDecision.Allowed
    }

    fun isScannerAllowed(role: UserRole): Boolean = role in scannerRoles

    fun canUsePermission(
        session: UserSession,
        permission: Permission,
    ): PermissionDecision {
        val user = session.user ?: return PermissionDecision.blocked(PermissionBlockReason.NotAuthenticated)

        if (!session.isAuthenticated) {
            return PermissionDecision.blocked(PermissionBlockReason.NotAuthenticated)
        }

        if (user.status.isBlocked) {
            return PermissionDecision.blocked(PermissionBlockReason.Banned)
        }

        if (session.tenant?.membershipStatus?.isPending == true) {
            return PermissionDecision.blocked(PermissionBlockReason.TenantPending)
        }

        return when (permission) {
            Permission.UseScanner -> if (user.role in scannerRoles) {
                PermissionDecision.Allowed
            } else {
                PermissionDecision.blocked(PermissionBlockReason.MissingRole)
            }
            Permission.ManageMiniVendor -> if (user.role in miniVendorRoles) {
                PermissionDecision.Allowed
            } else {
                PermissionDecision.blocked(PermissionBlockReason.MissingRole)
            }
            Permission.ManageTenant -> if (user.role.canManageTenant) {
                PermissionDecision.Allowed
            } else {
                PermissionDecision.blocked(PermissionBlockReason.MissingRole)
            }
            else -> PermissionDecision.Allowed
        }
    }

    companion object {
        val scannerRoles = setOf(
            UserRole.Master,
            UserRole.MasterTenant,
            UserRole.AdminGeral,
            UserRole.AdminGestor,
            UserRole.MiniVendor,
            UserRole.Vendas,
        )

        val miniVendorRoles = setOf(
            UserRole.Master,
            UserRole.MasterTenant,
            UserRole.AdminGeral,
            UserRole.AdminGestor,
            UserRole.MiniVendor,
            UserRole.Vendas,
        )
    }
}
