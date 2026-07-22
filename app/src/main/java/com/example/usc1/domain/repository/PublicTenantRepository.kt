package com.example.usc1.domain.repository

import com.example.usc1.domain.model.PublicTenant

interface PublicTenantRepository {
    suspend fun getDirectory(): List<PublicTenant>

    suspend fun resolveActiveTenant(
        tenantId: String,
        tenantSlug: String,
    ): PublicTenant?
}
