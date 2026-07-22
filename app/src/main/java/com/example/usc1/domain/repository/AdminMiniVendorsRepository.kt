package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminMiniVendor
import com.example.usc1.domain.model.AdminMiniVendorStatus

interface AdminMiniVendorsRepository {
    suspend fun getMiniVendors(forceRefresh: Boolean): List<AdminMiniVendor>

    suspend fun setMiniVendorStatus(
        miniVendorId: String,
        status: AdminMiniVendorStatus,
        approvedBy: String,
    )

    suspend fun setCategoryVisibility(miniVendorId: String, visible: Boolean)
}
