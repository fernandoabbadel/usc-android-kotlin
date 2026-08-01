package com.example.usc1.domain.repository

import com.example.usc1.domain.model.ProfileBundle
import com.example.usc1.domain.model.ProfileFollowUser

interface ProfileRepository {
    suspend fun getProfileBundle(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
    ): ProfileBundle

    suspend fun toggleFollow(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
        follow: Boolean,
    )

    suspend fun getFollowList(
        tenantId: String,
        targetUserId: String,
        followers: Boolean,
    ): List<ProfileFollowUser>

    suspend fun toggleAffinity(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
        send: Boolean,
    )
}
