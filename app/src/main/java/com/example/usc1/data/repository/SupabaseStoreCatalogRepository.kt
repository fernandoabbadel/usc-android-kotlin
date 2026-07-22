package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.StoreCatalogCategory
import com.example.usc1.domain.model.StoreCatalogPage
import com.example.usc1.domain.model.StoreCatalogProduct
import com.example.usc1.domain.model.StoreSeller
import com.example.usc1.domain.model.StoreSellerType
import com.example.usc1.domain.repository.StoreCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseStoreCatalogRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : StoreCatalogRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override suspend fun getProductsPage(
        category: String?,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): StoreCatalogPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, MaxPageSize)
        val from = ((safePage - 1) * safePageSize).toLong()
        val to = from + safePageSize
        val cleanCategory = category?.trim().orEmpty().takeUnless { it.isBlank() || it == AllCategory }

        val products = client.from(ProductsTable)
            .select(columns = Columns.raw(ProductColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("active", true)
                    eq("aprovado", true)
                    if (cleanCategory != null) {
                        eq("categoria", cleanCategory)
                    }
                }
                order(
                    column = if (cleanCategory == null) "createdAt" else "nome",
                    order = if (cleanCategory == null) Order.DESCENDING else Order.ASCENDING,
                )
                range(from..to)
            }
            .decodeList<StoreProductRow>()
            .mapNotNull { it.toDomain(tenantId) }

        StoreCatalogPage(
            products = products.take(safePageSize),
            categories = fetchCategories(client, tenantId),
            hasMore = products.size > safePageSize,
            page = safePage,
            pageSize = safePageSize,
            activeTenantId = tenantId,
        )
    }

    override suspend fun getProductById(productId: String): StoreCatalogProduct? = withContext(Dispatchers.IO) {
        val cleanProductId = productId.trim()
        if (cleanProductId.isBlank()) return@withContext null

        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(ProductsTable)
            .select(columns = Columns.raw(ProductColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanProductId)
                    eq("active", true)
                    eq("aprovado", true)
                }
                limit(count = 1)
            }
            .decodeList<StoreProductRow>()
            .firstOrNull()
            ?.toDomain(tenantId)
    }

    private suspend fun fetchCategories(
        client: SupabaseClient,
        tenantId: String,
    ): List<StoreCatalogCategory> {
        return client.from(CategoriesTable)
            .select(columns = Columns.raw(CategoryColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "display_order", order = Order.ASCENDING)
                limit(count = MaxCategories.toLong())
            }
            .decodeList<StoreCategoryRow>()
            .mapNotNull { it.toDomain(tenantId) }
            .filter { it.visible }
            .distinctBy { "${it.seller.type.remoteValue}:${it.seller.id}:${it.name.lowercase()}" }
            .sortedWith(
                compareBy<StoreCatalogCategory> { it.seller.type.sortOrder }
                    .thenBy { it.displayOrder ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() },
            )
    }

    private fun StoreProductRow.toDomain(activeTenantId: String): StoreCatalogProduct? {
        val cleanId = id.trim()
        val cleanTenantId = tenantId?.trim().orEmpty()
        if (cleanId.isBlank() || cleanTenantId.isBlank()) return null
        val seller = resolveSeller(activeTenantId)
        val cleanStatus = normalizeProductStatus(status, active)
        return StoreCatalogProduct(
            id = cleanId,
            tenantId = cleanTenantId,
            name = nome.trim().ifBlank { "Produto" },
            description = descricao?.trim().orEmpty(),
            category = categoria?.trim().orEmpty().ifBlank { "Loja" },
            imageUrl = img?.trim()?.takeIf { it.isNotBlank() },
            price = preco ?: 0.0,
            oldPrice = precoAntigo,
            stock = estoque,
            lote = lote?.trim()?.takeIf { it.isNotBlank() },
            status = cleanStatus,
            tagLabel = tagLabel?.trim()?.takeIf { it.isNotBlank() },
            seller = seller,
            createdAt = createdAt?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun StoreCategoryRow.toDomain(activeTenantId: String): StoreCatalogCategory? {
        val cleanName = nome.trim()
        if (cleanName.isBlank()) return null
        val type = StoreSellerType.fromRemote(sellerType, sellerId, activeTenantId)
        val sellerName = when {
            type == StoreSellerType.Tenant -> "Atlética"
            else -> cleanName
        }
        return StoreCatalogCategory(
            id = id.trim().ifBlank { "${type.remoteValue}:${sellerId.orEmpty()}:$cleanName" },
            name = cleanName,
            seller = StoreSeller(
                type = type,
                id = sellerId?.trim().orEmpty(),
                name = sellerName,
                logoUrl = logoUrl?.trim()?.takeIf { it.isNotBlank() },
            ),
            displayOrder = displayOrder,
            visible = visible ?: true,
        )
    }

    private fun StoreProductRow.resolveSeller(activeTenantId: String): StoreSeller {
        val type = StoreSellerType.fromRemote(sellerType, sellerId, activeTenantId)
        val fallbackName = when (type) {
            StoreSellerType.Tenant -> "Atlética"
            StoreSellerType.Liga -> "Liga"
            StoreSellerType.Comissao -> "Comissão"
            StoreSellerType.Diretorio -> "Diretório"
            StoreSellerType.MiniVendor -> "Mini-vendor"
            StoreSellerType.Unknown -> "Vendedor"
        }
        return StoreSeller(
            type = type,
            id = sellerId?.trim().orEmpty(),
            name = sellerName?.trim()?.takeIf { it.isNotBlank() } ?: fallbackName,
            logoUrl = sellerLogoUrl?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun normalizeProductStatus(status: String?, active: Boolean?): String {
        val cleanStatus = status?.trim()?.lowercase().orEmpty()
        return when {
            active == false -> "esgotado"
            cleanStatus == "em_breve" || cleanStatus == "agendado" -> "em_breve"
            cleanStatus == "esgotado" || cleanStatus == "encerrado" -> "esgotado"
            else -> "ativo"
        }
    }

    fun formatCurrency(value: Double): String = currencyFormatter.format(value)

    private companion object {
        const val AllCategory = "Todos"
        const val MaxPageSize = 60
        const val MaxCategories = 120
        const val ProductsTable = "produtos"
        const val CategoriesTable = "categorias"
        const val ProductColumns =
            "id,tenant_id,nome,preco,precoAntigo,img,descricao,categoria,estoque,lote,tagLabel,active,aprovado,status,seller_type,seller_id,seller_name,seller_logo_url,createdAt"
        const val CategoryColumns =
            "id,tenant_id,nome,cover_img,button_color,logo_url,seller_type,seller_id,display_order,visible"
    }
}

private val StoreSellerType.sortOrder: Int
    get() = when (this) {
        StoreSellerType.Tenant -> 0
        StoreSellerType.MiniVendor -> 1
        StoreSellerType.Liga,
        StoreSellerType.Comissao,
        StoreSellerType.Diretorio,
        StoreSellerType.Unknown -> 2
    }

@Serializable
private data class StoreProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String = "",
    val preco: Double? = null,
    @SerialName("precoAntigo") val precoAntigo: Double? = null,
    val img: String? = null,
    val descricao: String? = null,
    val categoria: String? = null,
    val estoque: Int? = null,
    val lote: String? = null,
    @SerialName("tagLabel") val tagLabel: String? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val status: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class StoreCategoryRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String = "",
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("display_order") val displayOrder: Int? = null,
    val visible: Boolean? = null,
)
