package com.example.usc1.domain.repository

import com.example.usc1.ui.guide.GuideUiState
import com.example.usc1.ui.guide.LegalUiState

interface GuideRepository {
    suspend fun getGuide(tenantId: String): GuideUiState
    suspend fun getLegalDocs(tenantId: String): LegalUiState
}
