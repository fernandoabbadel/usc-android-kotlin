package com.example.usc1.ui.events

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventStatus

data class EventsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedStatus: EventStatus? = null,
    val events: List<Event> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && events.isEmpty()

    companion object {
        fun loading() = EventsUiState(isLoading = true)
        fun empty() = EventsUiState(events = emptyList())
        fun error() = EventsUiState(errorMessage = "Não foi possível carregar os eventos mockados.")
    }
}

data class EventDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val event: Event? = null,
)

data class EventCheckoutUiState(
    val event: Event,
    val selectedQuantity: Int = 1,
) {
    val totalLabel: String = when {
        event.priceLabel.contains("R$ 45") && selectedQuantity == 1 -> "R$ 45,00"
        event.priceLabel.contains("R$ 45") -> "R$ ${45 * selectedQuantity},00"
        else -> event.priceLabel
    }
}
