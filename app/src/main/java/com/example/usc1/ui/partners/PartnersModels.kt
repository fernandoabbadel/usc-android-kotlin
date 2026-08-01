package com.example.usc1.ui.partners

import com.example.usc1.domain.model.PartnerRecord

import com.example.usc1.domain.model.PartnerTier

data class PartnerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partners: List<PartnerRecord> = emptyList(),
    val search: String = "",
) {
    private val filtered: List<PartnerRecord>
        get() {
            val term = search.trim().lowercase()
            if (term.isBlank()) return partners
            return partners.filter { partner ->
                "${partner.name} ${partner.category} ${partner.description}"
                    .lowercase()
                    .contains(term)
            }
        }

    /** Mesmas seções do `/parceiros` do web: Ouro, Prata e Standard. */
    val sections: List<PartnerTierSection>
        get() {
            val rows = filtered
            return listOf(
                PartnerTierSection(
                    tier = PartnerTier.Ouro,
                    title = "Plano Ouro",
                    subtitle = "Maior destaque",
                    partners = rows.filter { it.tier == PartnerTier.Ouro },
                ),
                PartnerTierSection(
                    tier = PartnerTier.Prata,
                    title = "Plano Prata",
                    subtitle = "Destaque médio",
                    partners = rows.filter { it.tier == PartnerTier.Prata },
                ),
                PartnerTierSection(
                    tier = PartnerTier.Standard,
                    title = "Plano Standard",
                    subtitle = "Todos os parceiros",
                    partners = rows.filter { it.tier == PartnerTier.Standard },
                ),
            )
        }
}

data class PartnerTierSection(
    val tier: PartnerTier,
    val title: String,
    val subtitle: String,
    val partners: List<PartnerRecord>,
)

data class PartnerDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partner: PartnerRecord? = null,
)
