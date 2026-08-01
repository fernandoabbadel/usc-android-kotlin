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

/** `PartnerLoginResult` de `partnersService.ts`, usado por `/empresa`. */
data class PartnerLoginResult(
    val id: String,
    val name: String,
    val status: PartnerStatus,
    val passwordValid: Boolean,
    val hasPasswordResetCode: Boolean = false,
)

/** `contact_visibility_ack`: aceite de exibição pública de cada contato. */
data class PartnerContactVisibility(
    val whatsApp: Boolean = false,
    val instagram: Boolean = false,
    val site: Boolean = false,
)

/** Payload de `createPartnerLead`, do passo 2 de `/empresa/cadastro`. */
data class PartnerLeadForm(
    val name: String = "",
    val cnpj: String = "",
    val responsible: String = "",
    val cpf: String = "",
    val category: String = "Alimentação",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val description: String = "",
    val address: String = "",
    val businessHours: String = "",
    val tier: PartnerTier = PartnerTier.Standard,
)

object PartnerRegistrationRules {
    val Categories = listOf(
        "Alimentação",
        "Vestuário",
        "Saúde",
        "Educação",
        "Serviços",
        "Lazer",
        "Outros",
    )

    fun keepDigits(value: String, maxDigits: Int): String =
        value.filter(Char::isDigit).take(maxDigits)

    fun formatCnpj(value: String): String {
        val digits = keepDigits(value, 14)
        return when {
            digits.isEmpty() -> ""
            digits.length <= 2 -> digits
            digits.length <= 5 -> "${digits.take(2)}.${digits.drop(2)}"
            digits.length <= 8 -> "${digits.take(2)}.${digits.substring(2, 5)}.${digits.drop(5)}"
            digits.length <= 12 ->
                "${digits.take(2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.drop(8)}"
            else ->
                "${digits.take(2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/" +
                    "${digits.substring(8, 12)}-${digits.drop(12)}"
        }
    }

    fun formatCpf(value: String): String {
        val digits = keepDigits(value, 11)
        return when {
            digits.isEmpty() -> ""
            digits.length <= 3 -> digits
            digits.length <= 6 -> "${digits.take(3)}.${digits.drop(3)}"
            digits.length <= 9 -> "${digits.take(3)}.${digits.substring(3, 6)}.${digits.drop(6)}"
            else ->
                "${digits.take(3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.drop(9)}"
        }
    }

    fun formatPhone(value: String): String {
        val digits = keepDigits(value, 13)
        if (digits.isEmpty()) return ""
        if (digits.length <= 2) return digits
        val country = digits.take(2)
        if (digits.length <= 4) return "$country (${digits.drop(2)}"
        val areaCode = digits.substring(2, 4)
        val number = digits.drop(4)
        return when {
            number.isEmpty() -> "$country ($areaCode)"
            number.length <= 4 -> "$country ($areaCode) $number"
            number.length <= 8 -> "$country ($areaCode) ${number.take(4)}-${number.drop(4)}"
            else -> "$country ($areaCode) ${number.take(5)}-${number.substring(5, minOf(9, number.length))}"
        }
    }

    /** `validateStep2` de `/empresa/cadastro`. */
    fun validate(form: PartnerLeadForm): String? {
        if (form.name.isBlank()) return "Nome fantasia é obrigatório."
        if (keepDigits(form.cnpj, 14).length != 14) return "CNPJ inválido (14 dígitos)."
        if (form.responsible.isBlank()) return "Nome do responsável é obrigatório."
        if (keepDigits(form.cpf, 11).length != 11) return "CPF inválido (11 dígitos)."

        val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        if (!emailRegex.matches(form.email.trim()) || !form.email.contains(".com")) {
            return "Email inválido."
        }

        val phoneDigits = keepDigits(form.phone, 13)
        if (phoneDigits.length !in 12..13) {
            return "Telefone inválido (use 55 + DDD + número)."
        }

        if (form.password.length < 8) return "A senha deve ter no mínimo 8 caracteres."
        if (form.password != form.passwordConfirmation) return "As senhas não conferem."
        return null
    }
}

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
