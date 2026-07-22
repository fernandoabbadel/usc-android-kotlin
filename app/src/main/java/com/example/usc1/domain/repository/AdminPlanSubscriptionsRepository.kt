package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminPlanSubscription
import com.example.usc1.domain.model.AdminPlanRequest

interface AdminPlanSubscriptionsRepository {
    suspend fun fetchPlanSubscriptions(
        maxResults: Int,
        forceRefresh: Boolean,
    ): List<AdminPlanSubscription>

    suspend fun fetchPlanRequests(
        maxResults: Int,
        forceRefresh: Boolean,
    ): List<AdminPlanRequest>
}
