package com.example.usc1.ui.membershipCard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseMembershipCardRepository
import com.example.usc1.domain.repository.MembershipCardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MembershipCardViewModel(
    private val repository: MembershipCardRepository = SupabaseMembershipCardRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MembershipCardUiState())
    val uiState: StateFlow<MembershipCardUiState> = _uiState.asStateFlow()
    private var lastTenantId: String? = null
    private var loadJob: Job? = null

    fun load(tenantId: String, forceRefresh: Boolean = false) {
        val normalizedTenantId = tenantId.trim()
        if (!forceRefresh && lastTenantId == normalizedTenantId && _uiState.value.errorMessage == null) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isConfigLoading = true, errorMessage = null) }
            try {
                val config = repository.getConfig(normalizedTenantId)
                lastTenantId = normalizedTenantId
                _uiState.update {
                    it.copy(
                        isConfigLoading = false,
                        errorMessage = null,
                        config = config,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastTenantId = null
                _uiState.update {
                    it.copy(
                        isConfigLoading = false,
                        errorMessage = error.message
                            ?: "Não foi possível carregar a configuração da carteirinha.",
                    )
                }
            }
        }
    }

    fun refresh() {
        load(lastTenantId.orEmpty(), forceRefresh = true)
    }
}
