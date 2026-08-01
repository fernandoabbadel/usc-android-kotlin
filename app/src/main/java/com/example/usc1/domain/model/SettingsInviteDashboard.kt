package com.example.usc1.domain.model

data class SettingsInviteDashboard(
    val entries: List<SettingsInviteEntry> = emptyList(),
    val totalCreatedToday: Int = 0,
    val remainingToday: Int = DefaultDailyLimit,
    val limitPerDay: Int = DefaultDailyLimit,
    val quota: SettingsInviteQuota = SettingsInviteQuota(),
) {
    /**
     * Espelha `activeEntries` do web: convite ainda válido e que ainda não virou
     * cadastro aprovado. Um convite aprovado sai da lista de ativos e vai para o histórico.
     */
    val activeEntries: List<SettingsInviteEntry>
        get() = entries.filter {
            it.activity == SettingsInviteActivity.Active &&
                it.approvalStatus != SettingsInviteApprovalStatus.Approved
        }

    val approvedEntries: List<SettingsInviteEntry>
        get() = entries.filter { it.approvalStatus == SettingsInviteApprovalStatus.Approved }

    val closedEntries: List<SettingsInviteEntry>
        get() = entries.filter {
            it.approvalStatus != SettingsInviteApprovalStatus.Approved &&
                it.activity != SettingsInviteActivity.Active
        }

    val activeCount: Int
        get() = activeEntries.size

    val approvedCount: Int
        get() = approvedEntries.size

    val historyCount: Int
        get() = approvedEntries.size + closedEntries.size

    companion object {
        const val DefaultDailyLimit = 5
        const val BonusLimit = 5
        const val BonusDelayMillis = 60L * 60L * 1000L
    }
}

/**
 * Espelho de `TenantInviteQuotaState` (`web-reference/src/lib/inviteQuota.ts`).
 * O bônus fica guardado em `users.extra.memberInviteQuotaByTenant[tenantId]`.
 */
data class SettingsInviteQuota(
    val requestedAt: String = "",
    val unlockAtMillis: Long = 0L,
    val bonusDayKey: String = "",
    val status: SettingsInviteQuotaStatus = SettingsInviteQuotaStatus.Idle,
    val bonusLimit: Int = 0,
) {
    val totalLimit: Int
        get() = SettingsInviteDashboard.DefaultDailyLimit + bonusLimit

    val canRequestMore: Boolean
        get() = status == SettingsInviteQuotaStatus.Idle

    fun remainingMillis(nowMillis: Long): Long =
        if (status == SettingsInviteQuotaStatus.Pending) {
            (unlockAtMillis - nowMillis).coerceAtLeast(0L)
        } else {
            0L
        }
}

enum class SettingsInviteQuotaStatus {
    Idle,
    Pending,
    Granted,
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
    /** Lado do vínculo ocupado pelo próprio usuário — é o único rótulo que ele pode editar. */
    val ownerRoleSide: SettingsMentorshipRoleSide = SettingsMentorshipRoleSide.Mentor,
    val ownerRoleLabel: String = "",
)

enum class SettingsMentorshipRoleSide {
    Mentor,
    Mentee,
}

enum class SettingsMentorshipAction {
    Accept,
    Reject,
    Cancel,
    Remove,
}

/** Candidato do seletor "Adicionar vínculo" (turma → aluno). */
data class SettingsMentorshipCandidate(
    val id: String,
    val name: String,
    val classCode: String,
    val photoUrl: String = "",
)

object SettingsMentorshipRoleOptions {
    private val SplitRegex = Regex("[\\\\/|]")

    /**
     * Espelho de `resolveMentorshipRoleOptions`: o label configurado pelo tenant pode trazer
     * variações separadas por `/`, `\` ou `|` ("Padrinho/Madrinha") e cada uma vira uma opção.
     */
    fun resolve(
        labels: AdminMentorshipLabelsConfig,
        side: SettingsMentorshipRoleSide,
    ): List<String> {
        val raw = when (side) {
            SettingsMentorshipRoleSide.Mentor -> labels.mentorLabel
            SettingsMentorshipRoleSide.Mentee -> labels.menteeLabel
        }
        val fallback = when (side) {
            SettingsMentorshipRoleSide.Mentor -> AdminMentorshipLabelsConfig().mentorLabel
            SettingsMentorshipRoleSide.Mentee -> AdminMentorshipLabelsConfig().menteeLabel
        }
        val options = raw.split(SplitRegex)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        return options.ifEmpty { listOf(fallback) }
    }
}

/**
 * Espelho de `TurmaLeaderPendingRequest` (`/api/turma-leader/pendentes`).
 */
data class SettingsTurmaLeaderPending(
    val requests: List<SettingsTurmaLeaderRequest> = emptyList(),
    val leaderTurma: String = "",
    val canManageAll: Boolean = false,
)

data class SettingsTurmaLeaderRequest(
    val id: String,
    val requesterUserId: String,
    val requesterName: String,
    val requesterEmail: String,
    val requesterClass: String,
    val requesterPhotoUrl: String,
    val requestedAtLabel: String,
    val inviteToken: String,
    val inviterName: String,
    val inviterEmail: String,
)

/** Espelho de `SupportTicketRecord` (tabela `support_requests`). */
data class SettingsSupportTicket(
    val id: String,
    val category: String,
    val subject: String,
    val message: String,
    val isResolved: Boolean,
    val response: String,
    val createdAtLabel: String,
)

enum class SettingsSupportCategory(val remoteValue: String, val label: String) {
    Geral("geral", "Geral"),
    Financeiro("financeiro", "Financeiro"),
    Conta("conta", "Conta"),
    Bug("bug", "Bug"),
    Denuncia("denuncia", "Denúncia"),
    Sugestoes("sugestorias", "Sugestões"),
    Outro("outro", "Outro"),
    ;

    companion object {
        const val SubjectMaxLength = 50
        const val MessageMaxLength = 300

        fun fromRemote(value: String?): SettingsSupportCategory {
            val clean = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == clean } ?: Geral
        }
    }
}

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
    val expiresAtMillis: Long = 0L,
    val requesterName: String = "",
    val requesterEmail: String = "",
    val requesterClass: String = "",
    val requestedAt: String = "",
    val approvalStatus: SettingsInviteApprovalStatus = SettingsInviteApprovalStatus.Unused,
    val activity: SettingsInviteActivity = SettingsInviteActivity.Active,
) {
    /** Mesma regra de `formatInviteCountdown` do web: dias+horas, horas, ou "Expirado". */
    fun countdownLabel(nowMillis: Long): String {
        if (expiresAtMillis <= 0L) return "Sem validade"
        val diff = expiresAtMillis - nowMillis
        if (diff <= 0L) return "Expirado"
        val totalHours = diff / (1000L * 60L * 60L)
        val totalDays = totalHours / 24L
        return if (totalDays >= 1L) "${totalDays}d ${totalHours % 24L}h" else "${totalHours.coerceAtLeast(1L)}h"
    }

    val isRevocable: Boolean
        get() = activity == SettingsInviteActivity.Active
}

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
