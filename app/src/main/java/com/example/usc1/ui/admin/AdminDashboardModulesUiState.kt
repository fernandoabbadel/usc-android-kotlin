package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminDashboardModulesBundle
import com.example.usc1.domain.model.AdminDashboardModulesGroup
import com.example.usc1.domain.model.TenantAppModuleDefinition
import com.example.usc1.domain.model.TenantAppModulesCatalog
import com.example.usc1.domain.model.TenantAppModulesConfig

data class AdminDashboardModulesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null,
    val tenantId: String = "",
    val tenantName: String = "",
    val tenantSlug: String = "",
    val activeProfileName: String = "",
    val modules: Map<String, Boolean> = TenantAppModulesCatalog.defaultConfig.modules,
    val groups: List<AdminDashboardModulesGroup> = emptyList(),
) {
    val hasActiveTenant: Boolean
        get() = tenantId.isNotBlank()

    fun isModuleEnabled(module: TenantAppModuleDefinition): Boolean {
        return modules[module.key] != false
    }

    fun toConfig(): TenantAppModulesConfig {
        return TenantAppModulesConfig(modules = modules)
    }
}

fun AdminDashboardModulesBundle.toUiState(): AdminDashboardModulesUiState {
    return AdminDashboardModulesUiState(
        isLoading = false,
        tenantId = tenantId,
        tenantName = tenantName,
        tenantSlug = tenantSlug,
        activeProfileName = activeProfileName,
        modules = config.modules,
        groups = groups,
    )
}
