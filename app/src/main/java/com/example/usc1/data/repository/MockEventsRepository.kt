package com.example.usc1.data.repository

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventStatus
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

    override suspend fun getEventById(eventId: String): Event? {
        delay(MockDelayMillis)
        return mockEvents.firstOrNull { it.id == eventId }
    }

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
                title = "Calourada AAAKN",
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
