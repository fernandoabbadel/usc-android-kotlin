package com.example.usc1.domain.repository

import com.example.usc1.ui.store.StoreOrder

interface StoreOrdersRepository {
    suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int = 80,
    ): List<StoreOrder>

    suspend fun getOrderById(
        tenantId: String,
        userId: String,
        orderId: String,
    ): StoreOrder?
}
