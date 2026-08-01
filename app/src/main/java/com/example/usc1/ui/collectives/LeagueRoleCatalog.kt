package com.example.usc1.ui.collectives

import java.text.Normalizer
import java.util.Locale

/** Porte de `web-reference/src/lib/leagueRoles.ts`. */
object LeagueRoleCatalog {
    val roleOptions = listOf(
        "Presidente",
        "Vice-Presidente",
        "Secretaria",
        "Tesouraria",
        "Diretoria",
        "Membro",
    )

    val managementRoleOptions = listOf(
        "Presidente",
        "Vice-Presidente",
        "Secretaria",
        "Tesouraria",
        "Diretoria",
    )

    const val DefaultRole = "Membro"

    private val roleImportance = mapOf(
        "presidente" to 0,
        "vice-presidente" to 1,
        "secretaria" to 2,
        "tesouraria" to 3,
        "diretoria" to 4,
        "membro" to 5,
    )

    private fun normalizeRoleText(value: String): String {
        val stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return stripped.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    fun resolveRoleLabel(value: String?): String {
        val cleanValue = value?.trim().orEmpty()
        val normalized = normalizeRoleText(cleanValue)

        if (normalized.isBlank()) return DefaultRole
        if (normalized.startsWith("president")) return "Presidente"
        if (normalized.startsWith("vice president") || normalized == "vice") return "Vice-Presidente"
        if (
            normalized.startsWith("secretar") ||
            normalized.startsWith("secratar") ||
            normalized.startsWith("secretari")
        ) {
            return "Secretaria"
        }
        if (normalized.startsWith("tesour")) return "Tesouraria"
        if (normalized.startsWith("diretor")) return "Diretoria"
        if (normalized.startsWith("membro")) return "Membro"

        return cleanValue.ifBlank { DefaultRole }
    }

    fun roleImportance(value: String?): Int {
        val label = resolveRoleLabel(value)
        val normalized = normalizeRoleText(label).replace(Regex("\\s+"), "-")
        return roleImportance[normalized] ?: Int.MAX_VALUE
    }

    fun canManageRole(value: String?): Boolean = managementRoleOptions.contains(resolveRoleLabel(value))

    fun sortMembersByRole(members: List<CollectiveMember>): List<CollectiveMember> =
        members.sortedWith(
            compareBy<CollectiveMember> { roleImportance(it.role) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { resolveRoleLabel(it.role) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )
}
