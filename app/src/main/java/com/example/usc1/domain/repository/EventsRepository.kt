package com.example.usc1.domain.repository

import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventStatus

interface EventsRepository {
    suspend fun getEvents(status: EventStatus? = null): List<Event>
    suspend fun getEventById(eventId: String): Event?
}
