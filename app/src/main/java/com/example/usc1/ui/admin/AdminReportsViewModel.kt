package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminReportsRepository
import com.example.usc1.domain.model.AdminReportItem
import com.example.usc1.domain.model.AdminReportsSection
import com.example.usc1.domain.repository.AdminReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminReportsViewModel(
    private val repository: AdminReportsRepository = SupabaseAdminReportsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

    fun load(
        section: AdminReportsSection,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    section = section,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val page = repository.getReports(
                    section = section,
                    limit = 240,
                    forceRefresh = forceRefresh,
                )
                _uiState.update { page.toUiState(it) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar denúncias.",
                    )
                }
            }
        }
    }

    fun updateResponse(reportId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                responsesById = state.responsesById + (reportId to value.take(2_000)),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun resolve(report: AdminReportItem) {
        val response = _uiState.value.responsesById[report.id].orEmpty().trim()
        if (response.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Escreva a resposta para o usuário.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyId = report.id, errorMessage = null, actionMessage = null) }
            try {
                repository.resolveReport(report, response)
                val section = _uiState.value.section
                val page = repository.getReports(section, limit = 240, forceRefresh = true)
                _uiState.update {
                    page.toUiState(
                        it.copy(
                            busyId = "",
                            actionMessage = "Resposta enviada e denúncia concluída.",
                        ),
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        busyId = "",
                        errorMessage = error.message ?: "Erro ao responder denúncia.",
                    )
                }
            }
        }
    }

    fun delete(report: AdminReportItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyId = report.id, errorMessage = null, actionMessage = null) }
            try {
                repository.deleteReport(report)
                _uiState.update {
                    it.copy(
                        busyId = "",
                        rows = it.rows.filterNot { row -> row.id == report.id },
                        actionMessage = "Registro excluído.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        busyId = "",
                        errorMessage = error.message ?: "Erro ao excluir denúncia.",
                    )
                }
            }
        }
    }

    fun previousPage() {
        _uiState.update { it.copy(page = maxOf(1, it.page - 1)) }
    }

    fun nextPage() {
        _uiState.update { it.copy(page = minOf(it.totalPages, it.page + 1)) }
    }
}
