package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.MiniVendorRepository
import com.example.usc1.ui.vendor.MiniVendorOrder
import com.example.usc1.ui.vendor.MiniVendorOrderStatus
import com.example.usc1.ui.vendor.MiniVendorProduct
import com.example.usc1.ui.vendor.MiniVendorUiState
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

class SupabaseMiniVendorRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : MiniVendorRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    override suspend fun getDashboard(
        tenantId: String,
        userId: String,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext MiniVendorUiState(
                statusLabel = "Entre com uma sessão válida para carregar seu Mini Vendor.",
            )
        }

        val client = clientProvider()
        val profile = client.from(MiniVendorsTable)
            .select(columns = Columns.raw(MiniVendorColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("user_id", cleanUserId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorProfileRow>()
            .firstOrNull()

        if (profile == null || profile.id.trim().isBlank()) {
            return@withContext MiniVendorUiState(
                statusLabel = "Você ainda não cadastrou sua lojinha.",
            )
        }

        val sellerId = profile.id.trim()
        val products = client.from(ProductsTable)
            .select(columns = Columns.raw(ProductColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxProducts.toLong())
            }
            .decodeList<MiniVendorProductRow>()
            .map(::mapProduct)

        val orders = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxOrders.toLong())
            }
            .decodeList<MiniVendorOrderRow>()

        val approvedOrders = orders
            .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Approved }
            .map(::mapOrder)
        val pendingOrders = orders
            .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Pending }
            .map(::mapOrder)

        MiniVendorUiState(
            profileId = sellerId,
            storeName = profile.storeName.orEmpty().trim().ifBlank { "Mini Vendor" },
            statusLabel = profile.status.toProfileStatusLabel(),
            description = profile.description.orEmpty().trim(),
            logoUrl = profile.logoUrl?.trim()?.takeIf(String::isNotBlank),
            coverUrl = profile.coverUrl?.trim()?.takeIf(String::isNotBlank),
            totalRevenueLabel = formatCurrency(
                orders
                    .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Approved }
                    .sumOf(MiniVendorOrderRow::amount),
            ),
            pendingAmountLabel = formatCurrency(
                orders
                    .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Pending }
                    .sumOf(MiniVendorOrderRow::amount),
            ),
            products = products,
            pendingOrders = pendingOrders,
            approvedOrders = approvedOrders,
        )
    }

    private fun mapProduct(row: MiniVendorProductRow): MiniVendorProduct {
        val stock = row.estoque ?: 0
        return MiniVendorProduct(
            id = row.id.trim(),
            name = row.nome.orEmpty().trim().ifBlank { "Produto" },
            priceLabel = formatCurrency(row.preco ?: 0.0),
            stockLabel = when {
                stock <= 0 -> "Sem estoque"
                stock == 1 -> "1 disponível"
                else -> "$stock disponíveis"
            },
            status = productStatusLabel(row),
            imageUrl = row.img?.trim()?.takeIf(String::isNotBlank),
        )
    }

    private fun mapOrder(row: MiniVendorOrderRow): MiniVendorOrder {
        return MiniVendorOrder(
            id = row.id.trim().ifBlank { "Pedido" }.take(12).uppercase(Locale.ROOT),
            customerName = row.userName.orEmpty().trim().ifBlank { "Cliente USC" },
            productName = row.productName.orEmpty().trim().ifBlank { "Produto" },
            amountLabel = formatCurrency(row.amount()),
            createdAtLabel = formatDate(row.createdAt.orEmpty()),
            status = row.status.toOrderStatus(),
        )
    }

    private fun productStatusLabel(row: MiniVendorProductRow): String {
        val cleanStatus = row.status.orEmpty().trim().lowercase(Locale.ROOT)
        return when {
            row.active == false -> "Inativo"
            row.aprovado == false -> "Em análise"
            cleanStatus == "em_breve" || cleanStatus == "agendado" -> "Em breve"
            cleanStatus == "esgotado" || cleanStatus == "encerrado" -> "Esgotado"
            row.vendidos > 0 -> "${row.vendidos} vendidos"
            else -> "Publicado"
        }
    }

    private fun String?.toProfileStatusLabel(): String {
        return when (orEmpty().trim().lowercase(Locale.ROOT)) {
            "approved", "aprovado" -> "Aprovado para vender"
            "rejected", "recusado", "rejeitado" -> "Cadastro recusado"
            "disabled", "desativado" -> "Mini Vendor desativado"
            else -> "Aguardando aprovação"
        }
    }

    private fun String?.toOrderStatus(): MiniVendorOrderStatus {
        return when (orEmpty().trim().lowercase(Locale.ROOT)) {
            "approved",
            "aprovado",
            "confirmado",
            "confirmed",
            "paid",
            "pago",
            "delivered",
            "entregue",
            "retirado" -> MiniVendorOrderStatus.Approved
            else -> MiniVendorOrderStatus.Pending
        }
    }

    private fun formatCurrency(value: Double): String = currencyFormatter.format(value)

    private fun formatDate(value: String): String {
        val clean = value.trim()
        if (clean.isBlank()) return ""
        val instant = runCatching { OffsetDateTime.parse(clean).toInstant() }
            .getOrElse {
                runCatching { Instant.parse(clean) }.getOrNull()
            } ?: return clean.take(16)
        return MiniVendorDateFormatter.format(instant)
    }

    private companion object {
        const val MiniVendorsTable = "mini_vendors"
        const val ProductsTable = "produtos"
        const val OrdersTable = "orders"
        const val MaxProducts = 120
        const val MaxOrders = 160
        const val MiniVendorColumns =
            "id,tenant_id,user_id,status,store_name,description,logo_url,cover_url,created_at,updated_at"
        const val ProductColumns =
            "id,tenant_id,nome,categoria,descricao,img,preco,estoque,active,aprovado,vendidos,status,seller_type,seller_id,seller_name,seller_logo_url,createdAt"
        const val OrderColumns =
            "id,tenant_id,userId,userName,productId,productName,price,total,quantidade,itens,status,seller_type,seller_id,seller_name,seller_logo_url,createdAt,updatedAt"
    }
}

private val MiniVendorDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM • HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.of("America/Sao_Paulo"))

@Serializable
private data class MiniVendorProfileRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val status: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class MiniVendorProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    val categoria: String? = null,
    val descricao: String? = null,
    val img: String? = null,
    val preco: Double? = null,
    val estoque: Int? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val vendidos: Int = 0,
    val status: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class MiniVendorOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double? = null,
    val total: Double? = null,
    val quantidade: Int? = null,
    val status: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    fun amount(): Double {
        val cleanTotal = total ?: 0.0
        if (cleanTotal > 0.0) return cleanTotal
        val cleanPrice = price ?: 0.0
        val cleanQuantity = quantidade ?: 1
        return cleanPrice * cleanQuantity.coerceAtLeast(1)
    }
}
