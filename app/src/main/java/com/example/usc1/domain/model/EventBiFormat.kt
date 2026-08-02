package com.example.usc1.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Formatadores e estatística do BI de Eventos (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 192-470.
 * São as 26 funções que todo o bloco `analytics` e os 26 componentes de gráfico consomem.
 */

/** `COLORS` do web (linha 192) — paleta das séries de gráfico. */
val EventBiColors: List<String> = listOf(
    "#22c55e", "#38bdf8", "#facc15", "#fb7185", "#a78bfa", "#f97316", "#14b8a6", "#e879f9",
)

/** `WEEKDAYS` do web (linha 193), começando no domingo como `Date.getDay()`. */
val EventBiWeekdays: List<String> = listOf(
    "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado",
)

/** `PERIODS` do web (linha 194). */
val EventBiPeriods: List<String> = listOf("Madrugada", "Manhã", "Tarde", "Noite")

/** `ChartValueFormat` do web (linha 83). */
enum class EventBiValueFormat { Currency, Number, Decimal, Hours, Percent }

// ------------------------------------------------------------------
// Formatação (266-324)
// ------------------------------------------------------------------

private val PtBr = Locale.forLanguageTag("pt-BR")

/**
 * `formatCurrency` (276): `Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })`.
 *
 * O locale `pt-BR` já entrega vírgula decimal e ponto de milhar. Trocar os separadores depois
 * faria "1.234,56" voltar a ser "1,234.56".
 */
fun formatEventBiCurrency(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return "R$ " + String.format(PtBr, "%,.2f", safe)
}

/** `formatNumber` (280): `maximumFractionDigits: 0`. */
fun formatEventBiNumber(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return String.format(PtBr, "%,d", safe.roundToLong())
}

/** `formatDecimal` (284): sempre 1 casa (`minimumFractionDigits: 1`). */
fun formatEventBiDecimal(value: Double): String {
    val safe = if (value.isFinite()) value else 0.0
    return String.format(PtBr, "%,.1f", safe)
}

/** `formatPercent` (288). */
fun formatEventBiPercent(value: Double): String = "${formatEventBiDecimal(value)}%"

/** `formatHours` (292). */
fun formatEventBiHours(value: Double): String = "${formatEventBiDecimal(value)}h"

/** `formatChartValue` (296). */
fun formatEventBiValue(value: Double, format: EventBiValueFormat): String = when (format) {
    EventBiValueFormat.Currency -> formatEventBiCurrency(value)
    EventBiValueFormat.Percent -> formatEventBiPercent(value)
    EventBiValueFormat.Hours -> formatEventBiHours(value)
    EventBiValueFormat.Decimal -> formatEventBiDecimal(value)
    EventBiValueFormat.Number -> formatEventBiNumber(value)
}

/**
 * `formatShortChartValue` (304): acima de 1000 usa `notation: "compact"` com 1 casa.
 * O `pt-BR` compacto é "mil", "mi", "bi", "tri".
 */
fun formatEventBiShortValue(value: Double, format: EventBiValueFormat): String {
    val safe = if (value.isFinite()) value else 0.0
    return when (format) {
        EventBiValueFormat.Currency ->
            if (abs(safe) >= 1000) "R$ ${compactNotation(safe)}" else formatEventBiCurrency(safe)
        EventBiValueFormat.Percent -> formatEventBiPercent(safe)
        EventBiValueFormat.Hours -> formatEventBiHours(safe)
        EventBiValueFormat.Decimal -> formatEventBiDecimal(safe)
        EventBiValueFormat.Number ->
            if (abs(safe) >= 1000) compactNotation(safe) else formatEventBiNumber(safe)
    }
}

private fun compactNotation(value: Double): String {
    val units = listOf(1_000_000_000_000.0 to "tri", 1_000_000_000.0 to "bi", 1_000_000.0 to "mi", 1_000.0 to "mil")
    val unit = units.firstOrNull { abs(value) >= it.first } ?: return formatEventBiNumber(value)
    val scaled = value / unit.first
    // `maximumFractionDigits: 1`: casa decimal só quando não é inteira.
    val text = if (abs(scaled - scaled.roundToLong()) < 0.05) {
        scaled.roundToLong().toString()
    } else {
        String.format(PtBr, "%.1f", scaled).replace('.', ',')
    }
    return "$text ${unit.second}"
}

/**
 * `parseNumber` (326): tolera "R$ 1.234,56" — remove o que não é dígito, separador ou sinal,
 * derruba o ponto de milhar e troca a vírgula por ponto.
 */
fun parseEventBiNumber(value: String?, fallback: Double = 0.0): Double {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return fallback
    val normalized = raw
        .replace(Regex("[^\\d,.-]"), "")
        .replace(Regex("\\.(?=\\d{3}(?:\\D|$))"), "")
        .replace(",", ".")
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() } ?: fallback
}

/**
 * `parseDate` (339): ISO, epoch em texto ou o formato brasileiro `dd/MM/aaaa[ HH:mm[:ss]]`.
 * Devolve 0 quando não há data (o `null` do web).
 */
fun parseEventBiDate(value: String?): Long {
    val clean = value?.trim().orEmpty()
    if (clean.isBlank()) return 0L

    val brMatch = Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{4})(?:\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$")
        .find(clean)
    if (brMatch != null) {
        val (day, month, year) = brMatch.destructured.toList().take(3).map { it.toInt() }
        val hour = brMatch.groupValues.getOrNull(4)?.toIntOrNull() ?: 0
        val minute = brMatch.groupValues.getOrNull(5)?.toIntOrNull() ?: 0
        val second = brMatch.groupValues.getOrNull(6)?.toIntOrNull() ?: 0
        return runCatching {
            LocalDateTime.of(year, month, day, hour, minute, second)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)
    }

    runCatching { return OffsetDateTime.parse(clean).toInstant().toEpochMilli() }
    runCatching { return Instant.parse(clean).toEpochMilli() }
    runCatching {
        return LocalDateTime.parse(clean).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    runCatching {
        return LocalDate.parse(clean).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    return clean.toLongOrNull()?.takeIf { it > 0 } ?: 0L
}

/** `normalizeText` (368): sem acento, sem espaço nas pontas, minúsculo. */
fun normalizeEventBiText(value: String?): String =
    java.text.Normalizer.normalize(value?.trim().orEmpty(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

// ------------------------------------------------------------------
// Estatística (376-427)
// ------------------------------------------------------------------

/** `safeDivide` (376): divisor zero devolve 0, nunca `Infinity`. */
fun safeDivide(numerator: Double, denominator: Double): Double =
    if (denominator != 0.0) numerator / denominator else 0.0

fun safeDivide(numerator: Int, denominator: Int): Double =
    if (denominator != 0) numerator.toDouble() / denominator.toDouble() else 0.0

/** `median` (380): média dos dois centrais quando a lista tem tamanho par. */
fun eventBiMedian(values: List<Double>): Double {
    val sorted = values.filter { it.isFinite() }.sorted()
    if (sorted.isEmpty()) return 0.0
    val middle = sorted.size / 2
    return if (sorted.size % 2 != 0) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

/** `percentile` (387): interpolação linear, alvo preso em [0,1]. */
fun eventBiPercentile(values: List<Double>, target: Double): Double {
    val sorted = values.filter { it.isFinite() }.sorted()
    if (sorted.isEmpty()) return 0.0
    val safeTarget = min(1.0, max(0.0, target))
    val index = (sorted.size - 1) * safeTarget
    val lower = floor(index).toInt()
    val upper = ceil(index).toInt()
    if (lower == upper) return sorted[lower]
    return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower)
}

/** `maxValue` (398): o `reduce` do web parte de 0, então nunca devolve negativo. */
fun eventBiMaxValue(values: List<Double>): Double =
    values.filter { it.isFinite() }.fold(0.0) { current, value -> max(current, value) }

/** `clamp` (402): valor não finito devolve o mínimo. */
fun eventBiClamp(value: Double, minValue: Double = 0.0, maxValue: Double = 100.0): Double {
    if (!value.isFinite()) return minValue
    return min(maxValue, max(minValue, value))
}

/** `scoreFromRatio` (407). */
fun scoreFromRatio(value: Double, max: Double): Double = eventBiClamp(safeDivide(value, max) * 100)

/** `scoreFromInverseRate` (411): quanto menor a taxa, maior a nota. */
fun scoreFromInverseRate(value: Double, max: Double = 100.0): Double =
    eventBiClamp(100 - safeDivide(value, max) * 100)

/** `scoreColor` (415). */
fun scoreColor(score: Double): String = when {
    score >= 85 -> "#22c55e"
    score >= 70 -> "#38bdf8"
    score >= 40 -> "#facc15"
    else -> "#fb7185"
}

/** `scoreBandLabel` (422). */
fun scoreBandLabel(score: Double): String = when {
    score >= 85 -> "85-100 repetir e escalar"
    score >= 70 -> "70-84 repetir"
    score >= 40 -> "40-69 ajustar"
    else -> "0-39 repensar"
}

// ------------------------------------------------------------------
// Rótulos de eixo (429-470)
// ------------------------------------------------------------------

private fun millisToLocal(millis: Long): LocalDateTime? =
    millis.takeIf { it > 0L }
        ?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

/** `hourLabel` (429): `"08h"`, ou `"Sem horário"` quando não há data. */
fun eventBiHourLabel(millis: Long): String =
    millisToLocal(millis)?.let { "${it.hour.toString().padStart(2, '0')}h" } ?: "Sem horário"

/** `hourSortValue` (433): rótulo fora do padrão vai para o fim da ordenação. */
fun eventBiHourSortValue(label: String): Double {
    val match = Regex("^(\\d{1,2})h$", RegexOption.IGNORE_CASE).find(label.trim())
        ?: return Long.MAX_VALUE.toDouble()
    return match.groupValues[1].toDoubleOrNull() ?: Long.MAX_VALUE.toDouble()
}

/** `dateKey` (440): `"dd/MM"`, ou `"Sem data"`. */
fun eventBiDateKey(millis: Long): String = millisToLocal(millis)?.let {
    "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthValue.toString().padStart(2, '0')}"
} ?: "Sem data"

/** `WEEKDAYS[date.getDay()]` do web: domingo é 0. */
fun eventBiWeekdayLabel(millis: Long): String {
    val local = millisToLocal(millis) ?: return "Sem data"
    // `DayOfWeek` do Java começa na segunda (1); `getDay()` do JS começa no domingo (0).
    return EventBiWeekdays[local.dayOfWeek.value % 7]
}

/**
 * `periodFromDate` do web: madrugada 0-5, manhã 6-11, tarde 12-17, noite 18-23.
 */
fun eventBiPeriodLabel(millis: Long): String {
    val local = millisToLocal(millis) ?: return "Sem horário"
    return when (local.hour) {
        in 0..5 -> "Madrugada"
        in 6..11 -> "Manhã"
        in 12..17 -> "Tarde"
        else -> "Noite"
    }
}

/** `leadBucketLabel` (446): antecedência entre a compra e o início do evento. */
fun eventBiLeadBucketLabel(purchaseMillis: Long, eventStartMillis: Long): String {
    if (purchaseMillis <= 0L || eventStartMillis <= 0L) return "Sem data"
    val diffDays = (eventStartMillis - purchaseMillis) / 864e5
    return when {
        diffDays >= 30 -> "30 dias ou mais"
        diffDays >= 15 -> "15 a 29 dias"
        diffDays >= 7 -> "7 a 14 dias"
        diffDays >= 3 -> "3 a 6 dias"
        diffDays >= 1 -> "24 a 72h"
        else -> "Menos de 24h"
    }
}

/** `ticketBucket` (457): faixa de preço do ingresso. */
fun eventBiTicketBucket(value: Double): String = when {
    value < 25 -> "R$ 0-25"
    value < 50 -> "R$ 25-50"
    value < 100 -> "R$ 50-100"
    value < 150 -> "R$ 100-150"
    else -> "Mais de R$ 150"
}

/** `TICKET_BUCKET_ORDER` (465). */
val EventBiTicketBucketOrder: List<String> =
    listOf("R$ 0-25", "R$ 25-50", "R$ 50-100", "R$ 100-150", "Mais de R$ 150")

/** `ticketBucketSortValue` (467). */
fun eventBiTicketBucketSortValue(name: String): Double {
    val index = EventBiTicketBucketOrder.indexOf(name)
    return if (index >= 0) index.toDouble() else EventBiTicketBucketOrder.size.toDouble()
}

/** Arredondamento de percentual usado em vários indicadores do `analytics`. */
fun Double.asPercentOf(total: Double): Double = safeDivide(this, total) * 100

internal fun Double.round1(): Double = (this * 10).roundToInt() / 10.0
