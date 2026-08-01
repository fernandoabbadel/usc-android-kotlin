package com.example.usc1.ui.collectives

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.util.Locale

/** Porte de `parseEventDateTimeMs` de `web-reference/src/lib/eventDateUtils.ts`. */
object CollectiveDateUtils {
    private val monthsPtBr = mapOf(
        "JAN" to 1,
        "FEV" to 2,
        "MAR" to 3,
        "ABR" to 4,
        "MAI" to 5,
        "JUN" to 6,
        "JUL" to 7,
        "AGO" to 8,
        "SET" to 9,
        "OUT" to 10,
        "NOV" to 11,
        "DEZ" to 12,
    )

    private val isoDate = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val brDate = Regex("""^\d{2}/\d{2}/\d{4}$""")

    private data class Clock(val hours: Int, val minutes: Int)

    private fun parseClock(raw: String?): Clock {
        val parts = raw?.trim().orEmpty().split(":")
        val hours = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return Clock(hours.coerceIn(0, 23), minutes.coerceIn(0, 59))
    }

    fun parseEventDateTimeMs(dateRaw: String?, timeRaw: String?): Long? {
        val date = dateRaw?.trim().orEmpty()
        if (date.isBlank()) return null

        val clock = parseClock(timeRaw)

        val localDate = when {
            isoDate.matches(date) -> runCatching {
                val parts = date.split("-").map(String::toInt)
                LocalDate.of(parts[0], parts[1], parts[2])
            }.getOrNull()

            brDate.matches(date) -> runCatching {
                val parts = date.split("/").map(String::toInt)
                LocalDate.of(parts[2], parts[1], parts[0])
            }.getOrNull()

            else -> parseTextualDate(date)
        } ?: return null

        return runCatching {
            LocalDateTime.of(localDate, java.time.LocalTime.of(clock.hours, clock.minutes))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private fun parseTextualDate(raw: String): LocalDate? {
        val normalized = CollectiveTextUtils
            .stripAccents(raw.uppercase(Locale.ROOT))
            .replace(Regex("[.,-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val parts = normalized.split(" ").filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val day = parts[0].toIntOrNull() ?: return null
        val month = monthsPtBr[parts[1].take(3)] ?: return null
        val year = parts.getOrNull(2)?.takeIf { it.length == 4 }?.toIntOrNull() ?: Year.now().value

        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    /** `getEventBadge` do web: dia e mês curto em pt-BR para o cartão da agenda. */
    fun eventBadge(dateRaw: String?): Pair<String, String> {
        val raw = dateRaw?.trim().orEmpty()
        val parsedMs = parseEventDateTimeMs(raw, "00:00")
            ?: return Pair(
                raw.take(2).ifBlank { "--" },
                raw.drop(3).take(5).ifBlank { "DATA" },
            )

        val date = java.time.Instant.ofEpochMilli(parsedMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val ptBr = Locale.forLanguageTag("pt-BR")
        val month = date.month
            .getDisplayName(java.time.format.TextStyle.SHORT, ptBr)
            .replace(".", "")
            .uppercase(ptBr)
        return Pair(String.format(ptBr, "%02d", date.dayOfMonth), month)
    }
}
