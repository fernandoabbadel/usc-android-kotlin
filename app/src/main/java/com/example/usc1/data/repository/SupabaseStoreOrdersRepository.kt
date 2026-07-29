package com.example.usc1.data.repository

import com.example.usc1.R
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.StoreSellerType
import com.example.usc1.domain.repository.StoreOrdersRepository
import com.example.usc1.ui.store.CartItem
import com.example.usc1.ui.store.StoreOrder
import com.example.usc1.ui.store.StoreOrderStatus
import com.example.usc1.ui.store.StorePaymentStatus
import com.example.usc1.ui.store.StoreProduct
import com.example.usc1.ui.store.StoreProductStatus
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

class SupabaseStoreOrdersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : StoreOrdersRepository {
    override suspend fun getOrders(
        tenantId: String,
        userId: String,
        limit: Int,
    ): List<StoreOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext emptyList()
        }

        clientProvider().from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.coerceIn(1, 120).toLong())
            }
            .decodeList<StoreOrderRow>()
            .mapNotNull { it.toUiOrder(cleanTenantId) }
    }

    override suspend fun getOrderById(
        tenantId: String,
        userId: String,
        orderId: String,
    ): StoreOrder? = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanOrderId = orderId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanOrderId.isBlank()) {
            return@withContext null
        }

        clientProvider().from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("userId", cleanUserId)
                    eq("id", cleanOrderId)
                }
                limit(count = 1)
            }
            .decodeList<StoreOrderRow>()
            .firstOrNull()
            ?.toUiOrder(cleanTenantId)
    }

    private fun StoreOrderRow.toUiOrder(activeTenantId: String): StoreOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val cleanQuantity = (quantidade ?: itens ?: 1).coerceAtLeast(1)
        val cleanProductName = firstNotBlank(productName, productname, "Pedido da loja")
        val cleanProductId = firstNotBlank(productId, productid, cleanId)
        val cleanSellerType = StoreSellerType.fromRemote(sellerType, sellerId, activeTenantId)
        val cleanSellerName = firstNotBlank(sellerName, cleanSellerType.label)
        val totalValue = total ?: (price * cleanQuantity)
        val mappedStatus = storeOrderStatusFromRemote(status)

        return StoreOrder(
            id = cleanId,
            title = cleanProductName,
            createdAtLabel = createdAt.toStoreOrderDateLabel(),
            status = mappedStatus,
            paymentStatus = paymentStatusFromRemote(status),
            amountLabel = brlFormatter.format(totalValue),
            items = listOf(
                CartItem(
                    product = StoreProduct(
                        id = cleanProductId,
                        name = cleanProductName,
                        description = "Pedido realizado na loja do tenant.",
                        category = cleanSellerType.label,
                        priceLabel = brlFormatter.format(price),
                        status = StoreProductStatus.Available,
                        badge = cleanSellerName,
                        imageRes = cleanSellerType.fallbackImageRes(),
                        imageUrl = sellerLogoUrl?.trim()?.takeIf { it.isNotBlank() },
                        stockLabel = if (cleanQuantity == 1) "1 item" else "$cleanQuantity itens",
                        reviewLabel = "Vendedor: $cleanSellerName",
                        sellerType = cleanSellerType,
                        sellerId = sellerId?.trim().orEmpty(),
                        sellerName = cleanSellerName,
                    ),
                    quantity = cleanQuantity,
                ),
            ),
            pickupLabel = mappedStatus.pickupLabel,
        )
    }

    private companion object {
        const val OrdersTable = "orders"
        const val OrderColumns =
            "id,tenant_id,userId,userName,username,productId,productid,productName,productname,price,total,quantidade,itens,status,createdAt,updatedAt,seller_type,seller_id,seller_name,seller_logo_url"

        val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(storeOrdersPtBr)
    }
}

@Serializable
private data class StoreOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    val username: String? = null,
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
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
)

private val storeOrdersPtBr: Locale = Locale.forLanguageTag("pt-BR")
private val storeOrdersZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val storeOrdersDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", storeOrdersPtBr)

private fun storeOrderStatusFromRemote(value: String?): StoreOrderStatus {
    return when (value?.trim()?.lowercase(Locale.ROOT)) {
        "approved", "aprovado", "confirmado", "confirmed", "delivered", "entregue", "ativo", "paid", "pago" -> StoreOrderStatus.Approved
        "rejected", "cancelled", "canceled", "cancelado", "rejeitado", "vencido" -> StoreOrderStatus.Cancelled
        else -> StoreOrderStatus.Pending
    }
}

private fun paymentStatusFromRemote(value: String?): StorePaymentStatus {
    return when (value?.trim()?.lowercase(Locale.ROOT)) {
        "approved", "aprovado", "confirmado", "confirmed", "delivered", "entregue", "ativo", "paid", "pago" -> StorePaymentStatus.Paid
        "rejected", "cancelled", "canceled", "cancelado", "rejeitado", "vencido" -> StorePaymentStatus.Cancelled
        else -> StorePaymentStatus.WaitingPayment
    }
}

private val StoreOrderStatus.pickupLabel: String
    get() = when (this) {
        StoreOrderStatus.Pending -> "Aguardando aprovação da atlética."
        StoreOrderStatus.Approved -> "Liberado para retirada."
        StoreOrderStatus.Cancelled -> "Pedido encerrado."
    }

private fun StoreSellerType.fallbackImageRes(): Int {
    return when (this) {
        StoreSellerType.Tenant -> R.drawable.logo_usc_wide
        StoreSellerType.MiniVendor,
        StoreSellerType.Liga,
        StoreSellerType.Comissao,
        StoreSellerType.Diretorio -> R.drawable.logo_platform_web
        StoreSellerType.Unknown -> R.drawable.carteirinha_bg
    }
}

private fun String?.toStoreOrderDateLabel(): String {
    val clean = this?.trim().orEmpty()
    if (clean.isBlank()) return "Data não informada"
    return runCatching {
        storeOrdersDateFormatter.format(OffsetDateTime.parse(clean).atZoneSameInstant(storeOrdersZone))
    }.recoverCatching {
        storeOrdersDateFormatter.format(Instant.parse(clean).atZone(storeOrdersZone))
    }.getOrElse {
        clean.take(16)
    }
}

private fun firstNotBlank(vararg values: String?): String {
    return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
}
