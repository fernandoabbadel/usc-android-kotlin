package com.example.usc1.ui.events

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventComment
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventOwnerType
import com.example.usc1.domain.model.EventPartyOrder
import com.example.usc1.domain.model.EventTicketOrder
import com.example.usc1.domain.model.EventTicketOrderStatus

/** Limites do `/eventos/[id]` do web. */
const val EventCommentMaxChars = 280
const val EventPollOptionMaxChars = 60
const val EventPollOptionMaxCount = 20

enum class EventFeedFilter(val label: String) {
    All("Todos"),
    Tenant("Eventos Atlética"),
    Leagues("Eventos Ligas"),
    Directory("Eventos Diretório"),
    Commissions("Eventos Comissões"),
}

data class EventsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFilter: EventFeedFilter = EventFeedFilter.All,
    val events: List<Event> = emptyList(),
) {
    val filteredEvents: List<Event>
        get() = when (selectedFilter) {
            EventFeedFilter.All -> events
            EventFeedFilter.Tenant -> events.filter { it.ownerType == EventOwnerType.Tenant }
            EventFeedFilter.Leagues -> events.filter { it.ownerType == EventOwnerType.Liga }
            EventFeedFilter.Directory -> events.filter { it.ownerType == EventOwnerType.Diretorio }
            EventFeedFilter.Commissions -> events.filter { it.ownerType == EventOwnerType.Comissao }
        }

    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && filteredEvents.isEmpty()

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
    val isSubmittingTicketRequest: Boolean = false,
    val ticketRequestError: String? = null,
    val createdTicketRequestId: String? = null,
    val isSubmittingMenuProductOrder: Boolean = false,
    val menuProductOrderError: String? = null,
    val createdMenuProductOrderId: String? = null,
    val isSubmittingRsvp: Boolean = false,
    val rsvpError: String? = null,
    val commentDraft: String = "",
    val isSubmittingComment: Boolean = false,
    val commentError: String? = null,
    val votingPollId: String? = null,
    val pollActionError: String? = null,
    val pollOptionDraft: String = "",
    val isSubmittingPollOption: Boolean = false,
    val commentActionId: String? = null,
    val commentActionError: String? = null,
    val ticketOrders: List<EventTicketOrder> = emptyList(),
    val isLoadingTicketOrders: Boolean = false,
    val ticketOrdersError: String? = null,
    val cancellingOrderId: String? = null,
    val viewerIsAdmin: Boolean = false,
) {
    /** Bloco "Pendentes" do web em `/eventos/[id]`. */
    val pendingTicketOrders: List<EventTicketOrder>
        get() = ticketOrders.filter { it.status == EventTicketOrderStatus.Pending }

    /** Bloco "Finalizados" do web em `/eventos/[id]`. */
    val historyTicketOrders: List<EventTicketOrder>
        get() = ticketOrders.filter { it.status != EventTicketOrderStatus.Pending }

    /** O web só esconde comentários ocultos de quem não é admin. */
    val visibleComments: List<EventComment>
        get() = event?.comments.orEmpty().filter { viewerIsAdmin || !it.hidden }
}

/** `/eventos/[id]/produtos` — menu do evento. */
data class EventPartyMenuUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val event: Event? = null,
) {
    val isMenuDisabled: Boolean
        get() = event != null && !event.isEventMenuEnabled
}

/** `/eventos/[id]/produtos/[productId]` — ficha do produto do evento. */
data class EventPartyProductUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val event: Event? = null,
    val product: EventMenuProduct? = null,
    val quantity: Int = 1,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val createdOrderId: String? = null,
)

/** `/eventos/[id]/produtos/fichas` — Minhas fichas. */
data class EventPartyVouchersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val event: Event? = null,
    val orders: List<EventPartyOrder> = emptyList(),
)
