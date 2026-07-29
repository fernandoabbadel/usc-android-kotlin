package com.example.usc1.domain.repository

import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsMentorshipHub

interface SettingsRepository {
    suspend fun getInviteDashboard(
        tenantId: String,
        userId: String,
        limit: Int = 50,
    ): SettingsInviteDashboard

    suspend fun getMentorshipHub(
        tenantId: String,
        userId: String,
    ): SettingsMentorshipHub
}
