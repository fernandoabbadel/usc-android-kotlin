package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminDatabaseScannerRepository
import com.example.usc1.domain.repository.AdminDatabaseScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDatabaseScannerViewModel(
    private val repository: AdminDatabaseScannerRepository = SupabaseAdminDatabaseScannerRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDatabaseScannerUiState())
    val uiState: StateFlow<AdminDatabaseScannerUiState> = _uiState.asStateFlow()

    fun scanDatabase(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val report = repository.scanDatabaseFields(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenantId = report.tenantId,
                        report = report.tables,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao escanear campos.",
                    )
                }
            }
        }
    }
}
