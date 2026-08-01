package com.example.usc1.domain.model

data class Event(
    val id: String,
    val tenantId: String = "",
    val title: String,
    val description: String,
    val dateLabel: String,
    val timeLabel: String,
    val rawDate: String = "",
    val rawTime: String = "",
    val location: String,
    val priceLabel: String,
    val status: EventStatus,
    val saleStatus: String = "ativo",
    val imageUrl: String? = null,
    val coverColorName: String,
    val lotName: String,
    val availableSpots: Int,
    val ownerType: EventOwnerType = EventOwnerType.Tenant,
    val ownerId: String = "",
    val ownerName: String = "",
    val likesCount: Int = 0,
    val confirmedCount: Int = 0,
    val maybeCount: Int = 0,
    val visibility: EventVisibility = EventVisibility.Public,
    val isHighlighted: Boolean = false,
    val isLowStock: Boolean = false,
    val topTurmas: List<String> = emptyList(),
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val receiptContactName: String = "",
    val receiptContactWhatsapp: String = "",
    /** `payment_config.recipients` do web: opções de quem recebe o comprovante. */
    val receiptRecipients: List<EventPaymentRecipient> = emptyList(),
    val products: List<EventProduct> = emptyList(),
    val isEventMenuEnabled: Boolean = false,
    val eventMenuTitle: String = "Menu do evento",
    val eventMenuCategory: String = "Menu do evento",
    val menuProducts: List<EventMenuProduct> = emptyList(),
    val viewerRsvpStatus: EventRsvpStatus? = null,
    val rsvps: List<EventRsvp> = emptyList(),
    val comments: List<EventComment> = emptyList(),
    val polls: List<EventPoll> = emptyList(),
)

data class EventPaymentRecipient(
    val userId: String = "",
    val name: String = "",
    val turma: String = "",
    val phone: String = "",
) {
    val displayLabel: String
        get() = listOf(name.trim().ifBlank { "Recebedor" }, turma.trim())
            .filter(String::isNotBlank)
            .joinToString(" - ")
}

enum class EventOwnerType(val remoteValue: String, val label: String) {
    Tenant("tenant", "Atlética"),
    Liga("liga", "Liga"),
    Comissao("comissao", "Comissão"),
    Diretorio("diretorio", "Diretório"),
}

enum class EventVisibility(val remoteValue: String, val label: String, val badge: String) {
    Public("public", "Público", "P"),
    Internal("internal", "Interno", "I"),
}

enum class EventStatus(val label: String) {
    Open("Aberto"),
    Closed("Encerrado"),
    SoldOut("Esgotado"),
    ComingSoon("Em breve"),
}

data class EventProduct(
    val id: String,
    val name: String,
    val priceValue: Double = 0.0,
    val priceLabel: String,
    val status: String,
    /** Preço de tabela quando o plano do usuário dá desconto no lote (`planPrices` do web). */
    val basePriceLabel: String = "",
    val planBenefitLabel: String = "",
) {
    val hasPlanDiscount: Boolean
        get() = basePriceLabel.isNotBlank()

    constructor(
        id: String,
        name: String,
        priceLabel: String,
        status: String,
    ) : this(
        id = id,
        name = name,
        priceValue = priceLabel
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull() ?: 0.0,
        priceLabel = priceLabel,
        status = status,
    )
}

data class EventMenuProduct(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val imageUrl: String? = null,
    val priceValue: Double = 0.0,
    val priceLabel: String,
    val status: String,
    val stockLabel: String,
    val stockCount: Int = 0,
    val orderIndex: Int = 9999,
)

enum class EventRsvpStatus(val remoteValue: String, val label: String) {
    Going("going", "Eu vou"),
    Maybe("maybe", "Talvez");

    companion object {
        fun fromRemote(value: String?): EventRsvpStatus? = when (value?.trim()?.lowercase()) {
            Going.remoteValue -> Going
            Maybe.remoteValue -> Maybe
            else -> null
        }
    }
}

data class EventRsvp(
    val id: String,
    val userId: String,
    val status: EventRsvpStatus,
    val userName: String,
    val userAvatar: String? = null,
    val userTurma: String = "",
    val timestampLabel: String = "",
)

data class EventComment(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val userTurma: String = "",
    val role: String = "",
    val text: String,
    val likesCount: Int = 0,
    val createdAtLabel: String = "",
    val likedByViewer: Boolean = false,
    val reportedByViewer: Boolean = false,
    val hidden: Boolean = false,
)

data class EventPollOption(
    val label: String,
    val votes: Int = 0,
    val votesByTurma: Map<String, Int> = emptyMap(),
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorAvatar: String = "",
)

data class EventPoll(
    val id: String,
    val question: String,
    val allowUserOptions: Boolean = false,
    val options: List<EventPollOption> = emptyList(),
    val viewerVotes: List<Int> = emptyList(),
    val votersCount: Int = 0,
    val createdAtLabel: String = "",
) {
    val totalVotes: Int
        get() = options.sumOf { it.votes }
}

/**
 * Pedido de ingresso do próprio usuário para um evento, como o bloco "Seus Pedidos"
 * de `/eventos/[id]` no web (tabela `solicitacoes_ingressos`).
 */
data class EventTicketOrder(
    val id: String,
    val eventId: String,
    val lotName: String,
    val quantity: Int,
    val totalLabel: String,
    val status: EventTicketOrderStatus,
    val requestedAtLabel: String,
    val approvedAtLabel: String = "",
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val receiptWhatsapp: String = "",
    val recipientName: String = "",
    val recipientTurma: String = "",
) {
    val shortCode: String
        get() = id.take(8).uppercase()
}

enum class EventTicketOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Confirmado"),
    Rejected("Cancelado");

    companion object {
        /** Espelha `normalizePedidoStatus` de `/eventos/[id]`. */
        fun fromRemote(value: String?): EventTicketOrderStatus =
            when (value?.trim()?.lowercase()) {
                "approved", "aprovado" -> Approved
                "rejected", "rejeitado", "cancelado" -> Rejected
                else -> Pending
            }
    }
}

/**
 * Ficha digital de um pedido de produto do evento, como `/eventos/[id]/produtos/fichas`
 * no web (`orders.data.eventParty.voucherEntries`).
 */
data class EventPartyVoucher(
    val id: String,
    val label: String,
    val status: EventPartyVoucherStatus,
    val code: String,
    val manualNumber: String = "",
    val qrPayload: String = "",
    val transferStatus: String = "",
    val transferredToUserName: String = "",
    val transferredFromUserName: String = "",
) {
    val transferNote: String
        get() = transferStatus.ifBlank {
            when {
                transferredToUserName.isNotBlank() -> "Transferido para $transferredToUserName"
                transferredFromUserName.isNotBlank() -> "Transferido de $transferredFromUserName"
                else -> ""
            }
        }
}

enum class EventPartyVoucherStatus(val label: String) {
    Pending("Pendente"),
    Active("Ativo"),
    Partial("Parcial"),
    Used("Utilizado"),
    Cancelled("Cancelado"),
    Transferred("Transferido"),
    Reversed("Estornado"),
    Refunded("Reembolsado"),
    Inactive("Utilizado");

    companion object {
        /** Espelha `normalizeVoucherEntryStatus` de `eventPartyService.ts`. */
        fun fromRemote(value: String?, fallback: EventPartyVoucherStatus = Pending): EventPartyVoucherStatus =
            when (value?.trim()?.lowercase()) {
                "ativo", "active", "liberado", "aprovado" -> Active
                "inativo", "used", "lido", "consumido", "retirado", "utilizado" -> Used
                "cancelado", "canceled", "cancelled", "rejected", "rejeitado" -> Cancelled
                "transferido", "transferred" -> Transferred
                "estornado", "refunded", "refund" -> Reversed
                "reembolsado" -> Refunded
                "pendente", "pending", "analise" -> Pending
                else -> fallback
            }
    }
}

/** Pedido de produto do evento com suas fichas, para a tela "Minhas fichas". */
data class EventPartyOrder(
    val id: String,
    val eventId: String,
    val productId: String,
    val productName: String,
    val productDescription: String = "",
    val quantity: Int,
    val totalLabel: String,
    val status: EventPartyVoucherStatus,
    val referenceSummary: String,
    val usedCount: Int = 0,
    val totalCount: Int = 0,
    val vouchers: List<EventPartyVoucher> = emptyList(),
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
    val holderTurma: String = "",
    val transferredToUserName: String = "",
    val transferredFromUserName: String = "",
) {
    /** O cartão público do web desativa o QR de ingresso transferido. */
    val isQrDisabled: Boolean
        get() = status == TicketStatus.Transferred || status == TicketStatus.Cancelled
}

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
    val itemType: EventOrderItemType = EventOrderItemType.Ticket,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val approvalStatus: String,
    val amountLabel: String,
    val quantity: Int,
    val createdAtLabel: String,
    val lotName: String,
)

enum class EventOrderItemType(val label: String, val unitLabel: String, val detailLabel: String) {
    Ticket("Ingresso", "ingresso(s)", "Lote"),
    EventProduct("Produto do evento", "produto(s)", "Produto"),
}

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
