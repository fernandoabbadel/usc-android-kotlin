package com.example.usc1.ui.store

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StoreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    fun selectCategory(category: String) {
        _uiState.update { current ->
            current.copy(
                selectedCategory = category,
                products = if (category == "Todos") {
                    StoreMockData.products
                } else {
                    StoreMockData.products.filter { it.category == category }
                },
            )
        }
    }

    fun refresh() {
        _uiState.value = StoreUiState()
    }
}

class CartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
}

class StoreOrdersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StoreOrdersUiState())
    val uiState: StateFlow<StoreOrdersUiState> = _uiState.asStateFlow()

    fun selectStatus(status: StoreOrderStatus?) {
        _uiState.update { current ->
            current.copy(
                selectedStatus = status,
                orders = status?.let { selected ->
                    StoreMockData.orders.filter { it.status == selected }
                } ?: StoreMockData.orders,
            )
        }
    }
}
