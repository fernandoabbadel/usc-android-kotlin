package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminPlanSubscriptionsRepository
import com.example.usc1.domain.model.AdminPlanSubscriptionListKind
import com.example.usc1.domain.repository.AdminPlanSubscriptionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminPlanSubscriptionsViewModel(
    private val repository: AdminPlanSubscriptionsRepository = SupabaseAdminPlanSubscriptionsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPlanSubscriptionsUiState())
    val uiState: StateFlow<AdminPlanSubscriptionsUiState> = _uiState.asStateFlow()

    fun load(kind: AdminPlanSubscriptionListKind, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh,
                    isRefreshing = forceRefresh,
                    errorMessage = null,
                    kind = kind,
                    page = 1,
                )
            }
            try {
                val rows = repository.fetchPlanSubscriptions(
                    maxResults = MaxPlanSubscriptions,
                    forceRefresh = forceRefresh,
                ).filter(kind::matches)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        rows = rows,
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        rows = emptyList(),
                        errorMessage = error.message ?: "Erro ao carregar assinaturas.",
                    )
                }
            }
        }
    }

    fun nextPage() {
        _uiState.update {
            if (it.canGoNext) it.copy(page = it.page + 1) else it
        }
    }

    fun previousPage() {
        _uiState.update {
            if (it.canGoPrevious) it.copy(page = it.page - 1) else it
        }
    }

    private companion object {
        const val MaxPlanSubscriptions = 600
    }
}
