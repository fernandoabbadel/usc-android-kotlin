package com.example.usc1.domain.repository

import com.example.usc1.ui.vendor.MiniVendorUiState

interface MiniVendorRepository {
    suspend fun getDashboard(
        tenantId: String,
        userId: String,
    ): MiniVendorUiState
}
