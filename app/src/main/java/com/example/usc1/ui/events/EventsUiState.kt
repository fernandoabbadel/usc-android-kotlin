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
        fun error(message: String? = null) = EventsUiState(
            errorMessage = message ?: "Não foi possível carregar os eventos.",
        )
    }
}

data class EventDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val event: Event? = null,
)
