package com.example.usc1.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.repository.EventOrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventOrdersViewModel(
    private val eventOrdersRepository: EventOrdersRepository = MockEventOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventOrdersUiState.loading())
    val uiState: StateFlow<EventOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = EventOrdersUiState.loading()
            _uiState.value = EventOrdersUiState(orders = eventOrdersRepository.getOrders())
        }
    }
}

class EventOrderDetailViewModel(
    private val eventOrdersRepository: EventOrdersRepository = MockEventOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventOrderDetailUiState(isLoading = true))
    val uiState: StateFlow<EventOrderDetailUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = EventOrderDetailUiState(isLoading = true)
            val order = eventOrdersRepository.getOrderById(orderId)
            _uiState.value = if (order == null) {
                EventOrderDetailUiState(errorMessage = "Pedido não encontrado.")
            } else {
                EventOrderDetailUiState(order = order)
            }
        }
    }
}
