package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminReportItem
import com.example.usc1.domain.model.AdminReportsPage
import com.example.usc1.domain.model.AdminReportsSection

interface AdminReportsRepository {
    suspend fun getReports(
        section: AdminReportsSection,
        limit: Int,
        forceRefresh: Boolean,
    ): AdminReportsPage

    suspend fun resolveReport(
        report: AdminReportItem,
        response: String,
    )

    suspend fun deleteReport(report: AdminReportItem)
}
