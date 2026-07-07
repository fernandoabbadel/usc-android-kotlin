package com.example.usc1.ui.tickets

import com.example.usc1.domain.model.EventTicket

data class EventTicketsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tickets: List<EventTicket> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && tickets.isEmpty()

    companion object {
        fun loading() = EventTicketsUiState(isLoading = true)
    }
}

data class EventTicketDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val ticket: EventTicket? = null,
)
