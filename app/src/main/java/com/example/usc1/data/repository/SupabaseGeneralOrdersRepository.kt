package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.GeneralOrdersRepository
import com.example.usc1.ui.generalorders.GeneralOrder
import com.example.usc1.ui.generalorders.GeneralOrderStatus
import com.example.usc1.ui.generalorders.GeneralOrderType
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

class SupabaseGeneralOrdersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : GeneralOrdersRepository {
    override suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<GeneralOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext emptyList()
        }

        val client = clientProvider()
        val boundedLimit = limit.coerceIn(1, 120)
        val storeOrders = runCatching { fetchStoreOrders(client, cleanTenantId, cleanUserId, boundedLimit) }
            .getOrDefault(emptyList())
        val eventOrders = runCatching { fetchEventTicketRequests(client, cleanTenantId, cleanUserId, boundedLimit) }
            .getOrDefault(emptyList())
        val planOrders = runCatching { fetchPlanSubscriptions(client, cleanTenantId, cleanUserId, boundedLimit) }
            .getOrDefault(emptyList())

        (storeOrders + eventOrders + planOrders)
            .sortedByDescending { it.sortMillis }
            .take(boundedLimit)
            .map { it.order }
    }

    private suspend fun fetchStoreOrders(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<GeneralOrderWithSort> {
        return client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<GeneralStoreOrderRow>()
            .mapNotNull { it.toUiOrder() }
    }

    private suspend fun fetchEventTicketRequests(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<GeneralOrderWithSort> {
        return client.from(TicketRequestsTable)
            .select(columns = Columns.raw(TicketRequestColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "dataSolicitacao", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<GeneralTicketRequestRow>()
            .mapNotNull { it.toUiOrder() }
    }

    private suspend fun fetchPlanSubscriptions(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<GeneralOrderWithSort> {
        return client.from(SubscriptionsTable)
            .select(columns = Columns.raw(SubscriptionColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<GeneralPlanSubscriptionRow>()
            .mapNotNull { it.toUiOrder() }
    }

    private fun GeneralStoreOrderRow.toUiOrder(): GeneralOrderWithSort? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val cleanName = firstNotBlank(productName, productname, "Pedido da loja")
        val quantity = quantidade ?: itens ?: 1
        val cleanStatus = generalOrderStatusFromRemote(status)
        val rawDate = createdAt?.trim().orEmpty()
        return GeneralOrderWithSort(
            order = GeneralOrder(
                id = cleanId,
                title = cleanName,
                type = GeneralOrderType.Store,
                status = cleanStatus,
                amountLabel = brlFormatter.format(total ?: price),
                createdAtLabel = rawDate.toDateLabel(),
                description = buildString {
                    append(cleanStatus.description)
                    append(" • ")
                    append(quantity.coerceAtLeast(1))
                    append(if (quantity == 1) " item" else " itens")
                    if (!productId.isNullOrBlank()) {
                        append(" • Produto ")
                        append(productId.trim().take(8).uppercase(Locale.ROOT))
                    }
                },
            ),
            sortMillis = rawDate.toMillis(),
        )
    }

    private fun GeneralTicketRequestRow.toUiOrder(): GeneralOrderWithSort? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val cleanStatus = generalOrderStatusFromRemote(status)
        val rawDate = dataSolicitacao.trim()
        return GeneralOrderWithSort(
            order = GeneralOrder(
                id = cleanId,
                title = eventoNome.trim().ifBlank { "Ingresso de evento" },
                type = GeneralOrderType.Events,
                status = cleanStatus,
                amountLabel = valorTotal.asCurrencyLabel(),
                createdAtLabel = rawDate.toDateLabel(),
                description = buildString {
                    append(cleanStatus.description)
                    append(" • ")
                    append(quantidade.coerceAtLeast(1))
                    append(if (quantidade == 1) " ingresso" else " ingressos")
                    if (loteNome.isNotBlank()) {
                        append(" • ")
                        append(loteNome.trim())
                    }
                    if (metodo.isNotBlank()) {
                        append(" • ")
                        append(metodo.trim().uppercase(Locale.ROOT))
                    }
                },
            ),
            sortMillis = rawDate.toMillis(),
        )
    }

    private fun GeneralPlanSubscriptionRow.toUiOrder(): GeneralOrderWithSort? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val cleanStatus = generalOrderStatusFromRemote(status)
        val rawDate = firstNotBlank(createdAt, dataInicio)
        return GeneralOrderWithSort(
            order = GeneralOrder(
                id = cleanId,
                title = planoNome.trim().ifBlank { "Plano da atlética" },
                type = GeneralOrderType.Plans,
                status = cleanStatus,
                amountLabel = brlFormatter.format(valorPago),
                createdAtLabel = rawDate.toDateLabel(),
                description = buildString {
                    append(cleanStatus.description)
                    if (metodo.isNotBlank()) {
                        append(" • Método ")
                        append(metodo.trim().uppercase(Locale.ROOT))
                    }
                    if (turma.isNotBlank()) {
                        append(" • Turma ")
                        append(turma.trim())
                    }
                },
            ),
            sortMillis = rawDate.toMillis(),
        )
    }

    private companion object {
        const val OrdersTable = "orders"
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val SubscriptionsTable = "assinaturas"
        const val OrderColumns =
            "id,tenant_id,userId,productId,productid,productName,productname,price,total,quantidade,itens,status,createdAt,updatedAt"
        const val TicketRequestColumns =
            "id,tenant_id,userId,eventoId,eventoNome,loteId,loteNome,metodo,quantidade,status,dataSolicitacao,dataAprovacao,valorTotal,valorUnitario"
        const val SubscriptionColumns =
            "id,tenant_id,userId,planoId,planoNome,valorPago,dataInicio,status,metodo,turma,createdAt,updatedAt"

        val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(generalOrdersPtBr)
    }
}

private data class GeneralOrderWithSort(
    val order: GeneralOrder,
    val sortMillis: Long,
)

private val generalOrdersPtBr: Locale = Locale.forLanguageTag("pt-BR")
private val generalOrdersZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val generalOrdersDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", generalOrdersPtBr)

@Serializable
private data class GeneralStoreOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("productId") val productId: String? = null,
    val productid: String? = null,
    @SerialName("productName") val productName: String? = null,
    val productname: String? = null,
    val price: Double = 0.0,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
private data class GeneralTicketRequestRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String = "",
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
private data class GeneralPlanSubscriptionRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String? = null,
    val planoId: String = "",
    val planoNome: String = "",
    val valorPago: Double = 0.0,
    val dataInicio: String? = null,
    val status: String = "",
    val metodo: String = "",
    val turma: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

private fun generalOrderStatusFromRemote(value: String?): GeneralOrderStatus {
    return when (value?.trim()?.lowercase(Locale.ROOT)) {
        "approved", "aprovado", "confirmado", "confirmed", "delivered", "entregue", "ativo" -> GeneralOrderStatus.Approved
        "rejected", "cancelled", "canceled", "cancelado", "vencido", "rejeitado" -> GeneralOrderStatus.Cancelled
        else -> GeneralOrderStatus.Pending
    }
}

private val GeneralOrderStatus.description: String
    get() = when (this) {
        GeneralOrderStatus.Pending -> "Aguardando aprovação do pagamento."
        GeneralOrderStatus.Approved -> "Pedido aprovado."
        GeneralOrderStatus.Cancelled -> "Pedido cancelado."
    }

private fun String?.toDateLabel(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return "Data não informada"
    val parsed = raw.toOffsetDateTimeOrNull()
    return if (parsed != null) {
        generalOrdersDateFormatter.format(parsed.atZoneSameInstant(generalOrdersZone))
    } else {
        raw.take(16)
    }
}

private fun String?.toMillis(): Long {
    val raw = this?.trim().orEmpty()
    return raw.toOffsetDateTimeOrNull()?.toInstant()?.toEpochMilli() ?: 0L
}

private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? {
    return runCatching { OffsetDateTime.parse(this) }
        .getOrElse {
            runCatching { Instant.parse(this).atZone(generalOrdersZone).toOffsetDateTime() }
                .getOrNull()
        }
}

private fun String.asCurrencyLabel(): String {
    val clean = trim()
    if (clean.isBlank()) return "R$ 0,00"
    if (clean.startsWith("R$", ignoreCase = true)) return clean
    return "R$ $clean"
}

private fun firstNotBlank(vararg values: String?): String {
    return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
}
