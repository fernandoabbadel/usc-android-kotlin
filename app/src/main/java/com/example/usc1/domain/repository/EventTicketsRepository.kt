package com.example.usc1.domain.repository

import com.example.usc1.domain.model.EventTicket

interface EventTicketsRepository {
    suspend fun getTickets(
        tenantId: String,
        userId: String,
        limit: Int = 80,
    ): List<EventTicket>

    suspend fun getTicketById(
        tenantId: String,
        userId: String,
        ticketId: String,
    ): EventTicket?
}
