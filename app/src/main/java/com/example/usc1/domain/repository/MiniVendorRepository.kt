package com.example.usc1.domain.repository

import com.example.usc1.ui.vendor.MiniVendorProfileForm
import com.example.usc1.ui.vendor.MiniVendorOrderStatus
import com.example.usc1.ui.vendor.MiniVendorProductForm
import com.example.usc1.ui.vendor.MiniVendorUiState

interface MiniVendorRepository {
    suspend fun getDashboard(
        tenantId: String,
        userId: String,
    ): MiniVendorUiState

    suspend fun saveProfile(
        tenantId: String,
        userId: String,
        form: MiniVendorProfileForm,
    ): MiniVendorUiState

    suspend fun saveProduct(
        tenantId: String,
        userId: String,
        form: MiniVendorProductForm,
    ): MiniVendorUiState

    suspend fun setProductActive(
        tenantId: String,
        userId: String,
        productId: String,
        active: Boolean,
    ): MiniVendorUiState

    suspend fun setOrderStatus(
        tenantId: String,
        userId: String,
        orderId: String,
        status: MiniVendorOrderStatus,
        approvedBy: String,
    ): MiniVendorUiState
}
