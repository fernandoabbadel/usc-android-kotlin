package com.example.usc1.domain.repository

import com.example.usc1.domain.model.HomeDashboardBundle

interface HomeDashboardRepository {
    suspend fun getDashboard(
        tenantId: String,
        tenantSlug: String,
        userId: String?,
        forceRefresh: Boolean = false,
    ): HomeDashboardBundle
}
