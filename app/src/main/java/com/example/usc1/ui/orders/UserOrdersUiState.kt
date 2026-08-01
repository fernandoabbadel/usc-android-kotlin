package com.example.usc1.ui.orders

import com.example.usc1.domain.model.UserOrder
import com.example.usc1.domain.model.UserOrderFinanceConfig
import com.example.usc1.domain.model.UserOrderPaymentConfig
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab

/** Página de 10 itens, igual a `PEDIDOS_PAGE_SIZE` do web. */
const val UserOrdersPageSize = 10

data class UserOrdersUiState(
    val tab: UserOrderTab = UserOrderTab.Eventos,
    val statusFilter: UserOrderStatus = UserOrderStatus.Pendente,
    val page: Int = 1,
    val orders: List<UserOrder> = emptyList(),
    val financeConfig: UserOrderFinanceConfig = UserOrderFinanceConfig(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val tenantBrandLabel: String = "Atlética",
    val buyerName: String = "",
    val buyerClass: String = "",
    val buyerPhone: String = "",
) {
    val approvedCount: Int get() = orders.count { it.status == UserOrderStatus.Aprovado }
    val pendingCount: Int get() = orders.count { it.status == UserOrderStatus.Pendente }
    val rejectedCount: Int get() = orders.count { it.status == UserOrderStatus.Rejeitado }

    val filteredOrders: List<UserOrder>
        get() = orders.filter { it.status == statusFilter }

    val totalPages: Int
        get() = maxOf(1, (filteredOrders.size + UserOrdersPageSize - 1) / UserOrdersPageSize)

    val paginatedOrders: List<UserOrder>
        get() {
            val start = (minOf(page, totalPages) - 1) * UserOrdersPageSize
            return filteredOrders.drop(start).take(UserOrdersPageSize)
        }

    fun orderById(orderId: String): UserOrder? = orders.firstOrNull { it.id == orderId }

    /**
     * Espelha `resolvePedidoPaymentConfig`: o `payment_config` do pedido tem prioridade,
     * depois o financeiro do tenant, depois o fallback de marca.
     */
    fun resolvePaymentConfig(order: UserOrder?): UserOrderPaymentConfig {
        val raw = order?.paymentConfig ?: UserOrderPaymentConfig()
        return raw.copy(
            pixKey = raw.pixKey.ifBlank { financeConfig.pixKey }
                .ifBlank { "financeiro@atletica.com.br" },
            bank = raw.bank.ifBlank { financeConfig.bank }.ifBlank { "Banco da Atlética" },
            holder = raw.holder.ifBlank { financeConfig.holder }.ifBlank { tenantBrandLabel },
            whatsapp = raw.whatsapp.ifBlank { financeConfig.whatsapp },
        )
    }

    /** `resolveReceiptContactProfile`: nome/turma de quem recebe o comprovante. */
    fun resolveRecipientName(order: UserOrder?): String {
        val recipient = order?.paymentConfig?.recipientName.orEmpty().trim()
        return recipient.ifBlank { tenantBrandLabel }
    }

    fun resolveRecipientClass(order: UserOrder?): String {
        val recipient = order?.paymentConfig?.recipientClass.orEmpty().trim()
        return recipient.ifBlank { "Financeiro" }
    }
}
