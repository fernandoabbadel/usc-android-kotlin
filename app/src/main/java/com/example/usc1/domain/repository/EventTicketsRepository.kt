package com.example.usc1.domain.repository

import com.example.usc1.domain.model.EventTicket

interface EventTicketsRepository {
    suspend fun getTickets(): List<EventTicket>
    suspend fun getTicketById(ticketId: String): EventTicket?
}
