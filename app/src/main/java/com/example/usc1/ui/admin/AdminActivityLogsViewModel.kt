package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminActivityLogsRepository
import com.example.usc1.domain.model.AdminActivityLogsCatalog
import com.example.usc1.domain.model.AdminActivityLogRecord
import com.example.usc1.domain.repository.AdminActivityLogsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminActivityLogsViewModel(
    private val repository: AdminActivityLogsRepository = SupabaseAdminActivityLogsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminActivityLogsUiState())
    val uiState: StateFlow<AdminActivityLogsUiState> = _uiState.asStateFlow()

    fun loadInitial(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val page = repository.fetchAdminActivityLogsPage(
                    pageSize = AdminActivityLogsCatalog.PageSize,
                    forceRefresh = forceRefresh,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        logs = page.logs,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        logs = emptyList(),
                        cursor = null,
                        hasMore = false,
                        errorMessage = error.message ?: "Não foi possível carregar os logs agora.",
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val cursor = state.cursor ?: return
        if (!state.hasMore || state.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val page = repository.fetchAdminActivityLogsPage(
                    pageSize = AdminActivityLogsCatalog.PageSize,
                    cursorId = cursor,
                )
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        logs = mergeUniqueLogs(it.logs, page.logs),
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Não foi possível carregar mais logs.",
                    )
                }
            }
        }
    }

    fun setSearchTerm(value: String) {
        _uiState.update { it.copy(searchTerm = value) }
    }

    private fun mergeUniqueLogs(
        current: List<AdminActivityLogRecord>,
        next: List<AdminActivityLogRecord>,
    ): List<AdminActivityLogRecord> {
        if (next.isEmpty()) return current
        val ids = current.map { it.id }.toMutableSet()
        return current + next.filter { ids.add(it.id) }
    }
}
