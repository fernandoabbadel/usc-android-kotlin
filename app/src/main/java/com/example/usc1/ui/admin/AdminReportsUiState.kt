package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminReportItem
import com.example.usc1.domain.model.AdminReportsPage
import com.example.usc1.domain.model.AdminReportsSection

data class AdminReportsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val section: AdminReportsSection = AdminReportsSection.Community,
    val tenantId: String = "",
    val rows: List<AdminReportItem> = emptyList(),
    val page: Int = 1,
    val busyId: String = "",
    val responsesById: Map<String, String> = emptyMap(),
) {
    val pageSize: Int = 20

    val totalPages: Int
        get() = maxOf(1, (rows.size + pageSize - 1) / pageSize)

    val visibleRows: List<AdminReportItem>
        get() = rows.drop((page - 1) * pageSize).take(pageSize)

    val canGoPrevious: Boolean
        get() = page > 1

    val canGoNext: Boolean
        get() = page < totalPages

    val supportsResponseActions: Boolean
        get() = section == AdminReportsSection.Banned || section == AdminReportsSection.Support
}

fun AdminReportsPage.toUiState(current: AdminReportsUiState): AdminReportsUiState {
    return current.copy(
        isLoading = false,
        errorMessage = null,
        tenantId = tenantId,
        section = section,
        rows = rows,
        page = current.page.coerceIn(1, maxOf(1, (rows.size + current.pageSize - 1) / current.pageSize)),
        responsesById = rows.associate { row -> row.id to (current.responsesById[row.id] ?: row.adminResponse) },
    )
}
