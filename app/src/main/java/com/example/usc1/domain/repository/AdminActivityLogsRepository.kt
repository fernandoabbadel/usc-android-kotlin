package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminActivityLogsPage

interface AdminActivityLogsRepository {
    suspend fun fetchAdminActivityLogsPage(
        pageSize: Int,
        cursorId: String? = null,
        forceRefresh: Boolean = false,
    ): AdminActivityLogsPage
}
