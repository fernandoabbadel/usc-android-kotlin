package com.example.usc1.ui.membershipCard

import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.data.repository.normalizeMembershipClassCode
import com.example.usc1.domain.model.MembershipCardConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class MembershipCardUiState(
    val isConfigLoading: Boolean = true,
    val errorMessage: String? = null,
    val config: MembershipCardConfig = MembershipCardConfig(),
    val card: MembershipCardUiModel = MembershipCardUiModel(),
)

data class MembershipCardUiModel(
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String? = null,
    val course: String = "",
    val classCode: String = "",
    val registrationNumber: String = "",
    val tenantId: String = "",
    val tenantName: String = "",
    val tenantAcronym: String = "USC",
    val tenantCourse: String = "Atlética",
    val tenantLogoUrl: String? = null,
    val tenantPalette: TenantPalette = TenantPalette.Green,
    val planName: String = "Visitante",
    val planColorKey: MembershipPlanColorKey = MembershipPlanColorKey.Zinc,
    val planIconKey: String = "ghost",
    val validity: String = MembershipCardConfig.DefaultValidity,
    val backgroundUrl: String? = null,
    val backgroundOpacity: Int = MembershipCardConfig.DefaultBackgroundOpacity,
    val qrPayload: String = "",
) {
    val canUpgrade: Boolean
        get() = planColorKey == MembershipPlanColorKey.Zinc ||
            planColorKey == MembershipPlanColorKey.Emerald
}

enum class MembershipPlanColorKey {
    Yellow,
    Emerald,
    Purple,
    Blue,
    Red,
    Orange,
    Zinc,
}

fun MembershipCardUiState.withSession(session: UserSession): MembershipCardUiState {
    val user = session.user ?: return copy(card = MembershipCardUiModel())
    val tenant = session.tenant
    val normalizedClassCode = normalizeMembershipClassCode(user.classCode)
    val planName = user.planName.trim()
        .ifBlank { user.planBadge.trim() }
        .ifBlank { "Visitante" }
    val tenantAcronym = tenant?.acronym.orEmpty()
        .trim()
        .ifBlank { "USC" }
        .uppercase()
    val tenantCourse = tenant?.course.orEmpty()
        .trim()
        .ifBlank { "Atlética" }

    return copy(
        card = MembershipCardUiModel(
            userId = user.id.trim(),
            userName = user.name.trim(),
            avatarUrl = user.avatarUrl?.trim()?.ifBlank { null },
            course = tenantCourse,
            classCode = user.classCode.trim(),
            registrationNumber = user.registrationNumber.trim(),
            tenantId = tenant?.id.orEmpty().trim(),
            tenantName = tenant?.name.orEmpty().trim(),
            tenantAcronym = tenantAcronym,
            tenantCourse = tenantCourse,
            tenantLogoUrl = tenant?.logoUrl?.trim()?.ifBlank { null },
            tenantPalette = tenant?.palette ?: TenantPalette.Green,
            planName = planName,
            planColorKey = resolveMembershipPlanColorKey(user.planColorKey),
            planIconKey = user.planIconKey.trim().ifBlank { "ghost" },
            validity = config.validity,
            backgroundUrl = config.backgroundUrls[normalizedClassCode],
            backgroundOpacity = config.backgroundOpacity,
            qrPayload = buildMembershipIdentityQrPayload(
                userId = user.id,
                tenantId = tenant?.id,
                userName = user.name,
                userClass = user.classCode,
                userAvatar = user.avatarUrl,
            ),
        ),
    )
}

fun resolveMembershipPlanColorKey(rawValue: String?): MembershipPlanColorKey {
    val value = rawValue.orEmpty().trim().lowercase()
    return when {
        value == "yellow" || value.contains("amber") || value.contains("gold") ->
            MembershipPlanColorKey.Yellow
        value == "emerald" || value.contains("green") -> MembershipPlanColorKey.Emerald
        value == "purple" || value.contains("violet") -> MembershipPlanColorKey.Purple
        value == "blue" || value.contains("sky") || value.contains("cyan") ->
            MembershipPlanColorKey.Blue
        value == "red" || value.contains("rose") -> MembershipPlanColorKey.Red
        value == "orange" || value.contains("orange") -> MembershipPlanColorKey.Orange
        else -> MembershipPlanColorKey.Zinc
    }
}

fun buildMembershipIdentityQrPayload(
    userId: String,
    tenantId: String?,
    userName: String?,
    userClass: String?,
    userAvatar: String?,
): String {
    return buildJsonObject {
        put("t", "usuario")
        put("v", 1)
        put("uid", userId)
        put("ten", tenantId.orEmpty())
        put("n", userName.orEmpty())
        put("tu", userClass.orEmpty())
        put("av", userAvatar.orEmpty())
    }.toString()
}
