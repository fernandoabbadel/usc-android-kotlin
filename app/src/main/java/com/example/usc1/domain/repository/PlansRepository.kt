package com.example.usc1.domain.repository

import com.example.usc1.ui.plans.PlanUiState

interface PlansRepository {
    suspend fun getPlansHub(
        tenantId: String,
        userId: String,
        userPlanName: String,
        userPlanStatus: String,
    ): PlanUiState
}
