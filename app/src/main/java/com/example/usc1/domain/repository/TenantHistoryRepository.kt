package com.example.usc1.domain.repository

import com.example.usc1.domain.model.OrganogramConfig
import com.example.usc1.domain.model.OrganogramDisplayMember
import com.example.usc1.domain.model.TenantHistoryData

interface TenantHistoryRepository {
    /** `fetchHistoryPageConfig` + `fetchHistoricEvents` do web. */
    suspend fun getHistory(tenantId: String): TenantHistoryData

    /** `fetchOrganogramConfig` do web. */
    suspend fun getOrganogramConfig(tenantId: String): OrganogramConfig

    /** `fetchCanonicalUserVisuals` aplicado aos membros publicados. */
    suspend fun resolveOrganogramMembers(
        tenantId: String,
        config: OrganogramConfig,
        fallbackPhotoUrl: String?,
    ): List<OrganogramDisplayMember>
}
