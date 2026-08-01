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
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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

    override suspend fun createOrder(
        tenantId: String,
        userId: String,
        userName: String,
        item: CartItem,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
    ): StoreOrder = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanUserName = userName.trim().ifBlank { "Aluno" }
        val product = item.product
        val productId = product.id.trim()
        val quantity = item.quantity.coerceAtLeast(1)

        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para registrar pedidos.")
        }
        if (cleanTenantId.isBlank()) {
            throw IllegalStateException("Entre em uma atlética para comprar na loja.")
        }
        if (cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para registrar o pedido.")
        }
        if (productId.isBlank()) {
            throw IllegalStateException("Produto inválido para pedido.")
        }

        val client = clientProvider()
        val productRow = client.from(ProductsTable)
            .select(columns = Columns.raw(ProductLookupColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", productId)
                }
                limit(count = 1)
            }
            .decodeList<StoreOrderProductRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Produto fora do tenant ativo.")

        validateVariantSelection(
            variants = productRow.variantes.asJsonArrayOrEmpty(),
            selectedLabel = item.variantLabel,
            quantity = quantity,
        )

        val unitPrice = resolvePlanScopedPrice(
            basePrice = productRow.preco ?: product.priceValue,
            entries = productRow.planPrices.asJsonArrayOrEmpty(),
            userPlanNames = userPlanNames,
            userPlanIds = userPlanIds,
        )
        val orderTotal = (unitPrice * quantity).roundMoney()
        val now = OffsetDateTime.now().toString()
        val orderId = UUID.randomUUID().toString()
        val orderData = buildOrderData(item)
        val sellerType = firstNotBlank(productRow.sellerType, product.sellerType.remoteValue)
        val sellerId = firstNotBlank(productRow.sellerId, product.sellerId, cleanTenantId)
        val sellerName = firstNotBlank(productRow.sellerName, product.sellerName, product.sellerType.label)
        val sellerLogoUrl = firstNotBlank(productRow.sellerLogoUrl, product.sellerLogoUrl)
        val paymentConfig = buildPaymentConfig(productRow.paymentConfig, product)

        val basePayload = storeOrderJsonPayloadOf(
            "id" to orderId,
            "tenant_id" to cleanTenantId,
            "userId" to cleanUserId,
            "userName" to cleanUserName,
            "productId" to productId,
            "productName" to product.name.trim().ifBlank { "Produto" },
            "price" to orderTotal,
            "quantidade" to quantity,
            "itens" to quantity,
            "status" to RemotePendingStatus,
            "createdAt" to now,
            "updatedAt" to now,
            "seller_type" to sellerType,
            "seller_id" to sellerId,
            "seller_name" to sellerName,
            "seller_logo_url" to resolveRemoteImageUrl(sellerLogoUrl),
            "payment_config" to paymentConfig,
            "data" to orderData.takeIf { it.isNotEmpty() },
        )

        insertWithOptionalColumnFallback(client, basePayload)
        insertStoreOrderNotification(
            client = client,
            userId = cleanUserId,
            productId = productId,
            productName = product.name.trim().ifBlank { "Produto" },
            now = now,
        )

        getOrderById(cleanTenantId, cleanUserId, orderId)
            ?: StoreOrderRow(
                id = orderId,
                tenantId = cleanTenantId,
                userId = cleanUserId,
                userName = cleanUserName,
                productId = productId,
                productName = product.name,
                price = orderTotal,
                quantidade = quantity,
                status = RemotePendingStatus,
                createdAt = now,
                updatedAt = now,
                sellerType = sellerType,
                sellerId = sellerId,
                sellerName = sellerName,
                sellerLogoUrl = sellerLogoUrl,
            ).toUiOrder(cleanTenantId)
            ?: throw IllegalStateException("Pedido criado, mas não foi possível carregar o resumo.")
    }

    private fun StoreOrderRow.toUiOrder(activeTenantId: String): StoreOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val linkedEventId = firstNotBlank(eventId, eventoId)
        if (linkedEventId.isNotBlank() || eventItemType.orEmpty().trim().isNotBlank()) return null
        val cleanQuantity = (quantidade ?: itens ?: 1).coerceAtLeast(1)
        val cleanProductName = firstNotBlank(productName, productname, "Pedido da loja")
        val cleanProductId = firstNotBlank(productId, productid, cleanId)
        val cleanSellerType = StoreSellerType.fromRemote(sellerType, sellerId, activeTenantId)
        val cleanSellerName = firstNotBlank(sellerName, cleanSellerType.label)
        val totalValue = total ?: (price * cleanQuantity)
        val mappedStatus = storeOrderStatusFromRemote(status)
        val payment = paymentConfig.toOrderPaymentPieces(cleanSellerName)
        val orderData = data.asJsonObjectOrNull()
        val orderColor = orderData.stringValue("corSelecionada")
            .ifBlank { orderData.stringValue("colorLabel") }
            .ifBlank { orderData.stringValue("variantColor") }
        val orderVariant = orderData.stringValue("varianteLabel")
            .ifBlank { orderData.stringValue("variantLabel") }
            .ifBlank {
                listOf(
                    orderData.stringValue("tamanhoSelecionado").ifBlank { orderData.stringValue("variantSize") }
                        .takeIf(String::isNotBlank)?.let { "Tamanho $it" }.orEmpty(),
                    orderData.stringValue("corVariante").ifBlank { orderData.stringValue("variantColor") }
                        .takeIf(String::isNotBlank)?.let { "Cor $it" }.orEmpty(),
                ).filter(String::isNotBlank).joinToString(" • ")
            }

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
                        imageUrl = resolveRemoteImageUrl(sellerLogoUrl),
                        stockLabel = if (cleanQuantity == 1) "1 item" else "$cleanQuantity itens",
                        reviewLabel = "Vendedor: $cleanSellerName",
                        pixKey = payment.pixKey,
                        pixBank = payment.bank,
                        pixHolder = payment.holder,
                        receiptWhatsapp = payment.whatsapp,
                        receiptName = payment.recipientName,
                        sellerType = cleanSellerType,
                        sellerId = sellerId?.trim().orEmpty(),
                        sellerName = cleanSellerName,
                        sellerLogoUrl = resolveRemoteImageUrl(sellerLogoUrl),
                    ),
                    quantity = cleanQuantity,
                    variantLabel = orderVariant,
                    colorLabel = orderColor,
                ),
            ),
            pickupLabel = mappedStatus.pickupLabel,
            sellerName = cleanSellerName,
            sellerTypeLabel = cleanSellerType.label,
        )
    }

    private companion object {
        const val OrdersTable = "orders"
        const val ProductsTable = "produtos"
        const val NotificationsTable = "notifications"
        const val RemotePendingStatus = "pendente"
        const val OrderColumns =
            "id,tenant_id,userId,userName,username,productId,productid,productName,productname,price,total,quantidade,itens,data,status,payment_config,createdAt,updatedAt,seller_type,seller_id,seller_name,seller_logo_url,eventId,eventoId,eventItemType"
        const val ProductLookupColumns =
            "id,tenant_id,preco,plan_prices,payment_config,seller_type,seller_id,seller_name,seller_logo_url,variantes"

        val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(storeOrdersPtBr)
    }
}

@Serializable
private data class StoreOrderProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val preco: Double? = null,
    @SerialName("plan_prices") val planPrices: JsonElement? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    val variantes: JsonElement? = null,
)

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
    val data: JsonElement? = null,
    val status: String? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("eventId") val eventId: String? = null,
    @SerialName("eventoId") val eventoId: String? = null,
    @SerialName("eventItemType") val eventItemType: String? = null,
)

private data class StoreOrderPaymentPieces(
    val pixKey: String = "",
    val bank: String = "",
    val holder: String = "",
    val whatsapp: String = "",
    val recipientName: String = "",
)

@Serializable
private data class StoreNotificationInsertRow(
    @SerialName("userId") val userId: String,
    val title: String,
    val message: String,
    val link: String,
    val read: Boolean,
    val type: String,
    @SerialName("createdAt") val createdAt: String,
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

private suspend fun insertWithOptionalColumnFallback(
    client: SupabaseClient,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val nonRemovableColumns = setOf(
        "id",
        "tenant_id",
        "userId",
        "userName",
        "productId",
        "productName",
        "price",
        "status",
    )
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from("orders").insert(JsonObject(mutablePayload))
            return
        } catch (error: Throwable) {
            val missingColumn = extractProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in nonRemovableColumns ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                throw error
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private fun storeOrderJsonPayloadOf(
    vararg pairs: Pair<String, Any?>,
): LinkedHashMap<String, JsonElement> {
    val payload = LinkedHashMap<String, JsonElement>()
    pairs.forEach { (key, value) ->
        val element = value.toStoreOrderJsonElementOrNull() ?: return@forEach
        payload[key] = element
    }
    return payload
}

private fun Any?.toStoreOrderJsonElementOrNull(): JsonElement? {
    return when (this) {
        null -> null
        is JsonElement -> this.takeUnless { it is JsonNull }
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this.toDouble())
        else -> JsonPrimitive(toString())
    }
}

private suspend fun insertStoreOrderNotification(
    client: SupabaseClient,
    userId: String,
    productId: String,
    productName: String,
    now: String,
) {
    runCatching {
        client.from("notifications").insert(
            StoreNotificationInsertRow(
                userId = userId,
                title = "Compra em Análise",
                message = "Seu pedido de $productName foi enviado para aprovação.",
                link = "/loja/$productId",
                read = false,
                type = "order",
                createdAt = now,
            ),
        )
    }
}

private fun extractProblematicColumn(error: Throwable): String? {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
    val patterns = listOf(
        Regex("column\\s+[a-z0-9_]+\\.([a-zA-Z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("column\\s+[\"']?([a-zA-Z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("could not find the [\"']?([a-zA-Z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(message)?.groupValues?.getOrNull(1)
    }
}

private fun buildOrderData(item: CartItem): JsonObject {
    val entries = linkedMapOf<String, JsonElement>()
    val color = item.colorLabel.trim()
    val variant = item.variantLabel.trim()
    if (color.isNotBlank()) entries["corSelecionada"] = JsonPrimitive(color)
    if (variant.isNotBlank()) {
        entries["varianteLabel"] = JsonPrimitive(variant)
        val pieces = variant.split("•").map { it.trim() }.filter { it.isNotBlank() }
        pieces.firstOrNull { it.contains("tamanho", ignoreCase = true).not() }?.let {
            entries["tamanhoSelecionado"] = JsonPrimitive(it)
        }
    }
    return JsonObject(entries)
}

private fun buildPaymentConfig(
    remoteConfig: JsonElement?,
    product: StoreProduct,
): JsonObject? {
    val remoteObject = remoteConfig.asJsonObjectOrNull()
    val pixKey = remoteObject.stringValue("chave").ifBlank { product.pixKey.trim() }
    val bank = remoteObject.stringValue("banco").ifBlank { product.pixBank.trim() }
    val holder = remoteObject.stringValue("titular").ifBlank { product.pixHolder.trim() }
    val whatsapp = remoteObject.stringValue("whatsapp").ifBlank { product.receiptWhatsapp.trim() }
    val recipientName = remoteObject.asJsonObjectOrEmpty("recipient").stringValue("name")
        .ifBlank { product.receiptName.trim() }
        .ifBlank { product.sellerName.trim() }

    val entries = linkedMapOf<String, JsonElement>()
    if (pixKey.isNotBlank()) entries["chave"] = JsonPrimitive(pixKey)
    if (bank.isNotBlank()) entries["banco"] = JsonPrimitive(bank)
    if (holder.isNotBlank()) entries["titular"] = JsonPrimitive(holder)
    if (whatsapp.isNotBlank()) entries["whatsapp"] = JsonPrimitive(whatsapp)
    if (recipientName.isNotBlank() || whatsapp.isNotBlank()) {
        val recipientEntries = linkedMapOf<String, JsonElement>()
        if (recipientName.isNotBlank()) recipientEntries["name"] = JsonPrimitive(recipientName)
        if (whatsapp.isNotBlank()) recipientEntries["phone"] = JsonPrimitive(whatsapp)
        entries["recipient"] = JsonObject(recipientEntries)
    }
    return JsonObject(entries).takeIf { it.isNotEmpty() }
}

private fun JsonElement?.toOrderPaymentPieces(fallbackRecipientName: String): StoreOrderPaymentPieces {
    val remoteObject = asJsonObjectOrNull()
    val recipientObject = remoteObject.asJsonObjectOrEmpty("recipient")
    return StoreOrderPaymentPieces(
        pixKey = remoteObject.stringValue("chave").ifBlank { remoteObject.stringValue("pixKey") },
        bank = remoteObject.stringValue("banco").ifBlank { remoteObject.stringValue("bank") },
        holder = remoteObject.stringValue("titular").ifBlank { remoteObject.stringValue("holder") },
        whatsapp = remoteObject.stringValue("whatsapp")
            .ifBlank { recipientObject.stringValue("phone") }
            .ifBlank { recipientObject.stringValue("whatsapp") },
        recipientName = recipientObject.stringValue("name").ifBlank { fallbackRecipientName },
    )
}

private fun validateVariantSelection(
    variants: List<JsonElement>,
    selectedLabel: String,
    quantity: Int,
) {
    val label = selectedLabel.trim()
    if (label.isBlank() || variants.isEmpty()) return
    val selected = variants.firstOrNull { element ->
        val variant = element.asJsonObjectOrNull() ?: return@firstOrNull false
        val size = variant.stringValue("tamanho").ifBlank { variant.stringValue("size") }
        val color = variant.stringValue("cor").ifBlank { variant.stringValue("color") }
        val id = variant.stringValue("id")
        val remoteLabel = listOf(color, size).filter { it.isNotBlank() }.joinToString(" • ")
        label.equals(remoteLabel, ignoreCase = true) ||
            label.equals(id, ignoreCase = true) ||
            label.contains(size, ignoreCase = true) && label.contains(color, ignoreCase = true)
    } ?: throw IllegalStateException("A variação escolhida não está mais disponível.")

    val stock = selected.asJsonObjectOrNull()?.intValue("estoque")
        ?: selected.asJsonObjectOrNull()?.intValue("stock")
    if (stock != null && stock < quantity) {
        throw IllegalStateException("Estoque insuficiente para a variação escolhida.")
    }
}

private fun resolvePlanScopedPrice(
    basePrice: Double,
    entries: List<JsonElement>,
    userPlanNames: List<String>,
    userPlanIds: List<String>,
): Double {
    val referenceKeys = buildPlanReferenceKeys(userPlanNames + userPlanIds)
    if (referenceKeys.isEmpty() || entries.isEmpty()) return basePrice

    val matchedPrice = entries.firstNotNullOfOrNull { element ->
        val entry = element.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null
        val planId = entry.stringValue("planId").ifBlank { entry.stringValue("id") }
        val planName = entry.stringValue("planName").ifBlank { entry.stringValue("nome") }
        val entryKeys = buildPlanReferenceKeys(listOf(planId, planName))
        val matches = entryKeys.any { it in referenceKeys }
        if (!matches) return@firstNotNullOfOrNull null
        entry.doubleValue("price") ?: entry.doubleValue("preco")
    }
    return matchedPrice ?: basePrice
}

private fun buildPlanReferenceKeys(values: List<String>): Set<String> {
    return values
        .flatMap { value ->
            val clean = value.trim()
            val baseId = clean.substringAfterLast("::", clean)
            listOf(clean, baseId, clean.normalizePlanToken(), baseId.normalizePlanToken())
        }
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .toSet()
}

private fun String.normalizePlanToken(): String {
    return lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "")
        .trim()
}

private fun Double.roundMoney(): Double = kotlin.math.round(this * 100.0) / 100.0

private fun JsonElement?.asJsonArrayOrEmpty(): List<JsonElement> {
    return when (this) {
        is JsonArray -> this.toList()
        else -> emptyList()
    }
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? {
    return this as? JsonObject
}

private fun JsonElement?.asJsonObjectOrEmpty(key: String): JsonObject {
    return (this.asJsonObjectOrNull()?.get(key) as? JsonObject) ?: JsonObject(emptyMap())
}

private fun JsonObject?.stringValue(key: String): String {
    val value = this?.get(key) ?: return ""
    return value.jsonPrimitiveOrNull()?.contentOrNull?.trim().orEmpty()
}

private fun JsonObject?.doubleValue(key: String): Double? {
    val value = this?.get(key) ?: return null
    return value.jsonPrimitiveOrNull()?.doubleOrNull
}

private fun JsonObject?.intValue(key: String): Int? {
    val value = this?.get(key) ?: return null
    return value.jsonPrimitiveOrNull()?.intOrNull
}

private fun JsonElement.jsonPrimitiveOrNull() = runCatching { jsonPrimitive }.getOrNull()
