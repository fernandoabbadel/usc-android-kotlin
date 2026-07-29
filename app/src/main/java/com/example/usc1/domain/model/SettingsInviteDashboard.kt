package com.example.usc1.domain.model

data class SettingsInviteDashboard(
    val entries: List<SettingsInviteEntry> = emptyList(),
    val totalCreatedToday: Int = 0,
    val remainingToday: Int = DefaultDailyLimit,
    val limitPerDay: Int = DefaultDailyLimit,
) {
    val activeCount: Int
        get() = entries.count { it.activity == SettingsInviteActivity.Active }

    val approvedCount: Int
        get() = entries.count { it.approvalStatus == SettingsInviteApprovalStatus.Approved }

    val historyCount: Int
        get() = entries.size - activeCount

    companion object {
        const val DefaultDailyLimit = 5
    }
}

data class SettingsMentorshipHub(
    val labels: AdminMentorshipLabelsConfig = AdminMentorshipLabelsConfig(),
    val mentor: SettingsMentorshipRoleCard? = null,
    val mentee: SettingsMentorshipRoleCard? = null,
    val incoming: List<SettingsMentorshipRequest> = emptyList(),
    val outgoing: List<SettingsMentorshipRequest> = emptyList(),
)

data class SettingsMentorshipRoleCard(
    val relationshipId: String,
    val user: SettingsMentorshipUser,
    val roleLabel: String,
)

data class SettingsMentorshipRequest(
    val id: String,
    val otherUser: SettingsMentorshipUser?,
    val status: SettingsMentorshipStatus,
    val direction: SettingsMentorshipDirection,
    val roleLabel: String,
    val createdAt: String,
)

data class SettingsMentorshipUser(
    val id: String,
    val name: String,
    val classCode: String,
    val photoUrl: String,
)

enum class SettingsMentorshipStatus(val label: String) {
    Pending("Pendente"),
    Accepted("Aceito"),
    Rejected("Recusado"),
    Cancelled("Cancelado"),
}

enum class SettingsMentorshipDirection {
    Incoming,
    Outgoing,
}

data class SettingsInviteEntry(
    val id: String,
    val token: String,
    val createdAt: String,
    val expiresAt: String,
    val usesCount: Int,
    val maxUses: Int,
    val requesterName: String = "",
    val requesterEmail: String = "",
    val requesterClass: String = "",
    val requestedAt: String = "",
    val approvalStatus: SettingsInviteApprovalStatus = SettingsInviteApprovalStatus.Unused,
    val activity: SettingsInviteActivity = SettingsInviteActivity.Active,
)

enum class SettingsInviteActivity(val label: String) {
    Active("Ativo"),
    Expired("Expirado"),
    Revoked("Revogado"),
    Closed("Encerrado"),
}

enum class SettingsInviteApprovalStatus(val label: String) {
    Approved("Aprovado"),
    Pending("Aguardando"),
    Rejected("Não aprovado"),
    Unused("Sem uso"),
}
