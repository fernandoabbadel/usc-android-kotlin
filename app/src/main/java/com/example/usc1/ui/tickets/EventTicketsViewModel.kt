package com.example.usc1.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.MockEventTicketsRepository
import com.example.usc1.domain.repository.EventTicketsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventTicketsViewModel(
    private val ticketsRepository: EventTicketsRepository = MockEventTicketsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventTicketsUiState.loading())
    val uiState: StateFlow<EventTicketsUiState> = _uiState.asStateFlow()

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _uiState.value = EventTicketsUiState.loading()
            _uiState.value = EventTicketsUiState(tickets = ticketsRepository.getTickets())
        }
    }
}

class EventTicketDetailViewModel(
    private val ticketsRepository: EventTicketsRepository = MockEventTicketsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventTicketDetailUiState(isLoading = true))
    val uiState: StateFlow<EventTicketDetailUiState> = _uiState.asStateFlow()

    fun loadTicket(ticketId: String) {
        viewModelScope.launch {
            _uiState.value = EventTicketDetailUiState(isLoading = true)
            val ticket = ticketsRepository.getTicketById(ticketId)
            _uiState.value = if (ticket == null) {
                EventTicketDetailUiState(errorMessage = "Ingresso não encontrado.")
            } else {
                EventTicketDetailUiState(ticket = ticket)
            }
        }
    }
}
