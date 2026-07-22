package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminStoreBundle
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreFinanceConfig
import com.example.usc1.domain.model.AdminStoreMenuItem

data class AdminStoreUiState(
    val isLoading: Boolean = true,
    val isSavingFinance: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null,
    val tenantId: String = "",
    val finance: AdminStoreFinanceConfig = AdminStoreFinanceConfig(),
    val menuItems: List<AdminStoreMenuItem> = AdminStoreCatalog.menuItems,
) {
    val hasActiveTenant: Boolean
        get() = tenantId.isNotBlank()
}

fun AdminStoreBundle.toUiState(): AdminStoreUiState {
    return AdminStoreUiState(
        isLoading = false,
        tenantId = tenantId,
        finance = finance,
        menuItems = menuItems,
    )
}
