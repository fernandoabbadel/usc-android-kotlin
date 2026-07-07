package com.example.usc1.ui.tenant

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TenantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()

    fun selectTenant(tenant: TenantIdentity) {
        _uiState.update { it.copy(currentTenant = tenant) }
    }
}
