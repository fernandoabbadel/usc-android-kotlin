package com.example.usc1.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabasePlansRepository
import com.example.usc1.domain.repository.PlansRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlansViewModel(
    private val repository: PlansRepository = SupabasePlansRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()
    private var lastLoadedKey: String? = null
    private var loadJob: Job? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId:$userId:${session.user?.planName.orEmpty()}:${session.user?.planStatus.orEmpty()}"

        if (tenantId.isBlank()) {
            loadJob?.cancel()
            lastLoadedKey = null
            _uiState.update {
                it.copy(
                    plans = emptyList(),
                    orders = emptyList(),
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar os planos.",
                )
            }
            return
        }
        if (!forceRefresh && lastLoadedKey == key && _uiState.value.errorMessage == null) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val next = repository.getPlansHub(
                    tenantId = tenantId,
                    userId = userId,
                    userPlanName = session.user?.planName.orEmpty(),
                    userPlanStatus = session.user?.planStatus.orEmpty(),
                )
                lastLoadedKey = key
                _uiState.value = next.copy(isLoading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastLoadedKey = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível carregar os planos.",
                    )
                }
            }
        }
    }

    fun findPlan(id: String): UscPlan? {
        val clean = id.trim()
        return _uiState.value.plans.firstOrNull { it.id.equals(clean, ignoreCase = true) }
    }
}
