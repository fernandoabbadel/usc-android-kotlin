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
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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
            .flatMap { it.toTickets() }
    }

    override suspend fun getTicketById(
        tenantId: String,
        userId: String,
        ticketId: String,
    ): EventTicket? = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanTicketId = ticketId.trim()
        val requestedOrderId = cleanTicketId.substringBefore(TicketRouteSeparator).trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanTicketId.isBlank()) {
            return@withContext null
        }

        clientProvider().from(TicketRequestsTable)
            .select(columns = Columns.raw(TicketRequestColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                    eq("id", requestedOrderId)
                }
                limit(count = 1)
            }
            .decodeList<EventTicketRequestRow>()
            .firstOrNull()
            ?.toTickets()
            ?.firstOrNull { it.id == cleanTicketId }
    }

    private fun EventTicketRequestRow.toTickets(): List<EventTicket> {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return emptyList()
        val mappedStatus = ticketStatusFromRemote(status)
        val entries = paymentConfig.ticketEntries()

        if (entries.isEmpty()) {
            val token = "EVT-${cleanId.take(8).uppercase(Locale.ROOT)}"
            return listOf(
                EventTicket(
                    id = cleanId,
                    eventId = eventoId.trim(),
                    eventTitle = eventoNome.trim().ifBlank { "Evento USC" },
                    holderName = userName.trim().ifBlank { "Titular" },
                    status = mappedStatus,
                    token = token,
                    lotName = loteNome.trim().ifBlank { "Ingresso" },
                    dateLabel = dataSolicitacao.toDateLabel(),
                    qrPayload = buildEventTicketPublicQrPayload(
                        tenantId = tenantId.orEmpty(),
                        orderId = cleanId,
                        ticketToken = token,
                    ),
                    transferAvailable = false,
                    holderTurma = userTurma.trim(),
                ),
            )
        }

        return entries.mapIndexed { index, entry ->
            val entryToken = entry.token.ifBlank { "${cleanId}:${index + 1}" }
            val entryStatus = ticketStatusFromRemote(entry.status).takeIf { entry.status.isNotBlank() } ?: mappedStatus
            val ticketId = "$cleanId$TicketRouteSeparator$entryToken"
            EventTicket(
                id = ticketId,
                eventId = entry.eventId.ifBlank { eventoId.trim() },
                eventTitle = entry.eventTitle.ifBlank { eventoNome.trim().ifBlank { "Evento USC" } },
                holderName = entry.holderName.ifBlank { userName.trim().ifBlank { "Titular" } },
                status = entryStatus,
                token = entryToken,
                lotName = entry.loteName.ifBlank { loteNome.trim().ifBlank { entry.label.ifBlank { "Ingresso" } } },
                dateLabel = dataSolicitacao.toDateLabel(),
                qrPayload = buildEventTicketPublicQrPayload(
                    tenantId = tenantId.orEmpty(),
                    orderId = cleanId,
                    ticketToken = entryToken,
                ),
                transferAvailable = entryStatus == TicketStatus.Active,
                holderTurma = entry.holderTurma.ifBlank { userTurma.trim() },
                transferredToUserName = entry.transferredToUserName,
                transferredFromUserName = entry.transferredFromUserName,
            )
        }
    }

    private companion object {
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val TicketRouteSeparator = "::"
        const val TicketRequestColumns =
            "id,tenant_id,userId,userName,userTurma,eventoId,eventoNome,loteId,loteNome,metodo,quantidade,status,dataSolicitacao,dataAprovacao,valorTotal,valorUnitario,payment_config"
    }
}

@Serializable
private data class EventTicketRequestRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userTurma: String = "",
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
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
)

private data class TicketEntryValue(
    val id: String,
    val token: String,
    val label: String,
    val status: String,
    val eventId: String,
    val eventTitle: String,
    val loteName: String,
    val holderName: String,
    val holderTurma: String = "",
    val transferredToUserName: String = "",
    val transferredFromUserName: String = "",
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

private fun JsonElement?.ticketEntries(): List<TicketEntryValue> {
    val config = this as? JsonObject ?: return emptyList()
    val entriesElement = config["ticketEntries"]
        ?: config["tickets"]
        ?: config["ingressos"]
        ?: return emptyList()
    val entries = entriesElement as? JsonArray ?: return emptyList()
    return entries.mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        TicketEntryValue(
            id = obj.stringValue("id"),
            token = obj.stringValue("token").ifBlank { obj.stringValue("ticketToken") },
            label = obj.stringValue("label").ifBlank { obj.stringValue("nome") },
            status = obj.stringValue("status"),
            eventId = obj.stringValue("eventId").ifBlank { obj.stringValue("eventoId") },
            eventTitle = obj.stringValue("eventTitle").ifBlank { obj.stringValue("eventoNome") },
            loteName = obj.stringValue("loteName").ifBlank { obj.stringValue("loteNome") },
            holderName = obj.stringValue("holderName").ifBlank { obj.stringValue("userName") },
            holderTurma = obj.stringValue("holderTurma").ifBlank { obj.stringValue("userTurma") },
            transferredToUserName = obj.stringValue("transferredToUserName"),
            transferredFromUserName = obj.stringValue("transferredFromUserName"),
        )
    }
}

private fun JsonObject.stringValue(key: String): String {
    val value = this[key] ?: return ""
    if (value is JsonNull) return ""
    return value.jsonPrimitive.contentOrNull.orEmpty().trim()
}

private fun buildEventTicketPublicQrPayload(
    tenantId: String,
    orderId: String,
    ticketToken: String,
): String {
    val encodedOrderId = orderId.encodePathSegment()
    val encodedToken = ticketToken.encodePathSegment()
    val cleanTenant = tenantId.trim().lowercase(Locale.ROOT)
    val tenantPrefix = cleanTenant.takeIf(String::isNotBlank)?.let { "/${it.encodePathSegment()}" }.orEmpty()
    return "https://usc-atleticas.vercel.app$tenantPrefix/public/ingressos/$encodedOrderId/$encodedToken"
}

private fun String.encodePathSegment(): String {
    return URLEncoder.encode(this, "UTF-8").replace("+", "%20")
}
