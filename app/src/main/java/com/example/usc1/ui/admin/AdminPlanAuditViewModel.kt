package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminPlanSubscriptionsRepository
import com.example.usc1.domain.repository.AdminPlanSubscriptionsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminPlanAuditViewModel(
    private val repository: AdminPlanSubscriptionsRepository = SupabaseAdminPlanSubscriptionsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPlanAuditUiState())
    val uiState: StateFlow<AdminPlanAuditUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val requests = async {
                    repository.fetchPlanRequests(maxResults = 300, forceRefresh = forceRefresh)
                }
                val subscriptions = async {
                    repository.fetchPlanSubscriptions(maxResults = 600, forceRefresh = forceRefresh)
                }
                _uiState.value = AdminPlanAuditUiState.fromRows(
                    requests = requests.await(),
                    subscriptions = subscriptions.await(),
                )
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar auditoria.",
                    )
                }
            }
        }
    }
}
