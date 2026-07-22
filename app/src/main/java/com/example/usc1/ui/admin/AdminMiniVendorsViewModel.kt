package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminMiniVendorsRepository
import com.example.usc1.domain.model.AdminMiniVendor
import com.example.usc1.domain.model.AdminMiniVendorDirectoryMode
import com.example.usc1.domain.model.AdminMiniVendorStatus
import com.example.usc1.domain.repository.AdminMiniVendorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminMiniVendorsViewModel(
    private val repository: AdminMiniVendorsRepository = SupabaseAdminMiniVendorsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminMiniVendorsUiState())
    val uiState: StateFlow<AdminMiniVendorsUiState> = _uiState.asStateFlow()

    fun load(mode: AdminMiniVendorDirectoryMode, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    mode = mode,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val rows = repository.getMiniVendors(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rows = rows,
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar mini vendors.",
                    )
                }
            }
        }
    }

    fun approve(row: AdminMiniVendor, approvedBy: String) {
        updateStatus(row, AdminMiniVendorStatus.Approved, "Status do mini vendor atualizado.", approvedBy)
    }

    fun reject(row: AdminMiniVendor, approvedBy: String) {
        updateStatus(row, AdminMiniVendorStatus.Rejected, "Status do mini vendor atualizado.", approvedBy)
    }

    fun disable(row: AdminMiniVendor, approvedBy: String) {
        updateStatus(row, AdminMiniVendorStatus.Disabled, "Status do mini vendor atualizado.", approvedBy)
    }

    fun toggleCategoryVisibility(row: AdminMiniVendor) {
        viewModelScope.launch {
            val actionId = "category:${row.id}"
            _uiState.update { it.copy(mutatingId = actionId, errorMessage = null, actionMessage = null) }
            try {
                repository.setCategoryVisibility(row.id, !row.categoryVisible)
                val mode = _uiState.value.mode
                val rows = repository.getMiniVendors(forceRefresh = true)
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        mode = mode,
                        rows = rows,
                        actionMessage = if (row.categoryVisible) {
                            "Categoria ocultada da loja."
                        } else {
                            "Categoria exibida na loja."
                        },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        errorMessage = error.message ?: "Erro ao atualizar a categoria publica.",
                    )
                }
            }
        }
    }

    private fun updateStatus(
        row: AdminMiniVendor,
        status: AdminMiniVendorStatus,
        successMessage: String,
        approvedBy: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingId = row.id, errorMessage = null, actionMessage = null) }
            try {
                repository.setMiniVendorStatus(row.id, status, approvedBy)
                val mode = _uiState.value.mode
                val rows = repository.getMiniVendors(forceRefresh = true)
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        mode = mode,
                        rows = rows,
                        actionMessage = successMessage,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        errorMessage = error.message ?: "Erro ao atualizar mini vendor.",
                    )
                }
            }
        }
    }
}
