package com.example.usc1.ui.generalorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseGeneralOrdersRepository
import com.example.usc1.domain.repository.GeneralOrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeneralOrdersViewModel(
    private val repository: GeneralOrdersRepository = SupabaseGeneralOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeneralOrdersUiState(isLoading = true))
    val uiState: StateFlow<GeneralOrdersUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.update {
                it.copy(
                    orders = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                )
            }
            return
        }
        if (!forceRefresh && key == lastLoadKey && !_uiState.value.isLoading) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getOrders(tenantId = tenantId, userId = userId)
            }.onSuccess { orders ->
                _uiState.update {
                    it.copy(
                        orders = orders,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        orders = emptyList(),
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar seus pedidos agora."
                        },
                    )
                }
            }
        }
    }

    fun selectStatus(status: GeneralOrderStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }
}
