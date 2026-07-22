package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminPartnersTierCounts
import com.example.usc1.domain.model.PartnerForm
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerScanRecord
import com.example.usc1.domain.model.PartnerStatus

enum class AdminPartnersMode {
    Active,
    Companies,
    Bi,
    History,
}

enum class AdminPartnersBiMetric(val label: String) {
    Quantity("Quantidade"),
    Value("Valor R$"),
}

data class AdminPartnersUiState(
    val mode: AdminPartnersMode = AdminPartnersMode.Companies,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val tenantId: String = "",
    val counts: AdminPartnersTierCounts = AdminPartnersTierCounts(),
    val partners: List<PartnerRecord> = emptyList(),
    val scans: List<PartnerScanRecord> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val statusFilter: PartnerStatus? = null,
    val search: String = "",
    val biMetric: AdminPartnersBiMetric = AdminPartnersBiMetric.Quantity,
    val usersPage: Int = 1,
    val mutatingId: String = "",
    val form: PartnerForm? = null,
) {
    val filteredPartners: List<PartnerRecord>
        get() {
            val term = search.trim().lowercase()
            if (term.isBlank()) return partners
            return partners.filter { partner ->
                "${partner.name} ${partner.category} ${partner.email} ${partner.responsible}"
                    .lowercase()
                    .contains(term)
            }
        }

    val title: String
        get() = when (mode) {
            AdminPartnersMode.Active -> "Parceiros Ativos"
            AdminPartnersMode.Companies -> "Empresas Parceiras"
            AdminPartnersMode.Bi -> "BI de parceiros"
            AdminPartnersMode.History -> "Histórico de scans"
        }

    val subtitle: String
        get() = when (mode) {
            AdminPartnersMode.Active -> "Contagem consolidada + lista paginada"
            AdminPartnersMode.Companies -> "Lista paginada (20 por leitura). Não carrega tudo de uma vez."
            AdminPartnersMode.Bi -> "Scans, cupons, tipos de QR Code e uso por usuário."
            AdminPartnersMode.History -> "Tabela administrativa completa das leituras de cupons de parceiros."
        }
}
