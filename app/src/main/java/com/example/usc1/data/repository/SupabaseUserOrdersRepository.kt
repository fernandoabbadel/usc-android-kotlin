package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.UserOrder
import com.example.usc1.domain.model.UserOrderFinanceConfig
import com.example.usc1.domain.model.UserOrderPaymentConfig
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab
import com.example.usc1.domain.model.UserOrderTicketEntry
import com.example.usc1.domain.repository.UserOrdersRepository
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Espelho de `fetchUserOrdersByTab` (`web-reference/src/lib/settingsService.ts`) somado à
 * normalização de `PedidosByTypePage`. Cada aba lê a sua própria tabela, filtrando sempre por
 * `userId` e `tenant_id`.
 */
class SupabaseUserOrdersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : UserOrdersRepository {

    override suspend fun getOrders(
        tenantId: String,
        userId: String,
        tab: UserOrderTab,
        maxResults: Int,
    ): List<UserOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanUserId.isBlank()) {
            return@withContext emptyList()
        }

        val table = when (tab) {
            UserOrderTab.Eventos -> TicketRequestsTable
            UserOrderTab.Loja -> OrdersTable
            UserOrderTab.Planos -> PlanRequestsTable
        }
        val orderField = when (tab) {
            UserOrderTab.Eventos, UserOrderTab.Planos -> "dataSolicitacao"
            UserOrderTab.Loja -> "createdAt"
        }
        val columns = when (tab) {
            UserOrderTab.Eventos -> EventOrderColumns
            UserOrderTab.Loja -> StoreOrderColumns
            UserOrderTab.Planos -> PlanOrderColumns
        }

        val rows = clientProvider().from(table)
            .select(columns = Columns.raw(columns)) {
                filter {
                    eq("userId", cleanUserId)
                    if (cleanTenantId.isNotBlank()) {
                        eq("tenant_id", cleanTenantId)
                    }
                }
                order(column = orderField, order = Order.DESCENDING)
                limit(count = maxResults.coerceIn(1, 200).toLong())
            }
            .decodeList<JsonObject>()

        rows.mapNotNull { row -> row.toUserOrder(tab) }
            .sortedByDescending(UserOrder::createdAtMillis)
    }

    override suspend fun getFinanceConfig(tenantId: String): UserOrderFinanceConfig =
        withContext(Dispatchers.IO) {
            val cleanTenantId = tenantId.trim()
            if (!SupabaseClientProvider.config.isConfigured) {
                return@withContext UserOrderFinanceConfig()
            }

            // Mesmos ids que `resolveFinanceiroDocIds`: escopado por tenant, com o legado global.
            val docIds = if (cleanTenantId.isBlank()) {
                listOf(FinanceiroDocId)
            } else {
                listOf("tenant:$cleanTenantId::$FinanceiroDocId")
            }

            val row = runCatching {
                clientProvider().from(AppConfigTable)
                    .select(columns = Columns.raw("id,data,chave,banco,titular,whatsapp")) {
                        filter { isIn("id", docIds) }
                        limit(count = docIds.size.toLong())
                    }
                    .decodeList<JsonObject>()
                    .firstOrNull()
            }.getOrNull() ?: return@withContext UserOrderFinanceConfig()

            // O registro pode trazer os campos na raiz ou dentro de `data`.
            val nested = row["data"] as? JsonObject
            fun field(key: String): String =
                row.stringValue(key).ifBlank { nested.stringValue(key) }

            UserOrderFinanceConfig(
                pixKey = field("chave"),
                bank = field("banco"),
                holder = field("titular"),
                whatsapp = field("whatsapp"),
            )
        }

    private fun JsonObject.toUserOrder(tab: UserOrderTab): UserOrder? {
        val id = stringValue("id")
        if (id.isBlank()) return null

        val createdAtRaw = firstNonBlank(
            stringValue("dataSolicitacao"),
            stringValue("createdAt"),
            (this["data"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
        )
        val createdAtMillis = createdAtRaw.toEpochMillisOrNull() ?: 0L

        val quantity = intValue("quantidade")
            ?: intValue("itens")
            ?: (this["itens"] as? JsonArray)?.size
            ?: 1

        val title: String
        val subtitle: String
        val amount: Double
        when (tab) {
            UserOrderTab.Eventos -> {
                title = stringValue("eventoNome").ifBlank { "Ingresso" }
                subtitle = "${quantity.coerceAtLeast(1)}x ${stringValue("loteNome").ifBlank { "Lote unico" }}"
                amount = currencyValue("valorTotal")
            }

            UserOrderTab.Loja -> {
                title = stringValue("productName").ifBlank { "Pedido #${id.take(6).uppercase(Locale.ROOT)}" }
                subtitle = "${quantity.coerceAtLeast(1)} item(ns)"
                amount = if (this["total"] != null && this["total"] !is JsonNull) {
                    currencyValue("total")
                } else {
                    currencyValue("price")
                }
            }

            UserOrderTab.Planos -> {
                title = stringValue("planoNome").ifBlank { "Adesao" }
                subtitle = "Anuidade"
                amount = currencyValue("valor")
            }
        }

        val nestedData = this["data"] as? JsonObject

        return UserOrder(
            id = id,
            tab = tab,
            title = title,
            subtitle = subtitle,
            amount = amount,
            status = UserOrderStatus.fromRemote(stringValue("status")),
            createdAtMillis = createdAtMillis,
            createdAtLabel = createdAtMillis.toDateTimeLabel(),
            paymentConfig = (this["payment_config"] as? JsonObject).toPaymentConfig(),
            sellerName = stringValue("seller_name"),
            sellerLogoUrl = resolveRemoteImageUrl(stringValue("seller_logo_url")).orEmpty(),
            buyerName = stringValue("userName"),
            buyerClass = stringValue("userTurma"),
            quantity = quantity.coerceAtLeast(1),
            eventId = stringValue("eventoId"),
            selectedColor = nestedData.stringValue("corSelecionada"),
        )
    }

    private companion object {
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val OrdersTable = "orders"
        const val PlanRequestsTable = "solicitacoes_adesao"
        const val AppConfigTable = "app_config"
        const val FinanceiroDocId = "financeiro"

        const val EventOrderColumns =
            "id,userId,tenant_id,status,eventoNome,eventoId,quantidade,loteNome,valorTotal," +
                "payment_config,userName,userTurma,dataSolicitacao,dataAprovacao,aprovadoPor,createdAt,data"
        const val StoreOrderColumns =
            "id,userId,tenant_id,status,productName,productId,payment_config,seller_name," +
                "seller_logo_url,userName,userTurma,quantidade,itens,total,price,createdAt,data"
        const val PlanOrderColumns =
            "id,userId,tenant_id,status,planoNome,valor,dataSolicitacao,createdAt,data"
    }
}

private fun JsonObject?.toPaymentConfig(): UserOrderPaymentConfig {
    val config = this ?: return UserOrderPaymentConfig()
    val recipient = config["recipient"] as? JsonObject
    return UserOrderPaymentConfig(
        pixKey = config.stringValue("chave"),
        bank = config.stringValue("banco"),
        holder = config.stringValue("titular"),
        whatsapp = config.stringValue("whatsapp"),
        recipientName = recipient.stringValue("name"),
        recipientClass = recipient.stringValue("turma"),
        recipientPhotoUrl = resolveRemoteImageUrl(recipient.stringValue("avatarUrl")).orEmpty(),
        ticketEntries = config.userOrderTicketEntries(),
    )
}

private fun JsonObject.userOrderTicketEntries(): List<UserOrderTicketEntry> {
    val entries = (this["ticketEntries"] ?: this["tickets"] ?: this["ingressos"]) as? JsonArray
        ?: return emptyList()
    return entries.mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val token = obj.stringValue("token").ifBlank { obj.stringValue("ticketToken") }
        if (token.isBlank()) return@mapNotNull null
        UserOrderTicketEntry(
            id = obj.stringValue("id").ifBlank { token },
            token = token,
            label = obj.stringValue("label").ifBlank { obj.stringValue("nome") }.ifBlank { "Ingresso" },
            status = obj.stringValue("status"),
            transferredToUserName = obj.stringValue("transferredToUserName"),
            transferredFromUserName = obj.stringValue("transferredFromUserName"),
        )
    }
}

private fun JsonObject?.stringValue(key: String): String {
    val value = this?.get(key) ?: return ""
    if (value is JsonNull) return ""
    return (value as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
}

private fun JsonObject.intValue(key: String): Int? {
    val value = this[key]
    if (value == null || value is JsonNull) return null
    return (value as? JsonPrimitive)?.intOrNull
}

/**
 * Espelho de `parseCurrencyValue`: aceita número, "1.234,56" e "1,234.56".
 */
private fun JsonObject.currencyValue(key: String): Double {
    val value = this[key]
    if (value == null || value is JsonNull) return 0.0
    val primitive = value as? JsonPrimitive ?: return 0.0
    primitive.doubleOrNull?.let { return if (it.isFinite()) it else 0.0 }

    val sanitized = primitive.contentOrNull.orEmpty().trim().filter { it.isDigit() || it in ",.-" }
    if (sanitized.isBlank()) return 0.0

    val lastComma = sanitized.lastIndexOf(',')
    val lastDot = sanitized.lastIndexOf('.')
    val normalized = when {
        lastComma >= 0 && lastDot >= 0 ->
            if (lastComma > lastDot) sanitized.replace(".", "").replace(",", ".")
            else sanitized.replace(",", "")

        lastComma >= 0 -> sanitized.replace(",", ".")
        else -> sanitized
    }
    return normalized.toDoubleOrNull()?.takeIf(Double::isFinite) ?: 0.0
}

private fun firstNonBlank(vararg values: String): String =
    values.firstOrNull { it.isNotBlank() }.orEmpty()

private val userOrdersZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val userOrdersFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
    .withZone(userOrdersZone)

private fun String.toEpochMillisOrNull(): Long? {
    val clean = trim()
    if (clean.isBlank()) return null
    return runCatching { OffsetDateTime.parse(clean).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(clean).toEpochMilli() }
        .getOrNull()
}

private fun Long.toDateTimeLabel(): String {
    if (this <= 0L) return "Data não informada"
    return userOrdersFormatter.format(Instant.ofEpochMilli(this))
}
