package com.example.usc1.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseEventOrdersRepository
import com.example.usc1.domain.repository.EventOrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventOrdersViewModel(
    private val eventOrdersRepository: EventOrdersRepository = SupabaseEventOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventOrdersUiState.loading())
    val uiState: StateFlow<EventOrdersUiState> = _uiState.asStateFlow()
    private var lastLoadKey: String = ""

    fun loadOrders(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = EventOrdersUiState(orders = emptyList())
            return
        }
        if (!forceRefresh && key == lastLoadKey && !_uiState.value.isLoading) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.value = EventOrdersUiState.loading()
            runCatching {
                eventOrdersRepository.getOrders(tenantId = tenantId, userId = userId)
            }.onSuccess { orders ->
                _uiState.value = EventOrdersUiState(orders = orders)
            }.onFailure { error ->
                _uiState.value = EventOrdersUiState(
                    errorMessage = error.message ?: "Não foi possível carregar pedidos de evento.",
                )
            }
        }
    }
}

class EventOrderDetailViewModel(
    private val eventOrdersRepository: EventOrdersRepository = SupabaseEventOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventOrderDetailUiState(isLoading = true))
    val uiState: StateFlow<EventOrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrder(
        session: UserSession,
        orderId: String,
    ) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || orderId.isBlank()) {
            _uiState.value = EventOrderDetailUiState(errorMessage = "Pedido não encontrado.")
            return
        }

        viewModelScope.launch {
            _uiState.value = EventOrderDetailUiState(isLoading = true)
            runCatching {
                eventOrdersRepository.getOrderById(
                    tenantId = tenantId,
                    userId = userId,
                    orderId = orderId,
                )
            }.onSuccess { order ->
                _uiState.value = if (order == null) {
                    EventOrderDetailUiState(errorMessage = "Pedido não encontrado.")
                } else {
                    EventOrderDetailUiState(order = order)
                }
            }.onFailure { error ->
                _uiState.value = EventOrderDetailUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o pedido.",
                )
            }
        }
    }
}
