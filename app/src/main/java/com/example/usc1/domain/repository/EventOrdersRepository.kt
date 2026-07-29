package com.example.usc1.domain.repository

import com.example.usc1.domain.model.EventOrder

interface EventOrdersRepository {
    suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int = 80,
    ): List<EventOrder>

    suspend fun getOrderById(
        tenantId: String,
        userId: String,
        orderId: String,
    ): EventOrder?
}
