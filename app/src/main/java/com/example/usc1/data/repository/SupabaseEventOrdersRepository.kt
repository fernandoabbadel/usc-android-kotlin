package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.domain.model.EventOrderItemType
import com.example.usc1.domain.model.OrderStatus
import com.example.usc1.domain.model.PaymentStatus
import com.example.usc1.domain.repository.EventOrdersRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseEventOrdersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : EventOrdersRepository {
    override suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<EventOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext emptyList()
        }

        val client = clientProvider()
        val safeLimit = limit.coerceIn(1, 120)
        val ticketRequests = runCatching { fetchTicketRequests(client, cleanTenantId, cleanUserId, safeLimit) }
            .getOrDefault(emptyList())
        val eventProductOrders = runCatching { fetchEventProductOrders(client, cleanTenantId, cleanUserId, safeLimit) }
            .getOrDefault(emptyList())

        (ticketRequests + eventProductOrders)
            .sortedByDescending { it.sortMillis }
            .take(safeLimit)
            .map { it.order }
    }

    override suspend fun getOrderById(
        tenantId: String,
        userId: String,
        orderId: String,
    ): EventOrder? = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanOrderId = orderId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanOrderId.isBlank()) {
            return@withContext null
        }

        val client = clientProvider()
        val ticketOrder = runCatching {
            client.from(TicketRequestsTable)
                .select(columns = Columns.raw(TicketRequestColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                        eq("id", cleanOrderId)
                    }
                    limit(count = 1)
                }
                .decodeList<EventOrderRequestRow>()
                .firstOrNull()
                ?.toOrderWithSort()
                ?.order
        }.getOrNull()

        ticketOrder ?: runCatching {
            client.from(OrdersTable)
                .select(columns = Columns.raw(EventProductOrderColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                        eq("id", cleanOrderId)
                    }
                    limit(count = 1)
                }
                .decodeList<EventProductOrderRow>()
                .firstOrNull()
                ?.toOrderWithSort()
                ?.order
        }.getOrNull()
    }

    private suspend fun fetchTicketRequests(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<EventOrderWithSort> {
        return client.from(TicketRequestsTable)
            .select(columns = Columns.raw(TicketRequestColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "dataSolicitacao", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<EventOrderRequestRow>()
            .mapNotNull { it.toOrderWithSort() }
    }

    private suspend fun fetchEventProductOrders(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<EventOrderWithSort> {
        return client.from(OrdersTable)
            .select(columns = Columns.raw(EventProductOrderColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<EventProductOrderRow>()
            .mapNotNull { it.toOrderWithSort() }
    }

    private fun EventOrderRequestRow.toOrderWithSort(): EventOrderWithSort? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val orderStatus = orderStatusFromRemote(status)
        return EventOrderWithSort(
            order = EventOrder(
                id = cleanId,
                eventId = eventoId.trim(),
                eventTitle = eventoNome.trim().ifBlank { "Evento USC" },
                itemType = EventOrderItemType.Ticket,
                status = orderStatus,
                paymentStatus = paymentStatusFromRemote(status),
                approvalStatus = approvalText(orderStatus, dataAprovacao),
                amountLabel = valorTotal.asCurrencyLabel(),
                quantity = quantidade.coerceAtLeast(1),
                createdAtLabel = dataSolicitacao.toEventOrderDateLabel(),
                lotName = loteNome.trim().ifBlank { "Ingresso" },
            ),
            sortMillis = dataSolicitacao.toEventOrderMillis(),
        )
    }

    private fun EventProductOrderRow.toOrderWithSort(): EventOrderWithSort? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val eventParty = data.asJsonObjectOrEmpty().objectValue("eventParty")
        val linkedEventId = eventParty.stringValue("eventId")
        if (linkedEventId.isBlank()) return null

        val quantity = (quantidade ?: itens ?: 1).coerceAtLeast(1)
        val totalAmount = total ?: price
        val orderStatus = orderStatusFromRemote(status)
        val cleanProductName = productName?.trim().orEmpty().ifBlank {
            eventParty.stringValue("productName").ifBlank { "Produto do evento" }
        }
        val section = eventParty.stringValue("section").ifBlank { "Menu do evento" }
        val rawDate = createdAt?.trim().orEmpty()

        return EventOrderWithSort(
            order = EventOrder(
                id = cleanId,
                eventId = linkedEventId,
                eventTitle = eventParty.stringValue("eventTitle").ifBlank { "Modo Vendas" },
                itemType = EventOrderItemType.EventProduct,
                status = orderStatus,
                paymentStatus = paymentStatusFromRemote(status),
                approvalStatus = approvalText(orderStatus, updatedAt),
                amountLabel = eventOrdersCurrencyFormatter.format(totalAmount),
                quantity = quantity,
                createdAtLabel = rawDate.toEventOrderDateLabel(),
                lotName = "$section • $cleanProductName",
            ),
            sortMillis = rawDate.toEventOrderMillis(),
        )
    }

    private companion object {
        const val OrdersTable = "orders"
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val TicketRequestColumns =
            "id,tenant_id,userId,userName,eventoId,eventoNome,loteId,loteNome,metodo,quantidade,status,dataSolicitacao,dataAprovacao,valorTotal,valorUnitario"
        const val EventProductOrderColumns =
            "id,tenant_id,userId,productId,productName,price,total,quantidade,itens,status,createdAt,updatedAt,data"
    }
}

private data class EventOrderWithSort(
    val order: EventOrder,
    val sortMillis: Long,
)

@Serializable
private data class EventOrderRequestRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String = "",
    val userName: String = "",
    val eventoId: String = "",
    val eventoNome: String = "",
    val loteId: String = "",
    val loteNome: String = "",
    val metodo: String = "",
    val quantidade: Int = 1,
    val status: String = "",
    val dataSolicitacao: String = "",
    val dataAprovacao: String? = null,
    val valorTotal: String = "",
    val valorUnitario: String = "",
)

@Serializable
private data class EventProductOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String = "",
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double = 0.0,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String = "",
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val data: JsonElement? = null,
)

private val eventOrderZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val eventOrderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
private val eventOrdersCurrencyFormatter: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private fun orderStatusFromRemote(value: String): OrderStatus {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "aprovado", "approved", "confirmado", "confirmed", "ativo", "active", "pago", "paid" -> OrderStatus.Approved
        "cancelado", "cancelled", "canceled" -> OrderStatus.Cancelled
        "rejeitado", "rejected" -> OrderStatus.Rejected
        else -> OrderStatus.Pending
    }
}

private fun paymentStatusFromRemote(value: String): PaymentStatus {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "aprovado", "approved", "confirmado", "confirmed", "ativo", "active", "pago", "paid" -> PaymentStatus.Paid
        "cancelado", "cancelled", "canceled", "rejeitado", "rejected" -> PaymentStatus.Cancelled
        "reembolsado", "refunded" -> PaymentStatus.Refunded
        else -> PaymentStatus.WaitingPayment
    }
}

private fun approvalText(status: OrderStatus, approvedAt: String?): String {
    return when (status) {
        OrderStatus.Pending -> "Aguardando aprovação"
        OrderStatus.Approved -> approvedAt?.trim()?.takeIf(String::isNotBlank)?.let { "Aprovado em ${it.toEventOrderDateLabel()}" } ?: "Aprovado"
        OrderStatus.Cancelled -> "Cancelado"
        OrderStatus.Rejected -> "Rejeitado"
    }
}

private fun String.toEventOrderDateLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return "Data não informada"
    return runCatching {
        eventOrderDateFormatter.format(OffsetDateTime.parse(clean).atZoneSameInstant(eventOrderZone))
    }.recoverCatching {
        eventOrderDateFormatter.format(Instant.parse(clean).atZone(eventOrderZone))
    }.getOrElse {
        clean.take(16)
    }
}

private fun String.toEventOrderMillis(): Long {
    val clean = trim()
    if (clean.isBlank()) return 0L
    return runCatching {
        OffsetDateTime.parse(clean).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(clean).toEpochMilli()
    }.getOrDefault(0L)
}

private fun String.asCurrencyLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return "R$ 0,00"
    if (clean.startsWith("R$", ignoreCase = true)) return clean
    return "R$ $clean"
}

private fun JsonElement?.asJsonObjectOrEmpty(): JsonObject {
    return this as? JsonObject ?: JsonObject(emptyMap())
}

private fun JsonObject.objectValue(key: String): JsonObject {
    return this[key] as? JsonObject ?: JsonObject(emptyMap())
}

private fun JsonObject.stringValue(key: String): String {
    val value = this[key] ?: return ""
    if (value is JsonNull) return ""
    return value.jsonPrimitive.contentOrNull?.trim().orEmpty()
}
