package com.example.usc1.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseEventsRepository
import com.example.usc1.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** `/eventos/[id]/produtos` — grade de produtos do menu do evento. */
class EventPartyMenuViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventPartyMenuUiState())
    val uiState: StateFlow<EventPartyMenuUiState> = _uiState.asStateFlow()

    fun load(eventId: String, session: UserSession? = null) {
        viewModelScope.launch {
            _uiState.value = EventPartyMenuUiState(isLoading = true)
            try {
                val event = eventsRepository.getEventById(
                    eventId = eventId,
                    userId = session?.user?.id.orEmpty(),
                    userPlanNames = session.planNames(),
                    userPlanIds = session.planIds(),
                )
                _uiState.value = if (event == null) {
                    EventPartyMenuUiState(
                        isLoading = false,
                        errorMessage = "Evento não encontrado no tenant ativo.",
                    )
                } else {
                    EventPartyMenuUiState(isLoading = false, event = event)
                }
            } catch (error: Throwable) {
                _uiState.value = EventPartyMenuUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao carregar menu do evento.",
                )
            }
        }
    }
}

/** `/eventos/[id]/produtos/[productId]` — ficha do produto com pedido. */
class EventPartyProductViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventPartyProductUiState())
    val uiState: StateFlow<EventPartyProductUiState> = _uiState.asStateFlow()

    fun load(eventId: String, productId: String, session: UserSession? = null) {
        viewModelScope.launch {
            _uiState.value = EventPartyProductUiState(isLoading = true)
            try {
                val event = eventsRepository.getEventById(
                    eventId = eventId,
                    userId = session?.user?.id.orEmpty(),
                    userPlanNames = session.planNames(),
                    userPlanIds = session.planIds(),
                )
                val product = event?.menuProducts?.firstOrNull { it.id == productId }
                _uiState.value = when {
                    event == null -> EventPartyProductUiState(
                        isLoading = false,
                        errorMessage = "Evento não encontrado no tenant ativo.",
                    )
                    product == null -> EventPartyProductUiState(
                        isLoading = false,
                        event = event,
                        errorMessage = "Produto indisponível neste evento.",
                    )
                    else -> EventPartyProductUiState(isLoading = false, event = event, product = product)
                }
            } catch (error: Throwable) {
                _uiState.value = EventPartyProductUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao carregar produto do evento.",
                )
            }
        }
    }

    fun changeQuantity(delta: Int) {
        _uiState.update { state ->
            val stockLimit = state.product?.stockCount?.takeIf { it > 0 } ?: MaxQuantity
            state.copy(quantity = (state.quantity + delta).coerceIn(1, minOf(stockLimit, MaxQuantity)))
        }
    }

    fun submitOrder(session: UserSession, onSuccess: (String) -> Unit) {
        val current = _uiState.value
        if (current.isSubmitting) return
        val event = current.event ?: return
        val product = current.product ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(submitError = "Entre na atlética para comprar no evento.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null, createdOrderId = null) }
            try {
                val userPlanNames = session.planNames()
                val userPlanIds = session.planIds()
                val orderId = eventsRepository.createEventProductOrder(
                    tenantId = tenantId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    event = event,
                    product = product,
                    quantity = current.quantity,
                    userPlanNames = userPlanNames,
                    userPlanIds = userPlanIds,
                )
                _uiState.update { it.copy(isSubmitting = false, createdOrderId = orderId) }
                onSuccess(orderId)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = error.message ?: "Não foi possível criar o pedido do evento.",
                    )
                }
            }
        }
    }

    private companion object {
        const val MaxQuantity = 10
    }
}

/** `/eventos/[id]/produtos/fichas` — fichas digitais compradas pelo usuário. */
class EventPartyVouchersViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventPartyVouchersUiState())
    val uiState: StateFlow<EventPartyVouchersUiState> = _uiState.asStateFlow()

    fun load(eventId: String, session: UserSession?) {
        viewModelScope.launch {
            _uiState.value = EventPartyVouchersUiState(isLoading = true)
            val user = session?.user
            if (user == null) {
                _uiState.value = EventPartyVouchersUiState(
                    isLoading = false,
                    errorMessage = "Entre para ver suas fichas.",
                )
                return@launch
            }
            try {
                val event = eventsRepository.getEventById(
                    eventId = eventId,
                    userId = user.id,
                    userPlanNames = session.planNames(),
                    userPlanIds = session.planIds(),
                )
                if (event == null) {
                    _uiState.value = EventPartyVouchersUiState(
                        isLoading = false,
                        errorMessage = "Evento não encontrado no tenant ativo.",
                    )
                    return@launch
                }
                val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
                val orders = eventsRepository.getViewerEventPartyOrders(
                    tenantId = tenantId,
                    userId = user.id,
                    eventId = event.id,
                )
                _uiState.value = EventPartyVouchersUiState(
                    isLoading = false,
                    event = event,
                    orders = orders,
                )
            } catch (error: Throwable) {
                _uiState.value = EventPartyVouchersUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao carregar fichas do evento.",
                )
            }
        }
    }
}
