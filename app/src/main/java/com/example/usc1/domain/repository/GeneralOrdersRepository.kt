package com.example.usc1.domain.repository

import com.example.usc1.ui.generalorders.GeneralOrder

interface GeneralOrdersRepository {
    suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int = 80,
    ): List<GeneralOrder>
}
