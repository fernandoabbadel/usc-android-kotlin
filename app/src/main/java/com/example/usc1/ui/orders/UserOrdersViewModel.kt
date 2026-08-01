package com.example.usc1.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseUserOrdersRepository
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab
import com.example.usc1.domain.repository.UserOrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserOrdersViewModel(
    private val repository: UserOrdersRepository = SupabaseUserOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserOrdersUiState(isLoading = true))
    val uiState: StateFlow<UserOrdersUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(
        session: UserSession,
        tab: UserOrderTab,
        forceRefresh: Boolean = false,
    ) {
        val tenant = session.tenant
        val tenantId = tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId::${tab.slug}"

        val brandLabel = tenant?.acronym.orEmpty().trim().uppercase()
            .ifBlank { tenant?.name.orEmpty().trim() }
            .ifBlank { "Atlética" }

        _uiState.update {
            it.copy(
                tab = tab,
                tenantBrandLabel = brandLabel,
                buyerName = session.user?.name.orEmpty(),
                buyerClass = session.user?.classCode.orEmpty(),
            )
        }

        if (userId.isBlank()) {
            lastLoadKey = ""
            _uiState.update { it.copy(orders = emptyList(), isLoading = false, errorMessage = "") }
            return
        }
        if (!forceRefresh && key == lastLoadKey && !_uiState.value.isLoading) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            runCatching {
                val orders = repository.getOrders(tenantId = tenantId, userId = userId, tab = tab)
                val finance = repository.getFinanceConfig(tenantId)
                orders to finance
            }.onSuccess { (orders, finance) ->
                _uiState.update {
                    it.copy(
                        orders = orders,
                        financeConfig = finance,
                        isLoading = false,
                        errorMessage = "",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        orders = emptyList(),
                        isLoading = false,
                        errorMessage = error.message.orEmpty()
                            .ifBlank { "Não foi possível carregar seus pedidos agora." },
                    )
                }
            }
        }
    }

    fun selectStatus(status: UserOrderStatus) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
    }

    fun goToPage(page: Int) {
        _uiState.update { state ->
            state.copy(page = page.coerceIn(1, state.totalPages))
        }
    }
}
