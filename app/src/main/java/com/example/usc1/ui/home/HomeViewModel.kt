package com.example.usc1.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseHomeDashboardRepository
import com.example.usc1.domain.repository.HomeDashboardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeDashboardRepository = SupabaseHomeDashboardRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState.loading())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var lastSession: UserSession? = null
    private var lastLoadedKey: String? = null
    private var loadJob: Job? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        lastSession = session
        val tenantId = session.tenant?.id.orEmpty().trim()
        val tenantSlug = session.tenant?.slug.orEmpty().trim().lowercase()
        val userId = session.user?.id.orEmpty().trim()
        val loadKey = "$tenantId:$tenantSlug:$userId"

        if (tenantId.isBlank()) {
            loadJob?.cancel()
            lastLoadedKey = null
            _uiState.value = HomeUiState(
                errorMessage = "Selecione uma atlética para carregar o dashboard.",
            )
            return
        }
        if (!forceRefresh && lastLoadedKey == loadKey && _uiState.value.errorMessage == null) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val dashboard = repository.getDashboard(
                    tenantId = tenantId,
                    tenantSlug = tenantSlug,
                    userId = userId.takeIf { it.isNotBlank() && !it.startsWith("guest_virtual_") },
                    forceRefresh = forceRefresh,
                )
                lastLoadedKey = loadKey
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        dashboard = dashboard,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastLoadedKey = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível carregar o dashboard.",
                    )
                }
            }
        }
    }

    fun refresh() {
        lastSession?.let { load(it, forceRefresh = true) }
    }
}
