package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.domain.model.OrderStatus
import com.example.usc1.domain.model.PaymentStatus
import com.example.usc1.domain.repository.EventOrdersRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

        clientProvider().from(TicketRequestsTable)
            .select(columns = Columns.raw(TicketRequestColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                }
                order(column = "dataSolicitacao", order = Order.DESCENDING)
                limit(count = limit.coerceIn(1, 120).toLong())
            }
            .decodeList<EventOrderRequestRow>()
            .mapNotNull { it.toOrder() }
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

        clientProvider().from(TicketRequestsTable)
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
            ?.toOrder()
    }

    private fun EventOrderRequestRow.toOrder(): EventOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val orderStatus = orderStatusFromRemote(status)
        return EventOrder(
            id = cleanId,
            eventId = eventoId.trim(),
            eventTitle = eventoNome.trim().ifBlank { "Evento USC" },
            status = orderStatus,
            paymentStatus = paymentStatusFromRemote(status),
            approvalStatus = approvalText(orderStatus, dataAprovacao),
            amountLabel = valorTotal.asCurrencyLabel(),
            quantity = quantidade.coerceAtLeast(1),
            createdAtLabel = dataSolicitacao.toEventOrderDateLabel(),
            lotName = loteNome.trim().ifBlank { "Ingresso" },
        )
    }

    private companion object {
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val TicketRequestColumns =
            "id,tenant_id,userId,userName,eventoId,eventoNome,loteId,loteNome,metodo,quantidade,status,dataSolicitacao,dataAprovacao,valorTotal,valorUnitario"
    }
}

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

private val eventOrderZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val eventOrderDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

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

private fun String.asCurrencyLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return "R$ 0,00"
    if (clean.startsWith("R$", ignoreCase = true)) return clean
    return "R$ $clean"
}
