package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.AdminMentorshipCatalog
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig
import com.example.usc1.domain.model.SettingsInviteActivity
import com.example.usc1.domain.model.SettingsInviteApprovalStatus
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsInviteEntry
import com.example.usc1.domain.model.SettingsMentorshipDirection
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.model.SettingsMentorshipRequest
import com.example.usc1.domain.model.SettingsMentorshipRoleCard
import com.example.usc1.domain.model.SettingsMentorshipStatus
import com.example.usc1.domain.model.SettingsMentorshipUser
import com.example.usc1.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SupabaseSettingsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : SettingsRepository {
    override suspend fun getInviteDashboard(
        tenantId: String,
        userId: String,
        limit: Int,
    ): SettingsInviteDashboard = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext SettingsInviteDashboard()
        }

        val client = clientProvider()
        val safeLimit = limit.coerceIn(1, 80)
        val invites = client.from(TenantInvitesTable)
            .select(columns = Columns.raw(InviteColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("created_by", cleanUserId)
                }
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = safeLimit.toLong())
            }
            .decodeList<TenantInviteRow>()

        val inviteIds = invites.mapNotNull { row -> row.id.trim().takeIf(String::isNotBlank) }
        val requests = if (inviteIds.isEmpty()) {
            emptyList()
        } else {
            client.from(TenantJoinRequestsTable)
                .select(columns = Columns.raw(JoinRequestColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        isIn("invite_id", inviteIds)
                    }
                }
                .decodeList<TenantJoinRequestRow>()
        }

        val requestByInviteId = requests
            .filter { it.inviteId.isNotBlank() }
            .associateBy { it.inviteId }
        val requesterIds = requests.mapNotNull { it.requesterUserId.trim().takeIf(String::isNotBlank) }
        val usersById = if (requesterIds.isEmpty()) {
            emptyMap()
        } else {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserPreviewColumns)) {
                    filter {
                        isIn("uid", requesterIds.distinct())
                    }
                }
                .decodeList<UserPreviewRow>()
                .associateBy { it.uid }
        }

        val entries = invites.map { invite ->
            val request = requestByInviteId[invite.id]
            val requester = request?.requesterUserId?.let(usersById::get)
            SettingsInviteEntry(
                id = invite.id,
                token = invite.token,
                createdAt = formatIsoDateTime(invite.createdAt),
                expiresAt = formatIsoDateTime(invite.expiresAt),
                usesCount = invite.usesCount,
                maxUses = invite.maxUses,
                requesterName = requester?.nome.orEmpty().ifBlank { request?.requesterName.orEmpty() },
                requesterEmail = requester?.email.orEmpty().ifBlank { request?.requesterEmail.orEmpty() },
                requesterClass = requester?.turma.orEmpty().ifBlank { request?.requesterTurma.orEmpty() },
                requestedAt = formatIsoDateTime(request?.requestedAt.orEmpty()),
                approvalStatus = request?.status.toApprovalStatus(),
                activity = invite.toActivity(),
            )
        }

        val todayStart = SettingsLocalDayFormatter.format(Instant.now().atZone(SettingsSaoPauloZone).toLocalDate())
        val totalCreatedToday = invites.count { it.createdAt.startsWith(todayStart) }
        SettingsInviteDashboard(
            entries = entries,
            totalCreatedToday = totalCreatedToday,
            remainingToday = (SettingsInviteDashboard.DefaultDailyLimit - totalCreatedToday).coerceAtLeast(0),
            limitPerDay = SettingsInviteDashboard.DefaultDailyLimit,
        )
    }

    override suspend fun getMentorshipHub(
        tenantId: String,
        userId: String,
    ): SettingsMentorshipHub = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext SettingsMentorshipHub()
        }

        val client = clientProvider()
        val labels = fetchMentorshipLabels(client, cleanTenantId)
        val tenantRows = runCatching {
            client.from(TenantMentorshipsTable)
                .select(columns = Columns.raw(MentorshipColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(count = 500)
                }
                .decodeList<TenantMentorshipRow>()
        }.getOrElse { emptyList() }

        val userRows = tenantRows.filter { row ->
            row.mentorUserId == cleanUserId || row.menteeUserId == cleanUserId
        }
        val previewIds = userRows
            .flatMap { row -> listOf(row.mentorUserId, row.menteeUserId) }
            .map(String::trim)
            .filter { it.isNotBlank() && it != cleanUserId }
            .distinct()
        val previews = if (previewIds.isEmpty()) {
            emptyMap()
        } else {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserPreviewColumns)) {
                    filter {
                        isIn("uid", previewIds)
                    }
                }
                .decodeList<UserPreviewRow>()
                .associateBy { it.uid }
        }

        val acceptedMentorRow = userRows.firstOrNull { row ->
            row.status == "accepted" && row.menteeUserId == cleanUserId
        }
        val acceptedMenteeRow = userRows.firstOrNull { row ->
            row.status == "accepted" && row.mentorUserId == cleanUserId
        }

        SettingsMentorshipHub(
            labels = labels,
            mentor = acceptedMentorRow?.let { row ->
                previews[row.mentorUserId]?.toMentorshipUser()?.let { user ->
                    SettingsMentorshipRoleCard(
                        relationshipId = row.id,
                        user = user,
                        roleLabel = row.mentorRoleLabel.ifBlank { labels.mentorLabel },
                    )
                }
            },
            mentee = acceptedMenteeRow?.let { row ->
                previews[row.menteeUserId]?.toMentorshipUser()?.let { user ->
                    SettingsMentorshipRoleCard(
                        relationshipId = row.id,
                        user = user,
                        roleLabel = row.menteeRoleLabel.ifBlank { labels.menteeLabel },
                    )
                }
            },
            incoming = userRows
                .filter { row -> row.status == "pending" && row.initiatorUserId != cleanUserId }
                .map { row -> row.toMentorshipRequest(cleanUserId, labels, previews, SettingsMentorshipDirection.Incoming) },
            outgoing = userRows
                .filter { row -> row.status == "pending" && row.initiatorUserId == cleanUserId }
                .map { row -> row.toMentorshipRequest(cleanUserId, labels, previews, SettingsMentorshipDirection.Outgoing) },
        )
    }

    private suspend fun fetchMentorshipLabels(
        client: SupabaseClient,
        tenantId: String,
    ): AdminMentorshipLabelsConfig {
        val scopedId = "tenant:${tenantId.trim()}::${AdminMentorshipCatalog.LabelsDocId}"
        return runCatching {
            client.from(AppConfigTable)
                .select(columns = Columns.raw("id,tenant_id,data")) {
                    filter {
                        eq("id", scopedId)
                    }
                    limit(count = 1)
                }
                .decodeList<SettingsMentorshipLabelsRow>()
                .firstOrNull()
                ?.toDomain()
        }.getOrNull() ?: AdminMentorshipLabelsConfig()
    }

    private companion object {
        const val TenantInvitesTable = "tenant_invites"
        const val TenantJoinRequestsTable = "tenant_join_requests"
        const val TenantMentorshipsTable = "tenant_mentorships"
        const val AppConfigTable = "app_config"
        const val UsersTable = "users"
        const val InviteColumns =
            "id,tenant_id,token,role_to_assign,requires_approval,max_uses,uses_count,expires_at,is_active,is_revoked,revoked_at,revoked_by,created_by,created_at"
        const val JoinRequestColumns =
            "id,tenant_id,requester_user_id,invite_id,status,requested_role,approved_role,requested_at,reviewed_at,rejection_reason,requester_name,requester_email,requester_turma,requester_photo"
        const val MentorshipColumns =
            "id,tenant_id,mentor_user_id,mentee_user_id,initiator_user_id,status,message,mentor_role_label,mentee_role_label,responded_at,created_at,updated_at"
        const val UserPreviewColumns = "uid,nome,email,turma,foto"

    }
}

@Serializable
private data class TenantInviteRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    val token: String = "",
    @SerialName("role_to_assign") val roleToAssign: String = "",
    @SerialName("requires_approval") val requiresApproval: Boolean = true,
    @SerialName("max_uses") val maxUses: Int = 1,
    @SerialName("uses_count") val usesCount: Int = 0,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_revoked") val isRevoked: Boolean = false,
    @SerialName("revoked_at") val revokedAt: String = "",
    @SerialName("revoked_by") val revokedBy: String = "",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
private data class TenantJoinRequestRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("requester_user_id") val requesterUserId: String = "",
    @SerialName("invite_id") val inviteId: String = "",
    val status: String = "",
    @SerialName("requested_role") val requestedRole: String = "",
    @SerialName("approved_role") val approvedRole: String = "",
    @SerialName("requested_at") val requestedAt: String = "",
    @SerialName("reviewed_at") val reviewedAt: String = "",
    @SerialName("rejection_reason") val rejectionReason: String = "",
    @SerialName("requester_name") val requesterName: String = "",
    @SerialName("requester_email") val requesterEmail: String = "",
    @SerialName("requester_turma") val requesterTurma: String = "",
    @SerialName("requester_photo") val requesterPhoto: String = "",
)

@Serializable
private data class UserPreviewRow(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val turma: String = "",
    val foto: String = "",
)

@Serializable
private data class TenantMentorshipRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("mentor_user_id") val mentorUserId: String = "",
    @SerialName("mentee_user_id") val menteeUserId: String = "",
    @SerialName("initiator_user_id") val initiatorUserId: String = "",
    val status: String = "",
    val message: String = "",
    @SerialName("mentor_role_label") val mentorRoleLabel: String = "",
    @SerialName("mentee_role_label") val menteeRoleLabel: String = "",
    @SerialName("responded_at") val respondedAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
private data class SettingsMentorshipLabelsRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val data: JsonObject? = null,
) {
    fun toDomain(): AdminMentorshipLabelsConfig {
        val fallback = AdminMentorshipLabelsConfig()
        return AdminMentorshipCatalog.normalize(
            AdminMentorshipLabelsConfig(
                hubTitle = data.string("hubTitle").ifBlank { fallback.hubTitle },
                mentorLabel = data.string("mentorLabel").ifBlank { fallback.mentorLabel },
                menteeLabel = data.string("menteeLabel").ifBlank { fallback.menteeLabel },
                inviteMentorLabel = data.string("inviteMentorLabel").ifBlank { fallback.inviteMentorLabel },
                inviteMenteeLabel = data.string("inviteMenteeLabel").ifBlank { fallback.inviteMenteeLabel },
                requestHelpText = data.string("requestHelpText").ifBlank { fallback.requestHelpText },
            ),
        )
    }
}

private fun TenantInviteRow.toActivity(): SettingsInviteActivity {
    if (isRevoked) return SettingsInviteActivity.Revoked
    if (!isActive) return SettingsInviteActivity.Closed
    val expiresAtInstant = expiresAt.toInstantOrNull()
    if (expiresAtInstant != null && expiresAtInstant <= Instant.now()) {
        return SettingsInviteActivity.Expired
    }
    if (maxUses > 0 && usesCount >= maxUses) return SettingsInviteActivity.Closed
    return SettingsInviteActivity.Active
}

private fun TenantMentorshipRow.toMentorshipRequest(
    currentUserId: String,
    labels: AdminMentorshipLabelsConfig,
    previews: Map<String, UserPreviewRow>,
    direction: SettingsMentorshipDirection,
): SettingsMentorshipRequest {
    val isCurrentMentor = mentorUserId == currentUserId
    val otherUserId = if (isCurrentMentor) menteeUserId else mentorUserId
    val roleLabel = if (isCurrentMentor) {
        menteeRoleLabel.ifBlank { labels.menteeLabel }
    } else {
        mentorRoleLabel.ifBlank { labels.mentorLabel }
    }
    return SettingsMentorshipRequest(
        id = id,
        otherUser = previews[otherUserId]?.toMentorshipUser(),
        status = status.toMentorshipStatus(),
        direction = direction,
        roleLabel = roleLabel,
        createdAt = formatIsoDateTime(createdAt),
    )
}

private fun UserPreviewRow.toMentorshipUser(): SettingsMentorshipUser {
    return SettingsMentorshipUser(
        id = uid,
        name = nome,
        classCode = turma,
        photoUrl = foto,
    )
}

private fun String.toMentorshipStatus(): SettingsMentorshipStatus = when (trim().lowercase()) {
    "accepted" -> SettingsMentorshipStatus.Accepted
    "rejected" -> SettingsMentorshipStatus.Rejected
    "cancelled" -> SettingsMentorshipStatus.Cancelled
    else -> SettingsMentorshipStatus.Pending
}

private fun String?.toApprovalStatus(): SettingsInviteApprovalStatus = when (this?.trim()?.lowercase()) {
    "approved" -> SettingsInviteApprovalStatus.Approved
    "pending" -> SettingsInviteApprovalStatus.Pending
    "rejected" -> SettingsInviteApprovalStatus.Rejected
    else -> SettingsInviteApprovalStatus.Unused
}

private fun formatIsoDateTime(value: String): String {
    val instant = value.toInstantOrNull() ?: return ""
    return SettingsDisplayFormatter.format(instant)
}

private fun String.toInstantOrNull(): Instant? {
    val value = trim()
    if (value.isBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun JsonObject?.string(key: String): String {
    return this?.get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}

private val SettingsSaoPauloZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val SettingsLocalDayFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val SettingsDisplayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm")
    .withZone(SettingsSaoPauloZone)
