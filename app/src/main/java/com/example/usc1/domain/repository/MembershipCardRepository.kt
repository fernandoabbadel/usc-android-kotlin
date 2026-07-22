package com.example.usc1.domain.repository

import com.example.usc1.domain.model.MembershipCardConfig

interface MembershipCardRepository {
    suspend fun getConfig(tenantId: String): MembershipCardConfig
}
