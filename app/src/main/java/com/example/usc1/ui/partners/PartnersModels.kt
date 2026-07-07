package com.example.usc1.ui.partners

import com.example.usc1.R

enum class PartnerStatus(val label: String) {
    Active("Ativo"),
    Featured("Destaque"),
    Paused("Pausado"),
}

data class PartnerBenefit(
    val title: String,
    val rule: String,
    val valueLabel: String,
)

data class PartnerHistoryEntry(
    val id: String,
    val benefitTitle: String,
    val usedAtLabel: String,
    val valueLabel: String,
)

data class PartnerCompany(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val addressLabel: String,
    val status: PartnerStatus,
    val imageRes: Int,
    val benefits: List<PartnerBenefit>,
    val history: List<PartnerHistoryEntry>,
)

data class PartnerUiState(
    val partners: List<PartnerCompany> = PartnersMockData.partners,
)

object PartnersMockData {
    private val defaultHistory = listOf(
        PartnerHistoryEntry(
            id = "BEN-204",
            benefitTitle = "Desconto aplicado",
            usedAtLabel = "Hoje • 12:14",
            valueLabel = "R$ 12,00",
        ),
        PartnerHistoryEntry(
            id = "BEN-188",
            benefitTitle = "Cupom USC",
            usedAtLabel = "30 JUN • 19:05",
            valueLabel = "15%",
        ),
    )

    val partners = listOf(
        PartnerCompany(
            id = "burguer-cardume",
            name = "Burguer Cardume",
            category = "Alimentação",
            description = "Hamburgueria parceira com combo exclusivo para membros ativos da USC.",
            addressLabel = "Av. Central, 420",
            status = PartnerStatus.Featured,
            imageRes = R.drawable.battle_forest,
            benefits = listOf(
                PartnerBenefit("Combo Tubarão", "Carteirinha ativa no balcão", "15% OFF"),
                PartnerBenefit("Retirada em evento", "Pedido aprovado na loja USC", "Frete zero"),
            ),
            history = defaultHistory,
        ),
        PartnerCompany(
            id = "gym-us",
            name = "US Gym",
            category = "Academia",
            description = "Academia parceira para check-in, frequência e benefícios do plano Atleta.",
            addressLabel = "Rua das Atléticas, 88",
            status = PartnerStatus.Active,
            imageRes = R.drawable.logo_usc_wide,
            benefits = listOf(
                PartnerBenefit("Diária avulsa", "Plano Atleta ou Lenda", "R$ 19,90"),
                PartnerBenefit("Avaliação física", "Primeiro check-in validado", "Grátis"),
            ),
            history = defaultHistory.take(1),
        ),
        PartnerCompany(
            id = "print-aaakn",
            name = "Print AAAKN",
            category = "Serviços",
            description = "Impressão, banners e materiais oficiais para ligas, eventos e comissões.",
            addressLabel = "Campus USC • Bloco B",
            status = PartnerStatus.Paused,
            imageRes = R.drawable.logo_aaakn,
            benefits = listOf(
                PartnerBenefit("Banners de evento", "Solicitação pela comissão", "10% OFF"),
                PartnerBenefit("Kit calouro", "Turmas aprovadas", "Preço fechado"),
            ),
            history = emptyList(),
        ),
    )

    fun partnerById(id: String): PartnerCompany =
        partners.firstOrNull { it.id == id } ?: partners.first()
}
