package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminTenantPoliciesBundle
import com.example.usc1.domain.model.TenantPolicyDocument

data class AdminTenantPoliciesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null,
    val tenantId: String = "",
    val tenantName: String = "",
    val tenantSlug: String = "",
    val policies: List<TenantPolicyDocument> = emptyList(),
) {
    val hasActiveTenant: Boolean
        get() = tenantId.isNotBlank()

    val tenantLabel: String
        get() = tenantSlug.ifBlank { tenantName.ifBlank { "tenant atual" } }
}

fun AdminTenantPoliciesBundle.toUiState(): AdminTenantPoliciesUiState {
    return AdminTenantPoliciesUiState(
        isLoading = false,
        tenantId = tenantId,
        tenantName = tenantName,
        tenantSlug = tenantSlug,
        policies = policies,
    )
}
