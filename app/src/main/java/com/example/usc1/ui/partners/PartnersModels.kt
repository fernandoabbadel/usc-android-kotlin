package com.example.usc1.ui.partners

import com.example.usc1.domain.model.PartnerRecord

data class PartnerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partners: List<PartnerRecord> = emptyList(),
)

data class PartnerDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partner: PartnerRecord? = null,
)
