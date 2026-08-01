package com.example.usc1.domain.repository

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventPartyOrder
import com.example.usc1.domain.model.EventPaymentRecipient
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.model.EventTicketOrder

interface EventsRepository {
    suspend fun getEvents(status: EventStatus? = null): List<Event>
    suspend fun getEventById(
        eventId: String,
        userId: String = "",
        userPlanNames: List<String> = emptyList(),
        userPlanIds: List<String> = emptyList(),
    ): Event?
    suspend fun createTicketRequest(
        tenantId: String,
        userId: String,
        userName: String,
        userTurma: String,
        event: Event,
        lot: EventProduct,
        quantity: Int,
        userPlanNames: List<String> = emptyList(),
        userPlanIds: List<String> = emptyList(),
        recipient: EventPaymentRecipient? = null,
    ): String

    suspend fun createEventProductOrder(
        tenantId: String,
        userId: String,
        userName: String,
        event: Event,
        product: EventMenuProduct,
        quantity: Int,
        userPlanNames: List<String> = emptyList(),
        userPlanIds: List<String> = emptyList(),
    ): String

    suspend fun setEventRsvp(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        status: EventRsvpStatus,
    )

    suspend fun createEventComment(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        text: String,
    ): String

    suspend fun voteEventPollOption(
        tenantId: String,
        userId: String,
        userTurma: String,
        eventId: String,
        pollId: String,
        optionIndex: Int,
    )

    /** Bloco "Seus Pedidos" de `/eventos/[id]`: pedidos de ingresso do próprio usuário. */
    suspend fun getViewerTicketOrders(
        tenantId: String,
        userId: String,
        eventId: String,
        limit: Int = 20,
    ): List<EventTicketOrder>

    suspend fun cancelTicketRequest(tenantId: String, requestId: String)

    suspend fun toggleEventCommentLike(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    )

    suspend fun reportEventComment(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    )

    suspend fun deleteEventComment(tenantId: String, eventId: String, commentId: String)

    suspend fun setEventCommentHidden(
        tenantId: String,
        eventId: String,
        commentId: String,
        hidden: Boolean,
    )

    suspend fun addEventPollOption(
        tenantId: String,
        eventId: String,
        pollId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        text: String,
    )

    /** Tela "Minhas fichas" (`/eventos/[id]/produtos/fichas`). */
    suspend fun getViewerEventPartyOrders(
        tenantId: String,
        userId: String,
        eventId: String,
    ): List<EventPartyOrder>
}
