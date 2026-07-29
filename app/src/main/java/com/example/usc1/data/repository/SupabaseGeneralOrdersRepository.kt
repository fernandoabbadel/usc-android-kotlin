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

        val rows = clientProvider().from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.coerceIn(1, 120).toLong())
            }
            .decodeList<GeneralOrderRow>()

        rows.mapNotNull { it.toUiOrder() }
    }

    private fun GeneralOrderRow.toUiOrder(): GeneralOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val cleanName = productName?.trim().orEmpty().ifBlank { "Pedido da loja" }
        val quantity = quantidade ?: itens ?: 1
        val status = generalOrderStatusFromRemote(status)
        return GeneralOrder(
            id = cleanId,
            title = cleanName,
            type = GeneralOrderType.Store,
            status = status,
            amountLabel = brlFormatter.format(total ?: price ?: 0.0),
            createdAtLabel = createdAt.toDateLabel(),
            description = buildString {
                append(status.description)
                append(" • ")
                append(quantity.coerceAtLeast(1))
                append(if (quantity == 1) " item" else " itens")
                if (!productId.isNullOrBlank()) {
                    append(" • Produto ")
                    append(productId.trim().take(8).uppercase())
                }
            },
        )
    }

    private companion object {
        const val OrdersTable = "orders"
        const val OrderColumns =
            "id,tenant_id,userId,productId,productName,price,total,quantidade,itens,status,createdAt,updatedAt"

        val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    }
}

@Serializable
private data class GeneralOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double? = null,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

private val generalOrdersDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    .withZone(ZoneId.of("America/Sao_Paulo"))

private fun generalOrderStatusFromRemote(value: String?): GeneralOrderStatus {
    return when (value?.trim()?.lowercase()) {
        "approved", "aprovado", "confirmado", "delivered", "entregue" -> GeneralOrderStatus.Approved
        "rejected", "cancelled", "canceled", "cancelado" -> GeneralOrderStatus.Cancelled
        else -> GeneralOrderStatus.Pending
    }
}

private val GeneralOrderStatus.description: String
    get() = when (this) {
        GeneralOrderStatus.Pending -> "Aguardando aprovação do pagamento."
        GeneralOrderStatus.Approved -> "Pedido aprovado para retirada."
        GeneralOrderStatus.Cancelled -> "Pedido cancelado."
    }

private fun String?.toDateLabel(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return "Data não informada"
    return runCatching {
        generalOrdersDateFormatter.format(Instant.parse(raw))
    }.recoverCatching {
        generalOrdersDateFormatter.format(OffsetDateTime.parse(raw))
    }.getOrElse {
        raw.take(16)
    }
}
