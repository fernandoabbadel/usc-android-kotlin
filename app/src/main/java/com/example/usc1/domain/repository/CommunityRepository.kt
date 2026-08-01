package com.example.usc1.domain.repository

import com.example.usc1.ui.community.CommunityComment
import com.example.usc1.ui.community.CommunityUiState

interface CommunityRepository {
    suspend fun getCommunityFeed(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        includeBlocked: Boolean = false,
    ): CommunityUiState

    suspend fun createPost(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        category: String,
        text: String,
    ): String

    /** Espelha `toggleCommunityPostReaction` do web-reference. */
    suspend fun togglePostReaction(
        tenantId: String,
        postId: String,
        userId: String,
        field: CommunityReactionField,
    ): CommunityReactionResult

    suspend fun getComments(
        tenantId: String,
        postId: String,
    ): List<CommunityComment>

    suspend fun createComment(
        tenantId: String,
        postId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        text: String,
    ): String
}

enum class CommunityReactionField(val column: String) {
    Likes("likes"),
    Hype("hype"),
}

data class CommunityReactionResult(
    val total: Int,
    val active: Boolean,
)
