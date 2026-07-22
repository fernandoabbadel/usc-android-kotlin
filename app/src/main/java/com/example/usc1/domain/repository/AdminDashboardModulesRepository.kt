package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminDashboardModulesBundle
import com.example.usc1.domain.model.TenantAppModulesConfig

interface AdminDashboardModulesRepository {
    suspend fun getModulesBundle(
        tenantName: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): AdminDashboardModulesBundle

    suspend fun saveModulesConfig(config: TenantAppModulesConfig)
}
