package com.example.usc1.domain.model

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val timeLabel: String,
    val location: String,
    val priceLabel: String,
    val status: EventStatus,
    val coverColorName: String,
    val lotName: String,
    val availableSpots: Int,
    val products: List<EventProduct> = emptyList(),
)

enum class EventStatus(val label: String) {
    Open("Aberto"),
    Closed("Encerrado"),
    SoldOut("Esgotado"),
    ComingSoon("Em breve"),
}

data class EventProduct(
    val id: String,
    val name: String,
    val priceLabel: String,
    val status: String,
)

data class EventTicket(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val holderName: String,
    val status: TicketStatus,
    val token: String,
    val lotName: String,
    val dateLabel: String,
    val qrPayload: String,
    val transferAvailable: Boolean,
)

enum class TicketStatus(val label: String) {
    Active("Ativo"),
    Pending("Pendente"),
    Used("Utilizado"),
    Transferred("Transferido"),
    Cancelled("Cancelado"),
}

data class EventOrder(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val approvalStatus: String,
    val amountLabel: String,
    val quantity: Int,
    val createdAtLabel: String,
    val lotName: String,
)

enum class OrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Cancelled("Cancelado"),
    Rejected("Rejeitado"),
}

enum class PaymentStatus(val label: String) {
    WaitingPayment("Aguardando pagamento"),
    Paid("Pago"),
    Refunded("Reembolsado"),
    Cancelled("Cancelado"),
}
