package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseAdminDashboardRepository
import com.example.usc1.domain.repository.AdminDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val repository: AdminDashboardRepository = SupabaseAdminDashboardRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenant = session.tenant
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    tenantName = tenant?.name.orEmpty(),
                    tenantSigla = tenant?.slug.orEmpty().uppercase(),
                )
            }
            try {
                val bundle = repository.getDashboardBundle(
                    usersLimit = 5,
                    logsLimit = 5,
                    forceRefresh = forceRefresh,
                )
                _uiState.value = bundle.toUiState(
                    tenantName = tenant?.name.orEmpty(),
                    tenantSigla = tenant?.slug.orEmpty().uppercase(),
                )
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar dashboard.",
                    )
                }
            }
        }
    }
}
