package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminActivityLogRecord

data class AdminActivityLogsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val searchTerm: String = "",
    val logs: List<AdminActivityLogRecord> = emptyList(),
    val cursor: String? = null,
    val hasMore: Boolean = false,
) {
    val filteredLogs: List<AdminActivityLogRecord>
        get() {
            val term = searchTerm.trim().lowercase()
            if (term.isBlank()) return logs
            return logs.filter { log ->
                log.userName.lowercase().contains(term) ||
                    log.details.lowercase().contains(term) ||
                    log.resource.lowercase().contains(term) ||
                    log.action.lowercase().contains(term)
            }
        }
}
