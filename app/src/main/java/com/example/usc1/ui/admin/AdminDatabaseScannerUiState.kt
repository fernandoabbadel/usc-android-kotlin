package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminDatabaseTableFields

data class AdminDatabaseScannerUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tenantId: String = "",
    val report: List<AdminDatabaseTableFields> = emptyList(),
)
