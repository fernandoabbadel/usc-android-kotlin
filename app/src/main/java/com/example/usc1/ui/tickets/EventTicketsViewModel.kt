package com.example.usc1.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseEventTicketsRepository
import com.example.usc1.domain.repository.EventTicketsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventTicketsViewModel(
    private val ticketsRepository: EventTicketsRepository = SupabaseEventTicketsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventTicketsUiState.loading())
    val uiState: StateFlow<EventTicketsUiState> = _uiState.asStateFlow()
    private var lastLoadKey: String = ""

    fun loadTickets(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = EventTicketsUiState(tickets = emptyList())
            return
        }
        if (!forceRefresh && key == lastLoadKey && !_uiState.value.isLoading) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.value = EventTicketsUiState.loading()
            runCatching {
                ticketsRepository.getTickets(tenantId = tenantId, userId = userId)
            }.onSuccess { tickets ->
                _uiState.value = EventTicketsUiState(tickets = tickets)
            }.onFailure { error ->
                _uiState.value = EventTicketsUiState(
                    errorMessage = error.message ?: "Não foi possível carregar seus ingressos.",
                )
            }
        }
    }
}

class EventTicketDetailViewModel(
    private val ticketsRepository: EventTicketsRepository = SupabaseEventTicketsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventTicketDetailUiState(isLoading = true))
    val uiState: StateFlow<EventTicketDetailUiState> = _uiState.asStateFlow()

    fun loadTicket(
        session: UserSession,
        ticketId: String,
    ) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || ticketId.isBlank()) {
            _uiState.value = EventTicketDetailUiState(errorMessage = "Ingresso não encontrado.")
            return
        }
        viewModelScope.launch {
            _uiState.value = EventTicketDetailUiState(isLoading = true)
            runCatching {
                ticketsRepository.getTicketById(
                    tenantId = tenantId,
                    userId = userId,
                    ticketId = ticketId,
                )
            }.onSuccess { ticket ->
                _uiState.value = if (ticket == null) {
                    EventTicketDetailUiState(errorMessage = "Ingresso não encontrado.")
                } else {
                    EventTicketDetailUiState(ticket = ticket)
                }
            }.onFailure { error ->
                _uiState.value = EventTicketDetailUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o ingresso.",
                )
            }
        }
    }
}
