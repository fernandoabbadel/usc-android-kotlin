package com.example.usc1.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseEventsRepository
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventPaymentRecipient
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Escopo de plano do usuário, como `collectUserPlanScope` no web. */
internal fun UserSession?.planNames(): List<String> {
    val user = this?.user ?: return emptyList()
    return listOf(user.planName, user.planBadge, user.role.name)
        .map(String::trim)
        .filter(String::isNotBlank)
}

internal fun UserSession?.planIds(): List<String> {
    val user = this?.user ?: return emptyList()
    return listOf(user.planBadge, user.role.name)
        .map(String::trim)
        .filter(String::isNotBlank)
}

class EventsViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState.loading())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents(filter: EventFeedFilter = _uiState.value.selectedFilter) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedFilter = filter,
                )
            }
            try {
                val events = eventsRepository.getEvents()
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

    fun selectFilter(filter: EventFeedFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}

class EventDetailViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventDetailUiState(isLoading = true))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String, session: UserSession? = null) {
        viewModelScope.launch {
            val viewerIsAdmin = session?.user?.role?.isAdminLike == true
            _uiState.value = EventDetailUiState(isLoading = true, viewerIsAdmin = viewerIsAdmin)
            try {
                val event = eventsRepository.getEventById(
                    eventId = eventId,
                    userId = session?.user?.id.orEmpty(),
                    userPlanNames = session.planNames(),
                    userPlanIds = session.planIds(),
                )
                _uiState.value = if (event == null) {
                    EventDetailUiState(
                        errorMessage = "Evento não encontrado no tenant ativo.",
                        viewerIsAdmin = viewerIsAdmin,
                    )
                } else {
                    EventDetailUiState(event = event, viewerIsAdmin = viewerIsAdmin)
                }
                if (event != null && session?.user != null) {
                    loadTicketOrders(event.id, session)
                }
            } catch (error: Throwable) {
                _uiState.value = EventDetailUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o evento.",
                    viewerIsAdmin = viewerIsAdmin,
                )
            }
        }
    }

    /** Bloco "Seus Pedidos" da ficha do evento no web. */
    private fun loadTicketOrders(eventId: String, session: UserSession) {
        val user = session.user ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { _uiState.value.event?.tenantId.orEmpty() }
        if (tenantId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTicketOrders = true, ticketOrdersError = null) }
            try {
                val orders = eventsRepository.getViewerTicketOrders(
                    tenantId = tenantId,
                    userId = user.id,
                    eventId = eventId,
                )
                _uiState.update { it.copy(isLoadingTicketOrders = false, ticketOrders = orders) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoadingTicketOrders = false,
                        ticketOrdersError = error.message ?: "Não foi possível carregar seus pedidos.",
                    )
                }
            }
        }
    }

    fun cancelTicketOrder(session: UserSession, orderId: String) {
        val current = _uiState.value
        if (current.cancellingOrderId != null) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        if (tenantId.isBlank()) {
            _uiState.update { it.copy(ticketOrdersError = "Entre na atlética para cancelar o pedido.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cancellingOrderId = orderId, ticketOrdersError = null) }
            try {
                eventsRepository.cancelTicketRequest(tenantId = tenantId, requestId = orderId)
                _uiState.update {
                    it.copy(
                        cancellingOrderId = null,
                        ticketOrders = it.ticketOrders.filterNot { order -> order.id == orderId },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        cancellingOrderId = null,
                        ticketOrdersError = error.message ?: "Não foi possível cancelar o pedido.",
                    )
                }
            }
        }
    }

    fun toggleCommentLike(session: UserSession, commentId: String) =
        runCommentAction(session, commentId, "Não foi possível curtir o comentário.") { tenantId, eventId, userId ->
            eventsRepository.toggleEventCommentLike(tenantId, eventId, commentId, userId)
        }

    fun reportComment(session: UserSession, commentId: String) =
        runCommentAction(session, commentId, "Não foi possível denunciar o comentário.") { tenantId, eventId, userId ->
            eventsRepository.reportEventComment(tenantId, eventId, commentId, userId)
        }

    fun deleteComment(session: UserSession, commentId: String) =
        runCommentAction(session, commentId, "Não foi possível apagar o comentário.") { tenantId, eventId, _ ->
            eventsRepository.deleteEventComment(tenantId, eventId, commentId)
        }

    fun setCommentHidden(session: UserSession, commentId: String, hidden: Boolean) =
        runCommentAction(session, commentId, "Não foi possível atualizar o comentário.") { tenantId, eventId, _ ->
            eventsRepository.setEventCommentHidden(tenantId, eventId, commentId, hidden)
        }

    private fun runCommentAction(
        session: UserSession,
        commentId: String,
        failureMessage: String,
        action: suspend (tenantId: String, eventId: String, userId: String) -> Unit,
    ) {
        val current = _uiState.value
        if (current.commentActionId != null) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(commentActionError = "Entre na atlética para interagir no mural.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(commentActionId = commentId, commentActionError = null) }
            try {
                action(tenantId, event.id, user.id)
                loadEvent(event.id, session)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        commentActionId = null,
                        commentActionError = error.message ?: failureMessage,
                    )
                }
            }
        }
    }

    fun updatePollOptionDraft(value: String) {
        _uiState.update {
            it.copy(
                pollOptionDraft = value.take(EventPollOptionMaxChars),
                pollActionError = null,
            )
        }
    }

    fun submitPollOption(session: UserSession, pollId: String) {
        val current = _uiState.value
        if (current.isSubmittingPollOption) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        val text = current.pollOptionDraft.trim()
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(pollActionError = "Entre na atlética para sugerir uma resposta.") }
            return
        }
        if (text.isBlank()) {
            _uiState.update { it.copy(pollActionError = "Escreva a resposta antes de enviar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPollOption = true, pollActionError = null) }
            try {
                eventsRepository.addEventPollOption(
                    tenantId = tenantId,
                    eventId = event.id,
                    pollId = pollId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    userAvatar = user.avatarUrl.orEmpty(),
                    userTurma = user.classCode.ifBlank { "Geral" },
                    text = text,
                )
                _uiState.update { it.copy(isSubmittingPollOption = false, pollOptionDraft = "") }
                loadEvent(event.id, session)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingPollOption = false,
                        pollActionError = error.message ?: "Não foi possível adicionar a resposta.",
                    )
                }
            }
        }
    }

    fun updateCommentDraft(value: String) {
        _uiState.update {
            it.copy(
                commentDraft = value.take(EventCommentMaxChars),
                commentError = null,
            )
        }
    }

    fun setRsvp(
        session: UserSession,
        status: EventRsvpStatus,
    ) {
        val current = _uiState.value
        if (current.isSubmittingRsvp) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(rsvpError = "Entre na atlética para responder presença.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRsvp = true, rsvpError = null) }
            try {
                eventsRepository.setEventRsvp(
                    tenantId = tenantId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    userAvatar = user.avatarUrl.orEmpty(),
                    userTurma = user.classCode.ifBlank { "Geral" },
                    eventId = event.id,
                    status = status,
                )
                loadEvent(event.id, session)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingRsvp = false,
                        rsvpError = error.message ?: "Não foi possível atualizar presença.",
                    )
                }
            }
        }
    }

    fun submitComment(session: UserSession) {
        val current = _uiState.value
        if (current.isSubmittingComment) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        val text = current.commentDraft.trim()
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(commentError = "Entre na atlética para comentar.") }
            return
        }
        if (text.isBlank()) {
            _uiState.update { it.copy(commentError = "Escreva uma mensagem para publicar no mural.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true, commentError = null) }
            try {
                eventsRepository.createEventComment(
                    tenantId = tenantId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    userAvatar = user.avatarUrl.orEmpty(),
                    userTurma = user.classCode.ifBlank { "Geral" },
                    eventId = event.id,
                    text = text,
                )
                loadEvent(event.id, session)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingComment = false,
                        commentError = error.message ?: "Não foi possível publicar no mural.",
                    )
                }
            }
        }
    }

    fun votePoll(
        session: UserSession,
        pollId: String,
        optionIndex: Int,
    ) {
        val current = _uiState.value
        if (current.votingPollId != null) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update { it.copy(pollActionError = "Entre na atlética para votar na enquete.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(votingPollId = pollId, pollActionError = null) }
            try {
                eventsRepository.voteEventPollOption(
                    tenantId = tenantId,
                    userId = user.id,
                    userTurma = user.classCode.ifBlank { "Geral" },
                    eventId = event.id,
                    pollId = pollId,
                    optionIndex = optionIndex,
                )
                loadEvent(event.id, session)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        votingPollId = null,
                        pollActionError = error.message ?: "Não foi possível registrar o voto.",
                    )
                }
            }
        }
    }

    fun submitTicketRequest(
        session: UserSession,
        lot: EventProduct,
        quantity: Int,
        recipient: EventPaymentRecipient? = null,
        onSuccess: (String) -> Unit,
    ) {
        val current = _uiState.value
        if (current.isSubmittingTicketRequest) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update {
                it.copy(ticketRequestError = "Entre na atlética para reservar ingresso.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingTicketRequest = true,
                    ticketRequestError = null,
                    createdTicketRequestId = null,
                )
            }
            try {
                val userPlanNames = session.planNames()
                val userPlanIds = session.planIds()
                val requestId = eventsRepository.createTicketRequest(
                    tenantId = tenantId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    userTurma = user.classCode.ifBlank { "Geral" },
                    event = event,
                    lot = lot,
                    quantity = quantity,
                    userPlanNames = userPlanNames,
                    userPlanIds = userPlanIds,
                    recipient = recipient,
                )
                _uiState.update {
                    it.copy(
                        isSubmittingTicketRequest = false,
                        ticketRequestError = null,
                        createdTicketRequestId = requestId,
                    )
                }
                onSuccess(requestId)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingTicketRequest = false,
                        ticketRequestError = error.message ?: "Não foi possível reservar o ingresso.",
                    )
                }
            }
        }
    }

    fun submitMenuProductOrder(
        session: UserSession,
        product: EventMenuProduct,
        quantity: Int,
        onSuccess: (String) -> Unit,
    ) {
        val current = _uiState.value
        if (current.isSubmittingMenuProductOrder) return
        val event = current.event ?: return
        val tenantId = session.tenant?.id.orEmpty().ifBlank { event.tenantId }
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            _uiState.update {
                it.copy(menuProductOrderError = "Entre na atlética para comprar no evento.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingMenuProductOrder = true,
                    menuProductOrderError = null,
                    createdMenuProductOrderId = null,
                )
            }
            try {
                val userPlanNames = session.planNames()
                val userPlanIds = session.planIds()
                val orderId = eventsRepository.createEventProductOrder(
                    tenantId = tenantId,
                    userId = user.id,
                    userName = user.name.ifBlank { user.email },
                    event = event,
                    product = product,
                    quantity = quantity,
                    userPlanNames = userPlanNames,
                    userPlanIds = userPlanIds,
                )
                _uiState.update {
                    it.copy(
                        isSubmittingMenuProductOrder = false,
                        menuProductOrderError = null,
                        createdMenuProductOrderId = orderId,
                    )
                }
                onSuccess(orderId)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingMenuProductOrder = false,
                        menuProductOrderError = error.message ?: "Não foi possível criar o pedido do evento.",
                    )
                }
            }
        }
    }
}
