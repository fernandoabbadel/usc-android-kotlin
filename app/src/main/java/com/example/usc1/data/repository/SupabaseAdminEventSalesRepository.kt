package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminEventSalesDashboard
import com.example.usc1.domain.model.AdminEventSalesEvent
import com.example.usc1.domain.model.AdminEventSalesOrder
import com.example.usc1.domain.model.AdminEventSalesOrderStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminEventSalesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    suspend fun getDashboard(limit: Int = 240): AdminEventSalesDashboard = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            return@withContext AdminEventSalesDashboard()
        }

        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val eventsRepository = SupabaseEventsRepository(clientProvider)
        val events = eventsRepository.getEvents()
            .filter { event -> event.isEventMenuEnabled || event.menuProducts.isNotEmpty() }
            .map { event -> runCatching { eventsRepository.getEventById(event.id) }.getOrNull() ?: event }
            .filter { event -> event.isEventMenuEnabled || event.menuProducts.isNotEmpty() }

        val eventById = events.associateBy { it.id }
        val eventSummaries = events.map { event ->
            AdminEventSalesEvent(
                id = event.id,
                title = event.title,
                menuTitle = event.eventMenuTitle.ifBlank { "Menu do evento" },
                category = event.eventMenuCategory.ifBlank { "Modo Vendas" },
                productCount = event.menuProducts.size,
                stockCount = event.menuProducts.sumOf { product -> product.stockCount.coerceAtLeast(0) },
                statusLabel = event.status.label,
            )
        }

        val rows = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.coerceIn(1, 500).toLong())
            }
            .decodeList<AdminEventSalesOrderRow>()

        val orders = rows.mapNotNull { row -> row.toDomain(eventById) }
        val approvedOrders = orders.filter { order -> order.status == AdminEventSalesOrderStatus.Approved || order.status == AdminEventSalesOrderStatus.Delivered }
        val pendingOrders = orders.filter { order -> order.status == AdminEventSalesOrderStatus.Pending }
        AdminEventSalesDashboard(
            events = eventSummaries,
            orders = orders,
            totalRevenueLabel = currencyFormatter.format(orders.sumOf { order -> order.totalValue }),
            pendingRevenueLabel = currencyFormatter.format(pendingOrders.sumOf { order -> order.totalValue }),
            approvedRevenueLabel = currencyFormatter.format(approvedOrders.sumOf { order -> order.totalValue }),
            totalItems = orders.sumOf { order -> order.quantity },
            pendingOrders = pendingOrders.size,
            approvedOrders = approvedOrders.size,
        )
    }

    private fun AdminEventSalesOrderRow.toDomain(
        eventById: Map<String, com.example.usc1.domain.model.Event>,
    ): AdminEventSalesOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val orderData = data.asObject() ?: return null
        val eventParty = orderData.objectValue("eventParty")
        val eventId = eventId?.trim().orEmpty()
            .ifBlank { eventoId?.trim().orEmpty() }
            .ifBlank { eventParty.stringValue("eventId") }
        if (eventId.isBlank()) return null

        val event = eventById[eventId]
        val cleanProductName = eventItemName?.trim().orEmpty()
            .ifBlank { productName?.trim().orEmpty() }
            .ifBlank { eventParty.stringValue("productName") }
            .ifBlank { "Produto do evento" }
        val cleanCategory = eventItemCategory?.trim().orEmpty()
            .ifBlank { eventParty.stringValue("section") }
            .ifBlank { eventParty.stringValue("categoryName") }
            .ifBlank { "Modo Vendas" }
        val quantity = (quantidade ?: itens ?: eventParty.intValue("quantity") ?: 1).coerceAtLeast(1)
        val totalAmount = total ?: (price * quantity).takeIf { it > 0.0 } ?: eventParty.doubleValue("total") ?: 0.0
        val statusValue = status?.trim().orEmpty()
        val mappedStatus = AdminEventSalesOrderStatus.fromRemote(statusValue)
        val voucherStatus = eventParty.stringValue("voucherStatus")
            .ifBlank { firstVoucherStatus(eventParty["voucherEntries"]) }
            .ifBlank { firstVoucherStatus(eventParty["vouchers"]) }
            .ifBlank { mappedStatus.label }

        return AdminEventSalesOrder(
            id = cleanId,
            eventId = eventId,
            eventTitle = event?.title ?: eventParty.stringValue("eventTitle").ifBlank { "Evento USC" },
            userName = userName?.trim().orEmpty().ifBlank { "Comprador" },
            productId = productId?.trim().orEmpty().ifBlank { eventParty.stringValue("productId") },
            productName = cleanProductName,
            category = cleanCategory,
            quantity = quantity,
            totalValue = totalAmount,
            totalLabel = currencyFormatter.format(totalAmount),
            status = mappedStatus,
            approvalLabel = approvalLabel(mappedStatus, approvedBy, updatedAt, eventParty),
            receiverLabel = paymentConfig.receiverLabel(),
            sourceLabel = source?.trim().orEmpty()
                .ifBlank { eventParty.stringValue("source") }
                .ifBlank { if (eventParty.booleanValue("manualOrder")) "Criado manualmente" else "Checkout público" },
            voucherStatusLabel = voucherStatus,
            createdAtLabel = createdAt?.trim().orEmpty().toAdminEventSalesDateLabel(),
        )
    }

    private fun approvalLabel(
        status: AdminEventSalesOrderStatus,
        approvedBy: String?,
        updatedAt: String?,
        eventParty: JsonObject,
    ): String {
        return when (status) {
            AdminEventSalesOrderStatus.Approved,
            AdminEventSalesOrderStatus.Delivered,
            -> {
                val actor = approvedBy?.trim().orEmpty()
                    .ifBlank { eventParty.stringValue("approvedByName") }
                    .ifBlank { "admin" }
                val date = eventParty.stringValue("approvedAt").ifBlank { updatedAt?.trim().orEmpty() }
                "Aprovado por $actor${date.toAdminEventSalesDateLabel().takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()}"
            }
            AdminEventSalesOrderStatus.Rejected -> "Comprovante rejeitado"
            AdminEventSalesOrderStatus.Cancelled -> "Pedido cancelado"
            AdminEventSalesOrderStatus.Pending -> "Análise financeira"
        }
    }

    private companion object {
        const val OrdersTable = "orders"
        const val OrderColumns =
            "id,tenant_id,userId,userName,productId,productName,price,total,quantidade,itens,data,status,approvedBy,payment_config,paymentSource,paymentMethod,source,createdAt,updatedAt,eventId,eventoId,eventItemType,eventItemName,eventLoteNome,eventItemCategory"
    }
}

@Serializable
private data class AdminEventSalesOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double = 0.0,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val data: JsonElement? = null,
    val status: String? = null,
    val approvedBy: String? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    val paymentSource: String? = null,
    val paymentMethod: String? = null,
    val source: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val eventId: String? = null,
    val eventoId: String? = null,
    val eventItemType: String? = null,
    val eventItemName: String? = null,
    val eventLoteNome: String? = null,
    val eventItemCategory: String? = null,
)

private val adminEventSalesZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val adminEventSalesDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.objectValue(key: String): JsonObject {
    return this[key].asObject() ?: JsonObject(emptyMap())
}

private fun JsonObject.stringValue(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}

private fun JsonObject.intValue(key: String): Int? {
    return stringValue(key).toIntOrNull()
}

private fun JsonObject.doubleValue(key: String): Double? {
    return this[key]?.jsonPrimitive?.doubleOrNull ?: stringValue(key).replace(",", ".").toDoubleOrNull()
}

private fun JsonObject.booleanValue(key: String): Boolean {
    val text = stringValue(key).lowercase(Locale.ROOT)
    return text == "true" || text == "1" || text == "sim"
}

private fun JsonElement?.receiverLabel(): String {
    val payment = asObject() ?: return "Não informado"
    val recipient = payment.objectValue("recipient")
    return listOf(
        recipient.stringValue("name"),
        recipient.stringValue("turma"),
        recipient.stringValue("phone"),
    ).filter(String::isNotBlank).joinToString(" - ").ifBlank { "Não informado" }
}

private fun firstVoucherStatus(element: JsonElement?): String {
    val entries = element as? JsonArray ?: return ""
    return entries.firstOrNull()?.asObject()?.stringValue("status").orEmpty()
}

private fun String.toAdminEventSalesDateLabel(): String {
    if (isBlank()) return ""
    return runCatching {
        adminEventSalesDateFormatter.format(Instant.parse(this).atZone(adminEventSalesZone))
    }.getOrNull().orEmpty()
}
