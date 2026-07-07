package com.example.usc1.ui.orders

import com.example.usc1.domain.model.EventOrder

data class EventOrdersUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orders: List<EventOrder> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && orders.isEmpty()

    companion object {
        fun loading() = EventOrdersUiState(isLoading = true)
    }
}

data class EventOrderDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val order: EventOrder? = null,
)
