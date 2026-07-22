package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminMentorshipRepository
import com.example.usc1.domain.model.AdminMentorshipCatalog
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig
import com.example.usc1.domain.repository.AdminMentorshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminMentorshipViewModel(
    private val repository: AdminMentorshipRepository = SupabaseAdminMentorshipRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminMentorshipUiState())
    val uiState: StateFlow<AdminMentorshipUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val labels = repository.fetchMentorshipLabels(forceRefresh)
                _uiState.update { it.copy(isLoading = false, labels = labels) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar configuração do apadrinhamento.",
                    )
                }
            }
        }
    }

    fun updateLabels(transform: (AdminMentorshipLabelsConfig) -> AdminMentorshipLabelsConfig) {
        _uiState.update {
            it.copy(
                labels = AdminMentorshipCatalog.normalize(transform(it.labels)),
                successMessage = null,
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            try {
                val labels = repository.saveMentorshipLabels(_uiState.value.labels)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        labels = labels,
                        successMessage = "Configuração de apadrinhamento salva.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Erro ao salvar configuração.",
                    )
                }
            }
        }
    }
}
