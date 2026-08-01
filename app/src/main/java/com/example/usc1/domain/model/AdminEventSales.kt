package com.example.usc1.domain.model

data class AdminEventSalesDashboard(
    val events: List<AdminEventSalesEvent> = emptyList(),
    val orders: List<AdminEventSalesOrder> = emptyList(),
    val totalRevenueLabel: String = "R$ 0,00",
    val pendingRevenueLabel: String = "R$ 0,00",
    val approvedRevenueLabel: String = "R$ 0,00",
    val totalItems: Int = 0,
    val pendingOrders: Int = 0,
    val approvedOrders: Int = 0,
) {
    val hasOperation: Boolean
        get() = events.isNotEmpty() || orders.isNotEmpty()
}

data class AdminEventSalesEvent(
    val id: String,
    val title: String,
    val menuTitle: String,
    val category: String,
    val productCount: Int,
    val stockCount: Int,
    val statusLabel: String,
)

data class AdminEventSalesOrder(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val userName: String,
    val productId: String,
    val productName: String,
    val category: String,
    val quantity: Int,
    val totalValue: Double,
    val totalLabel: String,
    val status: AdminEventSalesOrderStatus,
    val approvalLabel: String,
    val receiverLabel: String,
    val sourceLabel: String,
    val voucherStatusLabel: String,
    val createdAtLabel: String,
)

enum class AdminEventSalesOrderStatus(val remoteValue: String, val label: String) {
    Pending("pending", "Pendente"),
    Approved("approved", "Aprovado"),
    Rejected("rejected", "Rejeitado"),
    Cancelled("cancelled", "Cancelado"),
    Delivered("delivered", "Retirado");

    companion object {
        fun fromRemote(value: String?): AdminEventSalesOrderStatus {
            return when (value?.trim()?.lowercase()) {
                "aprovado", "approved", "confirmado", "confirmed", "pago", "paid" -> Approved
                "rejeitado", "rejected" -> Rejected
                "cancelado", "cancelled", "canceled" -> Cancelled
                "delivered", "entregue", "retirado", "used" -> Delivered
                else -> Pending
            }
        }
    }
}
