package com.example.usc1.ui.collectives

import java.text.Normalizer
import java.util.Locale

/** Helpers de texto portados de `web-reference/src/app/ligas_usc/page.tsx`. */
object CollectiveTextUtils {
    /** `LEAGUE_NAME_MAX_LENGTH` do web. */
    const val LeagueNameMaxLength = 42

    fun stripAccents(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** `normalizeLeagueText` do web. */
    fun normalize(value: String): String = stripAccents(value).lowercase(Locale.ROOT).trim()

    /** `splitLeagueTokens` do web. */
    fun splitTokens(value: String): List<String> =
        normalize(value)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }

    /** `clampLeagueCardName` do web. */
    fun clampCardName(value: String): String {
        val cleanValue = value.trim()
        if (cleanValue.length <= LeagueNameMaxLength) return cleanValue

        val sliced = cleanValue.take(LeagueNameMaxLength + 1)
        val lastSpaceIndex = sliced.lastIndexOf(' ')
        val cutIndex = if (lastSpaceIndex >= (LeagueNameMaxLength * 0.6).toInt()) {
            lastSpaceIndex
        } else {
            LeagueNameMaxLength
        }

        return "${sliced.take(cutIndex).trim()}..."
    }

    /** `normalizeTurmaCode` do web. */
    fun normalizeTurmaCode(value: String?): String = value?.trim()?.uppercase(Locale.ROOT).orEmpty()
}
