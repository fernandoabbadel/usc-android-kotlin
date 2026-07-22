package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminTenantPoliciesBundle
import com.example.usc1.domain.model.TenantPolicyDocument

interface AdminTenantPoliciesRepository {
    suspend fun getPoliciesBundle(
        tenantName: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): AdminTenantPoliciesBundle

    suspend fun savePolicies(policies: List<TenantPolicyDocument>): List<TenantPolicyDocument>
}
