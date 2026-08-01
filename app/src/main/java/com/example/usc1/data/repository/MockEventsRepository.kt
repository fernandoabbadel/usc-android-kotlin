package com.example.usc1.data.repository

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventPartyOrder
import com.example.usc1.domain.model.EventPaymentRecipient
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.model.EventTicketOrder
import com.example.usc1.domain.repository.EventsRepository
import kotlinx.coroutines.delay

class MockEventsRepository : EventsRepository {
    override suspend fun getEvents(status: EventStatus?): List<Event> {
        delay(MockDelayMillis)
        return if (status == null) {
            mockEvents
        } else {
            mockEvents.filter { it.status == status }
        }
    }

    override suspend fun getEventById(
        eventId: String,
        userId: String,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
    ): Event? {
        delay(MockDelayMillis)
        return mockEvents.firstOrNull { it.id == eventId }
    }

    override suspend fun createTicketRequest(
        tenantId: String,
        userId: String,
        userName: String,
        userTurma: String,
        event: Event,
        lot: EventProduct,
        quantity: Int,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
        recipient: EventPaymentRecipient?,
    ): String {
        delay(MockDelayMillis)
        return "mock-ticket-request-${event.id}-${lot.id}"
    }

    override suspend fun createEventProductOrder(
        tenantId: String,
        userId: String,
        userName: String,
        event: Event,
        product: EventMenuProduct,
        quantity: Int,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
    ): String {
        delay(MockDelayMillis)
        return "mock-event-product-order-${event.id}-${product.id}"
    }

    override suspend fun setEventRsvp(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        status: EventRsvpStatus,
    ) {
        delay(MockDelayMillis)
    }

    override suspend fun createEventComment(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        text: String,
    ): String {
        delay(MockDelayMillis)
        return "mock-event-comment-$eventId"
    }

    override suspend fun voteEventPollOption(
        tenantId: String,
        userId: String,
        userTurma: String,
        eventId: String,
        pollId: String,
        optionIndex: Int,
    ) {
        delay(MockDelayMillis)
    }

    // As leituras e escritas abaixo só existem contra o Supabase; o mock não inventa dado.
    override suspend fun getViewerTicketOrders(
        tenantId: String,
        userId: String,
        eventId: String,
        limit: Int,
    ): List<EventTicketOrder> = emptyList()

    override suspend fun cancelTicketRequest(tenantId: String, requestId: String): Unit =
        throw UnsupportedOperationException("Repositório mock não cancela pedidos.")

    override suspend fun toggleEventCommentLike(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    ): Unit = throw UnsupportedOperationException("Repositório mock não curte comentários.")

    override suspend fun reportEventComment(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    ): Unit = throw UnsupportedOperationException("Repositório mock não denuncia comentários.")

    override suspend fun deleteEventComment(
        tenantId: String,
        eventId: String,
        commentId: String,
    ): Unit = throw UnsupportedOperationException("Repositório mock não apaga comentários.")

    override suspend fun setEventCommentHidden(
        tenantId: String,
        eventId: String,
        commentId: String,
        hidden: Boolean,
    ): Unit = throw UnsupportedOperationException("Repositório mock não oculta comentários.")

    override suspend fun addEventPollOption(
        tenantId: String,
        eventId: String,
        pollId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        text: String,
    ): Unit = throw UnsupportedOperationException("Repositório mock não edita enquetes.")

    override suspend fun getViewerEventPartyOrders(
        tenantId: String,
        userId: String,
        eventId: String,
    ): List<EventPartyOrder> = emptyList()

    companion object {
        private const val MockDelayMillis = 220L

        val mockEvents = listOf(
            Event(
                id = "intermed-2026",
                title = "Intermed USC",
                description = "Evento esportivo com jogos, integração das turmas, festa de encerramento e retirada de fichas no dia.",
                dateLabel = "18 jul 2026",
                timeLabel = "18:00",
                location = "Ginásio principal",
                priceLabel = "A partir de R$ 45,00",
                status = EventStatus.Open,
                coverColorName = "Verde USC",
                lotName = "Lote 2",
                availableSpots = 124,
                products = listOf(
                    EventProduct("ficha-cerveja", "Ficha cerveja", "R$ 12,00", "Disponível"),
                    EventProduct("combo-festa", "Combo festa", "R$ 35,00", "Disponível"),
                ),
            ),
            Event(
                id = "calourada-2026",
                title = "Calourada USC",
                description = "Recepção dos calouros com música, jogos e ativações da atlética.",
                dateLabel = "02 ago 2026",
                timeLabel = "16:00",
                location = "Arena USC",
                priceLabel = "Em breve",
                status = EventStatus.ComingSoon,
                coverColorName = "Dourado",
                lotName = "Abertura em breve",
                availableSpots = 300,
            ),
            Event(
                id = "festa-junina",
                title = "Festa Julina da Atlética",
                description = "Arraiá da atlética com comidas típicas, música e pontuação para turmas.",
                dateLabel = "28 jun 2026",
                timeLabel = "19:30",
                location = "Pátio central",
                priceLabel = "Esgotado",
                status = EventStatus.SoldOut,
                coverColorName = "Vermelho",
                lotName = "Lote final",
                availableSpots = 0,
            ),
            Event(
                id = "treino-aberto",
                title = "Treino aberto de futsal",
                description = "Treino aberto para membros ativos e visitantes convidados.",
                dateLabel = "11 jun 2026",
                timeLabel = "07:30",
                location = "Quadra externa",
                priceLabel = "Grátis",
                status = EventStatus.Closed,
                coverColorName = "Azul",
                lotName = "Inscrição encerrada",
                availableSpots = 0,
            ),
        )
    }
}
