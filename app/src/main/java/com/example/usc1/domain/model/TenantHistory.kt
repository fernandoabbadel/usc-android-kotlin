package com.example.usc1.domain.model

/**
 * `/historico` e `/historico/organograma`.
 * Espelha `historyService.ts` (`historic_events` + `app_config:historico`) e
 * `organogramService.ts` (`app_config:organograma`).
 */
object TenantHistoryCatalog {
    const val AppConfigTable = "app_config"
    const val HistoryConfigDocId = "historico"
    const val OrganogramConfigDocId = "organograma"
    const val HistoricEventsTable = "historic_events"
    const val MaxHistoryEvents = 200

    const val DefaultHistoryTitle = "Nossa Historia"
    const val DefaultHistorySubtitle = "Carregando legado..."
    const val DefaultOrganogramTitle = "Organograma da Atlética"
    const val DefaultOrganogramSubtitle =
        "Presidencia, vice-presidencia e diretorias em um painel vivo."

    fun normalizeSectionName(value: String?): String =
        value.orEmpty().trim().replace(Regex("\\s+"), " ").take(60).ifBlank { "Diretoria" }
}

data class HistoricEvent(
    val id: String,
    val title: String,
    val date: String = "",
    val year: String = "",
    val description: String = "",
    val location: String = "",
    val photoUrl: String? = null,
)

data class HistoryPageConfig(
    val title: String = TenantHistoryCatalog.DefaultHistoryTitle,
    val subtitle: String = TenantHistoryCatalog.DefaultHistorySubtitle,
    val coverPhotoUrl: String? = null,
)

data class TenantHistoryData(
    val config: HistoryPageConfig = HistoryPageConfig(),
    val events: List<HistoricEvent> = emptyList(),
)

enum class OrganogramMemberStatus(val remoteValue: String) {
    Pending("pending"),
    Approved("approved"),
    Rejected("rejected"),
    Unset("");

    companion object {
        fun fromRemote(value: String?): OrganogramMemberStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized && it != Unset } ?: Unset
        }
    }
}

data class OrganogramMember(
    val id: String,
    val section: String,
    val role: String,
    val order: Int = 0,
    val status: OrganogramMemberStatus = OrganogramMemberStatus.Unset,
    val userId: String = "",
    val name: String = "",
    val photoUrl: String? = null,
) {
    /** `isPublishedOrganogramMember`: pendente e recusado não aparecem na página. */
    val isPublished: Boolean
        get() = status != OrganogramMemberStatus.Pending && status != OrganogramMemberStatus.Rejected
}

data class OrganogramConfig(
    val title: String = TenantHistoryCatalog.DefaultOrganogramTitle,
    val subtitle: String = TenantHistoryCatalog.DefaultOrganogramSubtitle,
    val members: List<OrganogramMember> = emptyList(),
    val sectionOrder: List<String> = emptyList(),
)

/** Membro já resolvido com os dados canônicos de `users`. */
data class OrganogramDisplayMember(
    val member: OrganogramMember,
    val displayName: String,
    val displayPhotoUrl: String?,
    val displayDetail: String,
    val hasCanonicalVisual: Boolean,
)

data class OrganogramSection(
    val name: String,
    val members: List<OrganogramDisplayMember>,
)

/** `groupedMembers` da página: agrupa por seção respeitando `ordemSecoes`. */
fun buildOrganogramSections(
    config: OrganogramConfig,
    members: List<OrganogramDisplayMember>,
): List<OrganogramSection> {
    val grouped = LinkedHashMap<String, MutableList<OrganogramDisplayMember>>()
    members.forEach { entry ->
        val key = entry.member.section.ifBlank { "Diretoria" }
        grouped.getOrPut(key) { mutableListOf() }.add(entry)
    }

    val ordered = config.sectionOrder + grouped.keys.filterNot { it in config.sectionOrder }
    return ordered.distinct().mapNotNull { section ->
        val sectionMembers = grouped[section].orEmpty()
        if (sectionMembers.isEmpty()) {
            null
        } else {
            OrganogramSection(
                name = section,
                members = sectionMembers.sortedWith(
                    compareBy({ it.member.order }, { it.member.role.lowercase() }),
                ),
            )
        }
    }
}
