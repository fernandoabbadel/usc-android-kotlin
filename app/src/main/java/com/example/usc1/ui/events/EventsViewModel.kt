package com.example.usc1.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseEventsRepository
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventsViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState.loading())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents(status: EventStatus? = _uiState.value.selectedStatus) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedStatus = status,
                )
            }
            try {
                val events = eventsRepository.getEvents(status)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        events = events,
                    )
                }
            } catch (error: Throwable) {
                _uiState.value = EventsUiState.error(error.message)
            }
        }
    }
}

class EventDetailViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventDetailUiState(isLoading = true))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = EventDetailUiState(isLoading = true)
            try {
                val event = eventsRepository.getEventById(eventId)
                _uiState.value = if (event == null) {
                    EventDetailUiState(errorMessage = "Evento não encontrado no tenant ativo.")
                } else {
                    EventDetailUiState(event = event)
                }
            } catch (error: Throwable) {
                _uiState.value = EventDetailUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o evento.",
                )
            }
        }
    }
}
