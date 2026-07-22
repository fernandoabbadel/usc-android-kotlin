package com.example.usc1.domain.model

enum class PartnerTier(val remoteValue: String, val label: String, val rank: Int) {
    Ouro("ouro", "Ouro", 0),
    Prata("prata", "Prata", 1),
    Standard("standard", "Standard", 2);

    companion object {
        fun fromRemote(value: String?): PartnerTier {
            return when (value?.trim()?.lowercase()) {
                "ouro" -> Ouro
                "prata" -> Prata
                else -> Standard
            }
        }
    }
}

enum class PartnerStatus(val remoteValue: String, val label: String) {
    Active("active", "Ativo"),
    Pending("pending", "Pendente"),
    Disabled("disabled", "Desativado");

    companion object {
        fun fromRemote(value: String?): PartnerStatus {
            return when (value?.trim()?.lowercase()) {
                "pending" -> Pending
                "disabled" -> Disabled
                else -> Active
            }
        }
    }
}

data class PartnerCoupon(
    val id: String,
    val title: String,
    val rule: String,
    val valueLabel: String,
    val imageUrl: String = "",
    val type: String = "",
    val active: Boolean = true,
    val qrCode: String = "",
)

data class PartnerRecord(
    val id: String,
    val tenantId: String,
    val name: String,
    val category: String,
    val tier: PartnerTier,
    val status: PartnerStatus,
    val cnpj: String,
    val responsible: String,
    val email: String,
    val phone: String,
    val description: String,
    val address: String,
    val businessHours: String,
    val instagram: String,
    val site: String,
    val whatsApp: String,
    val coverUrl: String,
    val logoUrl: String,
    val monthlyFee: Double,
    val salesTotal: Double,
    val totalScans: Int,
    val coupons: List<PartnerCoupon>,
    val createdAt: String = "",
) {
    val publicStatusLabel: String
        get() = when {
            tier == PartnerTier.Ouro && status == PartnerStatus.Active -> "Destaque"
            status == PartnerStatus.Active -> "Ativo"
            else -> status.label
        }
}

data class PartnerScanRecord(
    val id: String,
    val tenantId: String,
    val companyId: String,
    val companyName: String,
    val userName: String,
    val userId: String,
    val couponName: String,
    val savedValueLabel: String,
    val date: String,
    val hour: String,
    val couponId: String,
    val couponTitle: String,
    val scanMethod: String,
    val approvalMode: String,
    val qrCode: String,
    val couponType: String,
    val couponValue: String,
    val couponValueNumeric: Double,
    val status: String,
    val approvedByPartnerId: String,
    val userDisplayName: String,
    val timestamp: String,
)

data class AdminPartnersTierCounts(
    val total: Int = 0,
    val active: Int = 0,
    val pending: Int = 0,
    val disabled: Int = 0,
    val gold: Int = 0,
    val silver: Int = 0,
    val standard: Int = 0,
)

data class AdminPartnersPage(
    val tenantId: String,
    val partners: List<PartnerRecord>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
    val statusFilter: PartnerStatus?,
)

data class AdminPartnerScansPage(
    val tenantId: String,
    val scans: List<PartnerScanRecord>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)

data class AdminPartnersBundle(
    val tenantId: String,
    val partners: List<PartnerRecord>,
    val scans: List<PartnerScanRecord>,
)

data class PartnerForm(
    val partnerId: String = "",
    val name: String = "",
    val category: String = "",
    val tier: PartnerTier = PartnerTier.Standard,
    val status: PartnerStatus = PartnerStatus.Active,
    val cnpj: String = "",
    val responsible: String = "",
    val email: String = "",
    val phone: String = "",
    val description: String = "",
    val address: String = "",
    val businessHours: String = "",
    val instagram: String = "",
    val whatsApp: String = "",
    val site: String = "",
    val logoUrl: String = "",
    val coverUrl: String = "",
)

data class PartnerPasswordReset(
    val code: String,
    val expiresAt: String,
)

object PartnersCatalog {
    const val PageSize = 20
    const val BiPartnersLimit = 600
    const val BiScansLimit = 1_200
    const val MaxPublicPartners = 240
    const val MaxNameLength = 120
    const val MaxCategoryLength = 80
    const val MaxDescriptionLength = 2_000
    const val MaxContactLength = 180
    const val MaxUrlLength = 600
}
