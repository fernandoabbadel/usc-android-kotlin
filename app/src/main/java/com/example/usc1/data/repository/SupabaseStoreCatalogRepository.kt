package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.StoreCatalogCategory
import com.example.usc1.domain.model.StoreCatalogPage
import com.example.usc1.domain.model.StoreCatalogProduct
import com.example.usc1.domain.model.StorePaymentConfig
import com.example.usc1.domain.model.StoreProductVariant
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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
        val rows = runCatching {
            client.from(CategoriesTable)
                .select(columns = Columns.raw(CategoryColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "display_order", order = Order.ASCENDING)
                    limit(count = MaxCategories.toLong())
                }
                .decodeList<StoreCategoryRow>()
        }.getOrElse {
            client.from(CategoriesTable)
                .select(columns = Columns.raw(CategoryFallbackColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "nome", order = Order.ASCENDING)
                    limit(count = MaxCategories.toLong())
                }
                .decodeList<StoreCategoryRow>()
        }

        return rows
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
        if (data.hasEventPartyProduct()) return null
        val seller = resolveSeller(activeTenantId)
        val cleanStatus = normalizeProductStatus(status, active)
        val paymentObject = paymentConfig.asJsonObjectOrEmpty()
        val recipientObject = paymentObject["recipient"].asJsonObjectOrEmpty()
        return StoreCatalogProduct(
            id = cleanId,
            tenantId = cleanTenantId,
            name = nome.trim().ifBlank { "Produto" },
            description = descricao?.trim().orEmpty(),
            category = categoria?.trim().orEmpty().ifBlank { "Loja" },
            imageUrl = resolveRemoteImageUrl(img),
            price = preco ?: 0.0,
            oldPrice = precoAntigo,
            stock = estoque,
            lote = lote?.trim()?.takeIf { it.isNotBlank() },
            status = cleanStatus,
            tagLabel = tagLabel?.trim()?.takeIf { it.isNotBlank() },
            tagColor = tagColor?.trim()?.takeIf { it.isNotBlank() },
            tagEffect = tagEffect?.trim()?.takeIf { it.isNotBlank() },
            colors = cores.stringListValue(),
            variants = variantes.asJsonArrayOrEmpty().mapIndexedNotNull { index, item ->
                item.asJsonObjectOrNull()?.toVariant(index)
            },
            characteristics = caracteristicas.stringListValue(),
            likesCount = likes.countLikeEntries(),
            soldCount = vendidos ?: 0,
            clicksCount = cliques ?: 0,
            paymentConfig = StorePaymentConfig(
                pixKey = paymentObject.stringValue("chave").ifBlank { paymentObject.stringValue("pixKey") },
                bank = paymentObject.stringValue("banco").ifBlank { paymentObject.stringValue("bank") },
                holder = paymentObject.stringValue("titular").ifBlank { paymentObject.stringValue("holder") },
                whatsapp = paymentObject.stringValue("whatsapp").ifBlank { recipientObject.stringValue("whatsapp") },
                recipientName = recipientObject.stringValue("name").ifBlank { recipientObject.stringValue("nome") },
            ).takeIf { it.pixKey.isNotBlank() || it.bank.isNotBlank() || it.holder.isNotBlank() || it.whatsapp.isNotBlank() },
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
                logoUrl = resolveRemoteImageUrl(logoUrl),
            ),
            coverImageUrl = resolveRemoteImageUrl(coverImg),
            buttonColor = buttonColor?.trim()?.takeIf { it.isNotBlank() },
            displayOrder = displayOrder,
            visible = visible ?: true,
            isReceivingOrders = isReceivingOrders ?: true,
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
            logoUrl = resolveRemoteImageUrl(sellerLogoUrl),
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
            "id,tenant_id,nome,preco,precoAntigo,img,descricao,categoria,estoque,lote,tagLabel,tagColor,tagEffect,cores,variantes,caracteristicas,active,aprovado,status,plan_prices,plan_visibility,payment_config,seller_type,seller_id,seller_name,seller_logo_url,vendidos,cliques,likes,data,createdAt,updatedAt"
        const val CategoryColumns =
            "id,tenant_id,nome,cover_img,button_color,logo_url,seller_type,seller_id,display_order,visible,is_receiving_orders"
        const val CategoryFallbackColumns =
            "id,tenant_id,nome,cover_img,button_color,logo_url,seller_type,seller_id"
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
    @SerialName("tagColor") val tagColor: String? = null,
    @SerialName("tagEffect") val tagEffect: String? = null,
    val cores: JsonElement? = null,
    val variantes: JsonElement? = null,
    val caracteristicas: JsonElement? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val status: String? = null,
    @SerialName("plan_prices") val planPrices: JsonElement? = null,
    @SerialName("plan_visibility") val planVisibility: JsonElement? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    val vendidos: Int? = null,
    val cliques: Int? = null,
    val likes: JsonElement? = null,
    val data: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
private data class StoreCategoryRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String = "",
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("button_color") val buttonColor: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("display_order") val displayOrder: Int? = null,
    val visible: Boolean? = null,
    @SerialName("is_receiving_orders") val isReceivingOrders: Boolean? = null,
)

private fun JsonElement?.asJsonObjectOrEmpty(): JsonObject {
    return when (this) {
        is JsonObject -> this
        else -> JsonObject(emptyMap())
    }
}

private fun JsonElement?.asJsonArrayOrEmpty(): JsonArray {
    return when (this) {
        is JsonArray -> this
        else -> JsonArray(emptyList())
    }
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return when (this) {
        is JsonObject -> this
        else -> null
    }
}

private fun JsonElement?.stringListValue(): List<String> {
    if (this == null || this is JsonNull) return emptyList()
    return when (this) {
        is JsonArray -> mapNotNull { item ->
            if (item is JsonNull) null else item.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        }
        else -> jsonPrimitive.contentOrNull
            ?.split(",", ";", "|")
            ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .orEmpty()
    }
}

private fun JsonObject.stringValue(key: String): String {
    val value = this[key] ?: return ""
    if (value is JsonNull) return ""
    return value.jsonPrimitive.contentOrNull.orEmpty().trim()
}

private fun JsonElement?.hasEventPartyProduct(): Boolean {
    val data = this as? JsonObject ?: return false
    val eventParty = data["eventParty"] as? JsonObject ?: return false
    return eventParty.stringValue("eventId").isNotBlank() ||
        eventParty.stringValue("eventName").isNotBlank() ||
        eventParty.stringValue("section").isNotBlank()
}

private fun JsonObject.intValue(key: String): Int? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    return value.jsonPrimitive.intOrNull ?: value.jsonPrimitive.doubleOrNull?.toInt()
}

private fun JsonObject.toVariant(index: Int): StoreProductVariant {
    return StoreProductVariant(
        id = stringValue("id").ifBlank { "variant-$index" },
        color = stringValue("cor").ifBlank { stringValue("color") },
        size = stringValue("tamanho").ifBlank { stringValue("size") },
        stock = intValue("estoque") ?: intValue("stock"),
        sold = intValue("vendidos") ?: intValue("sold") ?: 0,
    )
}

private fun JsonElement?.countLikeEntries(): Int {
    if (this == null || this is JsonNull) return 0
    return when (this) {
        is JsonArray -> size
        else -> jsonPrimitive.intOrNull ?: jsonPrimitive.contentOrNull
            ?.split(",", ";", "|")
            ?.count { it.trim().isNotBlank() }
            ?: 0
    }
}
