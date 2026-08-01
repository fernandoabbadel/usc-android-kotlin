package com.example.usc1.domain.repository

import com.example.usc1.domain.model.UserOrder
import com.example.usc1.domain.model.UserOrderFinanceConfig
import com.example.usc1.domain.model.UserOrderTab

interface UserOrdersRepository {
    suspend fun getOrders(
        tenantId: String,
        userId: String,
        tab: UserOrderTab,
        maxResults: Int = 90,
    ): List<UserOrder>

    suspend fun getFinanceConfig(tenantId: String): UserOrderFinanceConfig
}
