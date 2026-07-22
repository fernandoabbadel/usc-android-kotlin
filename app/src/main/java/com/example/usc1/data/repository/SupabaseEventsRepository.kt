package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventOwnerType
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.repository.EventsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SupabaseEventsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : EventsRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    private val dateLabelFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))

    override suspend fun getEvents(status: EventStatus?): List<Event> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val rows = client.from(EventsTable)
            .select(columns = Columns.raw(EventFeedColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", "ativo")
                }
                order(column = "data", order = Order.ASCENDING)
                limit(count = FetchLimit)
            }
            .decodeList<EventRow>()

        rows.mapNotNull { row -> row.toDomain(tenantId) }
            .filter { event -> status == null || event.status == status }
            .filter { event -> event.status != EventStatus.Closed }
            .sortedBy { event -> event.sortKey() }
            .take(PageSize)
    }

    override suspend fun getEventById(eventId: String): Event? = withContext(Dispatchers.IO) {
        val cleanEventId = eventId.trim()
        if (cleanEventId.isBlank()) return@withContext null

        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(EventsTable)
            .select(columns = Columns.raw(EventDetailColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanEventId)
                }
                limit(count = 1)
            }
            .decodeList<EventRow>()
            .firstOrNull()
            ?.toDomain(tenantId)
            ?.takeUnless { it.status == EventStatus.Closed }
    }

    private fun EventRow.toDomain(activeTenantId: String): Event? {
        val cleanId = id.trim()
        val cleanTenantId = tenantId?.trim().orEmpty()
        if (cleanId.isBlank() || cleanTenantId.isBlank() || isVisibilityBlocked()) return null

        val statsObject = stats.asJsonObjectOrEmpty()
        val ownerType = resolveOwnerType(statsObject)
        val ownerId = when (ownerType) {
            EventOwnerType.Tenant -> cleanTenantId
            EventOwnerType.Liga,
            EventOwnerType.Comissao,
            EventOwnerType.Diretorio -> statsObject.stringValue("leagueId").ifBlank { cleanTenantId }
        }
        val lots = lotes.asJsonArrayOrEmpty()
            .mapNotNull { it.asJsonObjectOrNull() }
            .mapNotNull { it.toLot() }
        val activeLot = lots.firstOrNull { it.status == "ativo" }
        val firstLot = activeLot ?: lots.firstOrNull()
        val normalizedSaleStatus = normalizeSaleStatus(saleStatus, status, activeLot)
        val eventStatus = normalizeEventStatus(status, normalizedSaleStatus)
        val eventDateTime = parseEventDateTime(data, hora)
        val confirmed = statsObject.intValue("confirmados")
        val maybe = statsObject.intValue("talvez")
        val likes = statsObject.intValue("likes")

        return Event(
            id = cleanId,
            tenantId = cleanTenantId,
            title = titulo.trim().ifBlank { "Evento" },
            description = descricao?.trim().orEmpty(),
            dateLabel = formatDateLabel(data, eventDateTime),
            timeLabel = hora?.trim()?.takeIf { it.isNotBlank() } ?: "00:00",
            location = local?.trim().orEmpty().ifBlank { "Local a definir" },
            priceLabel = resolvePriceLabel(normalizedSaleStatus, firstLot),
            status = eventStatus,
            saleStatus = normalizedSaleStatus,
            imageUrl = imagem?.trim()?.takeIf { it.isNotBlank() },
            coverColorName = resolveCoverLabel(ownerType),
            lotName = firstLot?.name ?: if (normalizedSaleStatus == "em_breve") "Em breve" else "Lote",
            availableSpots = confirmed + maybe,
            ownerType = ownerType,
            ownerId = ownerId,
            ownerName = ownerType.label,
            likesCount = likes,
            products = lots.map { lot ->
                EventProduct(
                    id = lot.id.ifBlank { lot.name },
                    name = lot.name,
                    priceLabel = lot.priceLabel(currencyFormatter),
                    status = lot.status,
                )
            },
        )
    }

    private fun EventRow.isVisibilityBlocked(): Boolean {
        val dataExtra = dataExtra.asJsonObjectOrEmpty()
        val statsObject = stats.asJsonObjectOrEmpty()
        val blocked = dataExtra.booleanValue("adminVisibilityBlock") ||
            dataExtra.booleanValue("visibilityBlocked") ||
            statsObject.booleanValue("adminVisibilityBlock") ||
            statsObject.booleanValue("visibilityBlocked")
        return blocked
    }

    private fun EventRow.resolveOwnerType(statsObject: JsonObject): EventOwnerType {
        val raw = listOf(
            tipo,
            categoria,
            statsObject.stringValue("owner_type"),
            statsObject.stringValue("organizer_type"),
            statsObject.stringValue("scope_type"),
            statsObject.stringValue("eventScope"),
        ).joinToString(" ").normalizeForMatch()

        return when {
            raw.contains("diretorio") || raw.contains("directory") -> EventOwnerType.Diretorio
            raw.contains("comissao") || raw.contains("commission") || raw.contains("formatura") -> EventOwnerType.Comissao
            raw.contains("liga") || raw.contains("league") || statsObject.stringValue("leagueId").isNotBlank() -> EventOwnerType.Liga
            else -> EventOwnerType.Tenant
        }
    }

    private fun normalizeSaleStatus(
        saleStatus: String?,
        status: String?,
        activeLot: EventLot?,
    ): String {
        val cleanSaleStatus = saleStatus?.trim()?.lowercase().orEmpty()
        val cleanStatus = status?.trim()?.lowercase().orEmpty()
        return when {
            cleanStatus in ClosedStatuses -> "esgotado"
            cleanSaleStatus == "em_breve" || cleanSaleStatus == "agendado" -> "em_breve"
            cleanSaleStatus == "esgotado" || cleanSaleStatus == "encerrado" -> "esgotado"
            activeLot != null -> "ativo"
            cleanSaleStatus == "ativo" -> "ativo"
            else -> "em_breve"
        }
    }

    private fun normalizeEventStatus(status: String?, saleStatus: String): EventStatus {
        val cleanStatus = status?.trim()?.lowercase().orEmpty()
        return when {
            cleanStatus in ClosedStatuses -> EventStatus.Closed
            saleStatus == "em_breve" -> EventStatus.ComingSoon
            saleStatus == "esgotado" -> EventStatus.SoldOut
            else -> EventStatus.Open
        }
    }

    private fun resolvePriceLabel(saleStatus: String, lot: EventLot?): String {
        if (saleStatus == "em_breve") return "Em breve"
        if (lot == null) return if (saleStatus == "esgotado") "Esgotado" else "Em breve"
        if (lot.status == "esgotado") return "Esgotado"
        return lot.priceLabel(currencyFormatter)
    }

    private fun formatDateLabel(rawDate: String?, dateTime: LocalDateTime?): String {
        if (dateTime != null) {
            return dateTime.toLocalDate().format(dateLabelFormatter).replace(".", "")
        }
        return rawDate?.trim().orEmpty().ifBlank { "Data a definir" }
    }

    private fun Event.sortKey(): Long = parseEventDateTime(dateLabel, timeLabel)
        ?.toLocalDate()
        ?.toEpochDay()
        ?: Long.MAX_VALUE

    private fun parseEventDateTime(rawDate: String?, rawTime: String?): LocalDateTime? {
        val cleanDate = rawDate?.trim().orEmpty()
        if (cleanDate.isBlank()) return null
        val cleanTime = rawTime?.trim().orEmpty().ifBlank { "00:00" }
        val time = runCatching { LocalTime.parse(cleanTime.take(5)) }.getOrDefault(LocalTime.MIDNIGHT)

        DateFormats.forEach { formatter ->
            try {
                return LocalDate.parse(cleanDate, formatter).atTime(time)
            } catch (_: DateTimeParseException) {
            }
        }

        val parts = cleanDate.normalizeForMatch().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val day = parts[0].toIntOrNull()
            val month = MonthMap[parts[1].take(3)]
            val year = parts.getOrNull(2)?.toIntOrNull() ?: Year.now().value
            if (day != null && month != null) {
                return runCatching { LocalDate.of(year, month, day).atTime(time) }.getOrNull()
            }
        }
        return null
    }

    private fun JsonObject.toLot(): EventLot? {
        val name = stringValue("nome").ifBlank { stringValue("name") }
        if (name.isBlank()) return null
        val price = stringValue("preco").ifBlank { stringValue("price") }
        return EventLot(
            id = stringValue("id"),
            name = name,
            price = price.replace(",", ".").toDoubleOrNull() ?: 0.0,
            status = stringValue("status").ifBlank { "ativo" }.lowercase(),
        )
    }

    private fun EventLot.priceLabel(formatter: NumberFormat): String = formatter.format(price)

    private fun resolveCoverLabel(ownerType: EventOwnerType): String {
        return ownerType.label
    }

    private companion object {
        const val PageSize = 24
        const val FetchLimit = 72L
        const val EventsTable = "eventos"
        const val EventFeedColumns =
            "id,titulo,data,hora,local,imagem,tipo,categoria,destaque,status,sale_status,isLowStock,stats,lotes,data_extra,capacidade,tenant_id,createdAt,updatedAt"
        const val EventDetailColumns =
            "id,titulo,descricao,data,hora,local,imagem,imagePositionY,tipo,categoria,destaque,mapsUrl,status,sale_status,payment_config,pixChave,pixBanco,pixTitular,contatoComprovante,isLowStock,stats,lotes,data_extra,capacidade,tenant_id,createdAt,updatedAt"
        val ClosedStatuses = setOf("encerrado", "cancelado", "inativo")
        val DateFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        )
        val MonthMap = mapOf(
            "jan" to 1,
            "fev" to 2,
            "mar" to 3,
            "abr" to 4,
            "mai" to 5,
            "jun" to 6,
            "jul" to 7,
            "ago" to 8,
            "set" to 9,
            "out" to 10,
            "nov" to 11,
            "dez" to 12,
        )
    }
}

private data class EventLot(
    val id: String,
    val name: String,
    val price: Double,
    val status: String,
)

@Serializable
private data class EventRow(
    val id: String = "",
    val titulo: String = "",
    val descricao: String? = null,
    val data: String? = null,
    val hora: String? = null,
    val local: String? = null,
    val imagem: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val destaque: String? = null,
    val status: String? = null,
    @SerialName("sale_status") val saleStatus: String? = null,
    val stats: JsonElement? = null,
    val lotes: JsonElement? = null,
    @SerialName("data_extra") val dataExtra: JsonElement? = null,
    val capacidade: Int? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
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

private fun JsonObject.stringValue(key: String): String {
    val value = this[key] ?: return ""
    if (value is JsonNull) return ""
    return value.jsonPrimitive.contentOrNull.orEmpty().trim()
}

private fun JsonObject.intValue(key: String): Int {
    val value = this[key] ?: return 0
    if (value is JsonNull) return 0
    return value.jsonPrimitive.intOrNull ?: value.jsonPrimitive.doubleOrNull?.toInt() ?: 0
}

private fun JsonObject.booleanValue(key: String): Boolean {
    val value = this[key] ?: return false
    if (value is JsonNull) return false
    return value.jsonPrimitive.booleanOrNull ?: false
}

private fun String.normalizeForMatch(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    return decomposed.trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
