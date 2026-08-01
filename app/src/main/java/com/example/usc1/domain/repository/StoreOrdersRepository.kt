package com.example.usc1.domain.repository

import com.example.usc1.ui.store.CartItem
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

    suspend fun createOrder(
        tenantId: String,
        userId: String,
        userName: String,
        item: CartItem,
        userPlanNames: List<String> = emptyList(),
        userPlanIds: List<String> = emptyList(),
    ): StoreOrder
}
