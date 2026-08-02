package com.example.usc1.domain.model

import java.text.Collator
import java.util.Locale

/**
 * Tipos de linha do BI de Eventos (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 67-149
 * (`MetricRow`, `TableRow`, `HeatmapEntry`, `BubbleEntry`, `NetworkEdge`, `OperationalRecord`,
 * `ProductMetricRow`, `StatementLinkOptions`, `CheckinsLinkOptions`) e 2068-2090
 * (`addMetric`/`metricRows`).
 */

/** Comparação `localeCompare(..., "pt-BR")` do web. */
private val PtBrCollator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

internal fun comparePtBr(left: String, right: String): Int = PtBrCollator.compare(left, right)

/** `MetricRow` (67). */
data class EventBiMetricRow(
    val name: String,
    val quantity: Double = 0.0,
    val value: Double = 0.0,
    val average: Double = 0.0,
    val secondary: Double = 0.0,
    val sortValue: Double = 0.0,
    val hint: String = "",
    val href: String = "",
)

/** `ProductMetricRow` (177). */
data class EventBiProductMetricRow(
    val name: String,
    val quantity: Double = 0.0,
    val value: Double = 0.0,
    val average: Double = 0.0,
    val redeemed: Double = 0.0,
    val pending: Double = 0.0,
)

/**
 * `TableRow = Record<string, string | number | undefined>` (78).
 *
 * O web entrega essas linhas ao `DataTable` genérico, que lê as chaves pela ordem de inserção;
 * por isso o port também é um mapa ordenado em vez de uma classe por tabela.
 */
data class EventBiTableRow(val cells: Map<String, Any?>) {
    operator fun get(key: String): Any? = cells[key]

    fun text(key: String): String = when (val cell = cells[key]) {
        null -> ""
        is String -> cell
        is Double -> formatEventBiDecimal(cell)
        else -> cell.toString()
    }

    fun number(key: String): Double = when (val cell = cells[key]) {
        is Number -> cell.toDouble()
        is String -> parseEventBiNumber(cell)
        else -> 0.0
    }

    val href: String get() = text("href")
}

internal fun tableRowOf(vararg pairs: Pair<String, Any?>): EventBiTableRow =
    EventBiTableRow(linkedMapOf(*pairs))

/** `HeatmapEntry` (84). */
data class EventBiHeatmapEntry(
    val row: String,
    val column: String,
    val value: Double,
    val href: String = "",
)

/** `BubbleEntry` (91). */
data class EventBiBubbleEntry(
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val value: Double,
    val decision: String = "",
    val href: String = "",
)

/** `NetworkEdge` (101). */
data class EventBiNetworkEdge(
    val from: String,
    val to: String,
    val value: Double,
    val href: String = "",
)

// ------------------------------------------------------------------
// Links do workspace de evento (M10)
// ------------------------------------------------------------------

/** `StatementKind` (79). */
enum class EventBiRecordKind(val remoteValue: String) { Ticket("ingresso"), Product("produto") }

/** `StatementStatusFilter` (81). */
enum class EventBiStatementStatus(val remoteValue: String) {
    All("todos"), Approved("aprovado"), Pending("pendente"), Review("analise")
}

/** `OperationalFlow` (82). */
enum class EventBiFlow(val remoteValue: String) {
    Order("pedido"), Approval("aprovacao"), CheckIn("checkin"), Withdrawal("retirada")
}

/** `StatementLinkOptions` (108). */
data class EventBiStatementLink(
    val type: EventBiRecordKind? = null,
    val status: EventBiStatementStatus? = null,
    val search: String = "",
    val alert: String = "",
    val source: String = "",
    val approver: String = "",
    val flow: EventBiFlow? = null,
    val indicator: String = "",
)

/** `CheckinsLinkOptions` (119). */
data class EventBiCheckinsLink(val search: String = "", val indicator: String = "")

/**
 * `buildStatementHref` (3656) e `buildCheckinsHref` (3676).
 *
 * As duas rotas de destino (`/extrato` e `/checkins` do workspace de evento) são o M10. Enquanto
 * ele não existe, o app usa `Inert`, que devolve string vazia — o indicador continua sendo
 * calculado e o link fica desligado, em vez de navegar para uma tela inexistente.
 */
interface EventBiLinkBuilder {
    fun statement(eventId: String, options: EventBiStatementLink = EventBiStatementLink()): String
    fun checkins(eventId: String, options: EventBiCheckinsLink = EventBiCheckinsLink()): String

    object Inert : EventBiLinkBuilder {
        override fun statement(eventId: String, options: EventBiStatementLink) = ""
        override fun checkins(eventId: String, options: EventBiCheckinsLink) = ""
    }
}

// ------------------------------------------------------------------
// `addMetric` / `metricRows` (2068-2090)
// ------------------------------------------------------------------

/** Acumulador mutável equivalente ao `Map<string, MetricRow>` do web. */
class EventBiMetricBucket {
    private val rows = LinkedHashMap<String, MutableRow>()

    private class MutableRow(
        val name: String,
        var quantity: Double = 0.0,
        var value: Double = 0.0,
        var secondary: Double = 0.0,
        var average: Double = 0.0,
        var sortValue: Double = 0.0,
        var hint: String = "",
        var href: String = "",
    )

    /** `addMetric` (2068): nome vazio vira "Sem dado" e a média é recalculada a cada soma. */
    fun add(
        name: String,
        quantity: Double,
        value: Double,
        secondary: Double = 0.0,
        href: String = "",
    ) {
        val cleanName = name.trim().ifBlank { "Sem dado" }
        val current = rows.getOrPut(cleanName) { MutableRow(cleanName) }
        current.quantity += quantity
        current.value += value
        current.secondary += secondary
        current.average = safeDivide(current.value, current.quantity)
        if (current.href.isBlank() && href.isNotBlank()) current.href = href
    }

    /** Pré-carrega um balde com zero, como `leadBuckets`/`timingBuckets` fazem no web. */
    fun seed(name: String, sortValue: Double = 0.0) {
        rows.getOrPut(name) { MutableRow(name, sortValue = sortValue) }
    }

    fun addToSeeded(name: String, quantity: Double, value: Double, href: String = "") {
        val current = rows.getOrPut(name) { MutableRow(name) }
        current.quantity += quantity
        current.value += value
        if (current.href.isBlank() && href.isNotBlank()) current.href = href
    }

    operator fun contains(name: String): Boolean = rows.containsKey(name)

    fun valueOf(name: String): EventBiMetricRow? = rows[name]?.snapshot()

    /** Ordem de inserção, sem ordenar nem cortar — o `Array.from(map.values())` do web. */
    fun all(): List<EventBiMetricRow> = rows.values.map { it.snapshot() }

    val totalQuantity: Double get() = rows.values.sumOf { it.quantity }

    val size: Int get() = rows.size

    /**
     * `metricRows` (2086): ordena por valor, depois quantidade, depois nome em pt-BR, e corta.
     */
    fun rows(limit: Int = 12): List<EventBiMetricRow> = rows.values
        .map { it.snapshot() }
        .sortedWith(
            compareByDescending<EventBiMetricRow> { it.value }
                .thenByDescending { it.quantity }
                .thenComparator { left, right -> comparePtBr(left.name, right.name) },
        )
        .take(limit)

    private fun MutableRow.snapshot() = EventBiMetricRow(
        name = name,
        quantity = quantity,
        value = value,
        average = average,
        secondary = secondary,
        sortValue = sortValue,
        hint = hint,
        href = href,
    )
}

/**
 * `CountRate` (4725) + `addCountRate`/`countRateRows` (4753-4773): aprovado x presente por chave.
 */
class EventBiCountRateBucket {
    private class Rate(var approved: Double = 0.0, var present: Double = 0.0, var value: Double = 0.0, var href: String = "")

    private val rows = LinkedHashMap<String, Rate>()

    fun add(name: String, approved: Double, present: Double, value: Double, href: String) {
        val cleanName = name.trim().ifBlank { "Sem dado" }
        val current = rows.getOrPut(cleanName) { Rate(href = href) }
        current.approved += approved
        current.present += present
        current.value += value
        if (current.href.isBlank() && href.isNotBlank()) current.href = href
    }

    /** `countRateRows`: `value` é a taxa de presença, `secondary` é o ausente. */
    fun rows(limit: Int = 12): List<EventBiMetricRow> = rows.entries
        .map { (name, row) ->
            EventBiMetricRow(
                name = name,
                quantity = row.present,
                value = safeDivide(row.present, row.approved) * 100,
                secondary = maxOf(0.0, row.approved - row.present),
                average = safeDivide(row.value, row.present),
                href = row.href,
            )
        }
        .sortedWith(
            compareByDescending<EventBiMetricRow> { it.value }
                .thenByDescending { it.quantity }
                .thenComparator { left, right -> comparePtBr(left.name, right.name) },
        )
        .take(limit)
}
