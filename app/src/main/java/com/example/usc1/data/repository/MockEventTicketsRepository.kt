package com.example.usc1.data.repository

import com.example.usc1.domain.model.EventTicket
import com.example.usc1.domain.model.TicketStatus
import com.example.usc1.domain.repository.EventTicketsRepository
import kotlinx.coroutines.delay

class MockEventTicketsRepository : EventTicketsRepository {
    override suspend fun getTickets(): List<EventTicket> {
        delay(MockDelayMillis)
        return mockTickets
    }

    override suspend fun getTicketById(ticketId: String): EventTicket? {
        delay(MockDelayMillis)
        return mockTickets.firstOrNull { it.id == ticketId }
    }

    companion object {
        private const val MockDelayMillis = 180L

        val mockTickets = listOf(
            EventTicket(
                id = "ticket-001",
                eventId = "intermed-2026",
                eventTitle = "Intermed USC",
                holderName = "Fernando USC",
                status = TicketStatus.Active,
                token = "EVT-INT-2026-001",
                lotName = "Lote 2",
                dateLabel = "18 jul 2026 • 18:00",
                qrPayload = "usc:event-ticket:EVT-INT-2026-001",
                transferAvailable = true,
            ),
            EventTicket(
                id = "ticket-002",
                eventId = "festa-junina",
                eventTitle = "Festa Julina da Atlética",
                holderName = "Fernando USC",
                status = TicketStatus.Used,
                token = "EVT-JUL-2026-442",
                lotName = "Lote final",
                dateLabel = "28 jun 2026 • 19:30",
                qrPayload = "usc:event-ticket:EVT-JUL-2026-442",
                transferAvailable = false,
            ),
        )
    }
}
