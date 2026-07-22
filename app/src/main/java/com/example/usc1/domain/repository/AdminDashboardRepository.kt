package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminDashboardBundle

interface AdminDashboardRepository {
    suspend fun getDashboardBundle(
        usersLimit: Int = 5,
        logsLimit: Int = 5,
        forceRefresh: Boolean = false,
    ): AdminDashboardBundle
}
