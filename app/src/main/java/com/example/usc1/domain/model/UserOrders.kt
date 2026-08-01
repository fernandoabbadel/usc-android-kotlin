package com.example.usc1.domain.model

/**
 * Espelho de `PedidoUnificado`
 * (`web-reference/src/app/configuracoes/pedidos/_components/PedidosByTypePage.tsx`).
 *
 * Cada aba lê uma tabela diferente, mas o card renderizado é o mesmo:
 * `eventos` -> `solicitacoes_ingressos`, `loja` -> `orders`, `planos` -> `solicitacoes_adesao`.
 */
enum class UserOrderTab(val slug: String, val title: String, val description: String) {
    Eventos("eventos", "Pedidos de Eventos", "Ingressos e lotes"),
    Loja("loja", "Pedidos da Loja", "Compras de produtos"),
    Planos("planos", "Pedidos de Planos", "Adesoes e status"),
    ;

    companion object {
        fun fromSlug(value: String?): UserOrderTab {
            val clean = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.slug == clean } ?: Eventos
        }
    }
}

enum class UserOrderStatus(val slug: String, val label: String) {
    Aprovado("aprovados", "Aprovado"),
    Pendente("pendentes", "Pendente"),
    Rejeitado("negados", "Negado"),
    ;

    companion object {
        fun fromSlug(value: String?): UserOrderStatus {
            return when (value?.trim()?.lowercase()) {
                "aprovados" -> Aprovado
                "negados", "rejeitados" -> Rejeitado
                else -> Pendente
            }
        }

        fun fromRemote(value: String?): UserOrderStatus {
            return when (value?.trim()?.lowercase()) {
                "approved", "aprovado" -> Aprovado
                "rejected", "rejeitado" -> Rejeitado
                else -> Pendente
            }
        }
    }
}

data class UserOrder(
    val id: String,
    val tab: UserOrderTab,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val status: UserOrderStatus,
    val createdAtMillis: Long,
    val createdAtLabel: String,
    val paymentConfig: UserOrderPaymentConfig = UserOrderPaymentConfig(),
    val sellerName: String = "",
    val sellerLogoUrl: String = "",
    val buyerName: String = "",
    val buyerClass: String = "",
    val quantity: Int = 1,
    val eventId: String = "",
    val selectedColor: String = "",
) {
    val shortCode: String
        get() = id.take(8).uppercase()
}

/** Espelho de `CommercePaymentConfig` — o que o pedido guarda em `payment_config`. */
data class UserOrderPaymentConfig(
    val pixKey: String = "",
    val bank: String = "",
    val holder: String = "",
    val whatsapp: String = "",
    val recipientName: String = "",
    val recipientClass: String = "",
    val recipientPhotoUrl: String = "",
    val ticketEntries: List<UserOrderTicketEntry> = emptyList(),
)

data class UserOrderTicketEntry(
    val id: String,
    val token: String,
    val label: String,
    val status: String,
    val transferredToUserName: String = "",
    val transferredFromUserName: String = "",
) {
    /** No web, `lido` e `transferido` pintam de vermelho; o resto é "Ativo". */
    val statusLabel: String
        get() = when (status.trim().lowercase()) {
            "lido" -> "Lido"
            "transferido" -> "Transferido"
            else -> "Ativo"
        }

    val isBlocked: Boolean
        get() = status.trim().lowercase() in setOf("lido", "transferido")
}

/** Dados de PIX do tenant (`financeiro`), usados quando o pedido não traz `payment_config`. */
data class UserOrderFinanceConfig(
    val pixKey: String = "",
    val bank: String = "",
    val holder: String = "",
    val whatsapp: String = "",
)
