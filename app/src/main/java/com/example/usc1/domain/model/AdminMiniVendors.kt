package com.example.usc1.domain.model

data class AdminMiniVendor(
    val id: String,
    val tenantId: String,
    val userId: String,
    val status: AdminMiniVendorStatus,
    val storeName: String,
    val slug: String,
    val description: String,
    val logoUrl: String,
    val coverUrl: String,
    val pixKey: String,
    val pixBank: String,
    val pixHolder: String,
    val pixWhatsapp: String,
    val instagram: String,
    val instagramEnabled: Boolean,
    val whatsapp: String,
    val whatsappEnabled: Boolean,
    val profileVisible: Boolean,
    val categoryVisible: Boolean,
    val productsVisible: Boolean,
    val categoryButtonColor: String,
    val approvedBy: String,
    val approvedAt: String,
    val createdAt: String,
    val updatedAt: String,
)

enum class AdminMiniVendorStatus(val remoteValue: String, val label: String) {
    Pending("pending", "Pendente"),
    Approved("approved", "Aprovado"),
    Rejected("rejected", "Rejeitado"),
    Disabled("disabled", "Desativado");

    companion object {
        fun fromRemote(value: String?): AdminMiniVendorStatus {
            return when (value?.trim()?.lowercase()) {
                "approved" -> Approved
                "rejected" -> Rejected
                "disabled" -> Disabled
                else -> Pending
            }
        }
    }
}

enum class AdminMiniVendorDirectoryMode {
    Approvals,
    Vendors,
}
