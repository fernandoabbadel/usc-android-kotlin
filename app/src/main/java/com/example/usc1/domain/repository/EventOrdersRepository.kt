package com.example.usc1.domain.repository

import com.example.usc1.domain.model.EventOrder

interface EventOrdersRepository {
    suspend fun getOrders(): List<EventOrder>
    suspend fun getOrderById(orderId: String): EventOrder?
}
