package com.example.usc1.domain.repository

import com.example.usc1.ui.community.CommunityUiState

interface CommunityRepository {
    suspend fun getCommunityFeed(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        includeBlocked: Boolean = false,
    ): CommunityUiState
}
