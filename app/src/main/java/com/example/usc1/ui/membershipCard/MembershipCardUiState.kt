package com.example.usc1.ui.membershipCard

data class MembershipCardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val card: MembershipCardUiModel = MembershipCardUiModel(),
)

data class MembershipCardUiModel(
    val userName: String = "Fernando USC",
    val course: String = "Medicina",
    val className: String = "T9",
    val tenantName: String = "AAAKN USC",
    val planName: String = "Cardume Livre",
    val memberStatus: String = "Membro ativo",
    val validUntil: String = "31/12/2026",
    val memberCode: String = "USC-2026-042",
    val qrLabel: String = "QR mockado",
)
