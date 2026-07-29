package com.example.usc1.domain.repository

import com.example.usc1.ui.collectives.CollectiveGroup
import com.example.usc1.ui.collectives.CollectiveKind

interface CollectivesRepository {
    suspend fun getCollectives(
        tenantId: String,
        kind: CollectiveKind,
    ): List<CollectiveGroup>
}
