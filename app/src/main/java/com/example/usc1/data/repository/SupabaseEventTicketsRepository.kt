package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.EventTicket
import com.example.usc1.domain.model.TicketStatus
import com.example.usc1.domain.repository.EventTicketsRepository
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

class SupabaseEventTicketsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : EventTicketsRepository {
    override suspend fun getTickets(
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<EventTicket> = withContext(Dispatchers.IO) {
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
            .decodeList<EventTicketRequestRow>()
            .mapNotNull { it.toTicket() }
    }

    override suspend fun getTicketById(
        tenantId: String,
        userId: String,
        ticketId: String,
    ): EventTicket? = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanTicketId = ticketId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanTicketId.isBlank()) {
            return@withContext null
        }

        clientProvider().from(TicketRequestsTable)
            .select(columns = Columns.raw(TicketRequestColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                    eq("id", cleanTicketId)
                }
                limit(count = 1)
            }
            .decodeList<EventTicketRequestRow>()
            .firstOrNull()
            ?.toTicket()
    }

    private fun EventTicketRequestRow.toTicket(): EventTicket? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val mappedStatus = ticketStatusFromRemote(status)
        val token = "EVT-${cleanId.take(8).uppercase(Locale.ROOT)}"
        return EventTicket(
            id = cleanId,
            eventId = eventoId.trim(),
            eventTitle = eventoNome.trim().ifBlank { "Evento USC" },
            holderName = userName.trim().ifBlank { "Titular" },
            status = mappedStatus,
            token = token,
            lotName = loteNome.trim().ifBlank { "Ingresso" },
            dateLabel = dataSolicitacao.toDateLabel(),
            qrPayload = "usc:event-ticket:$cleanId",
            transferAvailable = mappedStatus == TicketStatus.Active,
        )
    }

    private companion object {
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val TicketRequestColumns =
            "id,tenant_id,userId,userName,eventoId,eventoNome,loteId,loteNome,metodo,quantidade,status,dataSolicitacao,dataAprovacao,valorTotal,valorUnitario"
    }
}

@Serializable
private data class EventTicketRequestRow(
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

private val eventTicketZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val eventTicketDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm", Locale.forLanguageTag("pt-BR"))

private fun ticketStatusFromRemote(value: String): TicketStatus {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "aprovado", "approved", "confirmado", "confirmed", "ativo", "active", "pago", "paid" -> TicketStatus.Active
        "usado", "used", "utilizado", "checked_in", "validado" -> TicketStatus.Used
        "transferido", "transferred" -> TicketStatus.Transferred
        "cancelado", "cancelled", "canceled", "rejeitado", "rejected" -> TicketStatus.Cancelled
        else -> TicketStatus.Pending
    }
}

private fun String.toDateLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return "Data não informada"
    return runCatching {
        eventTicketDateFormatter.format(OffsetDateTime.parse(clean).atZoneSameInstant(eventTicketZone))
    }.recoverCatching {
        eventTicketDateFormatter.format(Instant.parse(clean).atZone(eventTicketZone))
    }.getOrElse {
        clean.take(16)
    }
}
