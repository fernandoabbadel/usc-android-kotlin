package com.example.usc1.data.repository

import com.example.usc1.domain.model.EventOrder
import com.example.usc1.domain.model.OrderStatus
import com.example.usc1.domain.model.PaymentStatus
import com.example.usc1.domain.repository.EventOrdersRepository
import kotlinx.coroutines.delay

class MockEventOrdersRepository : EventOrdersRepository {
    override suspend fun getOrders(): List<EventOrder> {
        delay(MockDelayMillis)
        return mockOrders
    }

    override suspend fun getOrderById(orderId: String): EventOrder? {
        delay(MockDelayMillis)
        return mockOrders.firstOrNull { it.id == orderId }
    }

    companion object {
        private const val MockDelayMillis = 180L

        val mockOrders = listOf(
            EventOrder(
                id = "order-evt-001",
                eventId = "intermed-2026",
                eventTitle = "Intermed USC",
                status = OrderStatus.Pending,
                paymentStatus = PaymentStatus.WaitingPayment,
                approvalStatus = "Aguardando aprovação",
                amountLabel = "R$ 90,00",
                quantity = 2,
                createdAtLabel = "Hoje, 10:42",
                lotName = "Lote 2",
            ),
            EventOrder(
                id = "order-evt-002",
                eventId = "festa-junina",
                eventTitle = "Festa Julina da Atlética",
                status = OrderStatus.Approved,
                paymentStatus = PaymentStatus.Paid,
                approvalStatus = "Aprovado pela diretoria",
                amountLabel = "R$ 35,00",
                quantity = 1,
                createdAtLabel = "28 jun 2026",
                lotName = "Lote final",
            ),
            EventOrder(
                id = "order-evt-003",
                eventId = "treino-aberto",
                eventTitle = "Treino aberto de futsal",
                status = OrderStatus.Cancelled,
                paymentStatus = PaymentStatus.Cancelled,
                approvalStatus = "Cancelado pelo usuário",
                amountLabel = "R$ 0,00",
                quantity = 1,
                createdAtLabel = "11 jun 2026",
                lotName = "Inscrição gratuita",
            ),
        )
    }
}
