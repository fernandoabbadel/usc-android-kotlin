package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.AdminMentorshipCatalog
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig
import com.example.usc1.domain.model.SettingsInviteActivity
import com.example.usc1.domain.model.SettingsInviteApprovalStatus
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsInviteEntry
import com.example.usc1.domain.model.SettingsInviteQuota
import com.example.usc1.domain.model.SettingsInviteQuotaStatus
import com.example.usc1.domain.model.SettingsMentorshipAction
import com.example.usc1.domain.model.SettingsMentorshipCandidate
import com.example.usc1.domain.model.SettingsMentorshipDirection
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.model.SettingsMentorshipRequest
import com.example.usc1.domain.model.SettingsMentorshipRoleCard
import com.example.usc1.domain.model.SettingsMentorshipRoleOptions
import com.example.usc1.domain.model.SettingsMentorshipRoleSide
import com.example.usc1.domain.model.SettingsMentorshipStatus
import com.example.usc1.domain.model.SettingsMentorshipUser
import com.example.usc1.domain.model.SettingsSupportCategory
import com.example.usc1.domain.model.SettingsSupportTicket
import com.example.usc1.domain.model.SettingsTurmaLeaderPending
import com.example.usc1.domain.model.SettingsTurmaLeaderRequest
import com.example.usc1.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
                expiresAtMillis = invite.expiresAt.toInstantOrNull()?.toEpochMilli() ?: 0L,
                requesterName = requester?.nome.orEmpty().ifBlank { request?.requesterName.orEmpty() },
                requesterEmail = requester?.email.orEmpty().ifBlank { request?.requesterEmail.orEmpty() },
                requesterClass = requester?.turma.orEmpty().ifBlank { request?.requesterTurma.orEmpty() },
                requestedAt = formatIsoDateTime(request?.requestedAt.orEmpty()),
                approvalStatus = request?.status.toApprovalStatus(),
                activity = invite.toActivity(),
            )
        }

        val quota = readInviteQuota(client, cleanUserId, cleanTenantId)
        val todayStart = SettingsLocalDayFormatter.format(Instant.now().atZone(SettingsSaoPauloZone).toLocalDate())
        val totalCreatedToday = invites.count { it.createdAt.startsWith(todayStart) }
        SettingsInviteDashboard(
            entries = entries,
            totalCreatedToday = totalCreatedToday,
            remainingToday = (quota.totalLimit - totalCreatedToday).coerceAtLeast(0),
            limitPerDay = quota.totalLimit,
            quota = quota,
        )
    }

    override suspend fun revokeInvite(
        tenantId: String,
        userId: String,
        inviteId: String,
    ) = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanInviteId = inviteId.trim()
        require(cleanTenantId.isNotBlank() && cleanInviteId.isNotBlank()) {
            "Convite inválido para revogar."
        }

        val client = clientProvider()
        val patch = buildJsonObject {
            put("is_active", JsonPrimitive(false))
            put("is_revoked", JsonPrimitive(true))
            put("revoked_at", JsonPrimitive(Instant.now().toString()))
            put("revoked_by", if (cleanUserId.isBlank()) JsonNull else JsonPrimitive(cleanUserId))
        }

        client.from(TenantInvitesTable).update(patch) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("id", cleanInviteId)
                if (cleanUserId.isNotBlank()) {
                    eq("created_by", cleanUserId)
                }
            }
        }
        Unit
    }

    override suspend fun requestMoreInvites(
        tenantId: String,
        userId: String,
    ): SettingsInviteQuota = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        require(cleanTenantId.isNotBlank() && cleanUserId.isNotBlank()) {
            "Tenant inválido para pedir mais convites."
        }

        val client = clientProvider()
        val extra = readUserExtra(client, cleanUserId)
        val current = extra.toInviteQuota(cleanTenantId)
        if (!current.canRequestMore) {
            throw IllegalStateException(
                if (current.status == SettingsInviteQuotaStatus.Pending) {
                    "Seu pedido já foi feito. Os ${SettingsInviteDashboard.BonusLimit} convites extras liberam em até 1 hora."
                } else {
                    "Seu bonus de ${SettingsInviteDashboard.BonusLimit} convites extras ja foi liberado para hoje."
                },
            )
        }

        // Espelha `buildRequestedTenantInviteQuotaExtra`: preserva o resto de `extra`
        // e regrava só a entrada do tenant atual.
        val now = Instant.now()
        val unlockAt = now.plusMillis(SettingsInviteDashboard.BonusDelayMillis)
        val byTenant = (extra[InviteQuotaExtraKey] as? JsonObject) ?: JsonObject(emptyMap())
        val nextByTenant = buildJsonObject {
            byTenant.forEach { (key, value) -> if (key != cleanTenantId) put(key, value) }
            put(
                cleanTenantId,
                buildJsonObject {
                    put("requestedAt", JsonPrimitive(now.toString()))
                    put("unlockAt", JsonPrimitive(unlockAt.toString()))
                    put("bonusDayKey", JsonPrimitive(unlockAt.toSaoPauloDayKey()))
                },
            )
        }
        val nextExtra = buildJsonObject {
            extra.forEach { (key, value) -> if (key != InviteQuotaExtraKey) put(key, value) }
            put(InviteQuotaExtraKey, nextByTenant)
        }

        client.from(UsersTable).update(buildJsonObject { put("extra", nextExtra) }) {
            filter { eq("uid", cleanUserId) }
        }

        nextExtra.toInviteQuota(cleanTenantId)
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
                    // O card do padrinho é visto pelo afilhado: o lado editável é `mentee`.
                    SettingsMentorshipRoleCard(
                        relationshipId = row.id,
                        user = user,
                        roleLabel = row.mentorRoleLabel.ifBlank { labels.mentorLabel },
                        ownerRoleSide = SettingsMentorshipRoleSide.Mentee,
                        ownerRoleLabel = row.menteeRoleLabel.ifBlank { labels.menteeLabel },
                    )
                }
            },
            mentee = acceptedMenteeRow?.let { row ->
                previews[row.menteeUserId]?.toMentorshipUser()?.let { user ->
                    SettingsMentorshipRoleCard(
                        relationshipId = row.id,
                        user = user,
                        roleLabel = row.menteeRoleLabel.ifBlank { labels.menteeLabel },
                        ownerRoleSide = SettingsMentorshipRoleSide.Mentor,
                        ownerRoleLabel = row.mentorRoleLabel.ifBlank { labels.mentorLabel },
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

    override suspend fun getMentorshipCandidates(
        tenantId: String,
        userId: String,
        maxResults: Int,
    ): List<SettingsMentorshipCandidate> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext emptyList()
        }

        clientProvider().from(UsersTable)
            .select(columns = Columns.raw(UserPreviewColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("tenant_status", "approved")
                }
                limit(count = maxResults.coerceIn(1, 400).toLong())
            }
            .decodeList<UserPreviewRow>()
            .filter { it.uid.isNotBlank() && it.uid != cleanUserId }
            .map { row ->
                SettingsMentorshipCandidate(
                    id = row.uid,
                    name = row.nome.trim().ifBlank { "Atleta" },
                    classCode = row.turma.trim().ifBlank { "Sem turma" },
                    photoUrl = resolveRemoteImageUrl(row.foto).orEmpty(),
                )
            }
            .sortedWith(compareBy({ it.classCode }, { it.name }))
    }

    override suspend fun sendMentorshipInvite(
        tenantId: String,
        currentUserId: String,
        targetUserId: String,
        targetIsMentor: Boolean,
    ) = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanCurrentUserId = currentUserId.trim()
        val cleanTargetUserId = targetUserId.trim()
        require(cleanTenantId.isNotBlank() && cleanCurrentUserId.isNotBlank() && cleanTargetUserId.isNotBlank()) {
            "Dados invalidos para enviar convite de apadrinhamento."
        }
        if (cleanCurrentUserId == cleanTargetUserId) {
            throw IllegalStateException("Você não pode se relacionar consigo mesmo.")
        }

        val mentorUserId = if (targetIsMentor) cleanTargetUserId else cleanCurrentUserId
        val menteeUserId = if (targetIsMentor) cleanCurrentUserId else cleanTargetUserId

        val client = clientProvider()
        val tenantRows = fetchTenantMentorshipRows(client, cleanTenantId)
        val existing = tenantRows.firstOrNull { row ->
            row.mentorUserId == mentorUserId && row.menteeUserId == menteeUserId
        }
        if (existing != null && (existing.status == "pending" || existing.status == "accepted")) {
            throw IllegalStateException(
                if (existing.status == "accepted") {
                    "Esse vinculo de apadrinhamento ja esta ativo."
                } else {
                    "Ja existe um convite de apadrinhamento pendente para esse par."
                },
            )
        }

        ensureInviteSlotAvailable(tenantRows, mentorUserId, menteeUserId, existing?.id)

        val nowIso = Instant.now().toString()
        val basePatch = buildJsonObject {
            put("initiator_user_id", JsonPrimitive(cleanCurrentUserId))
            put("status", JsonPrimitive("pending"))
            put("message", JsonNull)
            put("mentor_role_label", JsonNull)
            put("mentee_role_label", JsonNull)
            put("responded_at", JsonNull)
            put("updated_at", JsonPrimitive(nowIso))
        }

        if (existing != null) {
            client.from(TenantMentorshipsTable).update(basePatch) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", existing.id)
                }
            }
        } else {
            val insertPayload = buildJsonObject {
                put("tenant_id", JsonPrimitive(cleanTenantId))
                put("mentor_user_id", JsonPrimitive(mentorUserId))
                put("mentee_user_id", JsonPrimitive(menteeUserId))
                basePatch.forEach { (key, value) -> put(key, value) }
            }
            client.from(TenantMentorshipsTable).insert(insertPayload)
        }
        Unit
    }

    override suspend fun respondToMentorshipInvite(
        tenantId: String,
        currentUserId: String,
        relationshipId: String,
        action: SettingsMentorshipAction,
        selectedRoleLabel: String,
    ) = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanCurrentUserId = currentUserId.trim()
        val cleanRelationshipId = relationshipId.trim()
        require(cleanTenantId.isNotBlank() && cleanCurrentUserId.isNotBlank() && cleanRelationshipId.isNotBlank()) {
            "Convite de apadrinhamento inválido."
        }

        val client = clientProvider()
        val tenantRows = fetchTenantMentorshipRows(client, cleanTenantId)
        val row = tenantRows.firstOrNull { it.id == cleanRelationshipId }
            ?: throw IllegalStateException("Convite não encontrado.")

        val isParticipant =
            row.mentorUserId == cleanCurrentUserId || row.menteeUserId == cleanCurrentUserId
        if (!isParticipant) {
            throw IllegalStateException("Sem permissão para responder esse convite.")
        }

        if (action == SettingsMentorshipAction.Remove) {
            if (row.status != "accepted") {
                throw IllegalStateException("So da para remover um vinculo ja aceito.")
            }
        } else if (row.status != "pending") {
            throw IllegalStateException("Esse convite ja foi resolvido.")
        }

        if (action == SettingsMentorshipAction.Cancel) {
            if (row.initiatorUserId != cleanCurrentUserId) {
                throw IllegalStateException("So quem enviou pode cancelar esse convite.")
            }
        } else if (action != SettingsMentorshipAction.Remove && row.initiatorUserId == cleanCurrentUserId) {
            throw IllegalStateException("A resposta deve ser dada pela outra pessoa.")
        }

        val nextStatus = when (action) {
            SettingsMentorshipAction.Accept -> "accepted"
            SettingsMentorshipAction.Reject -> "rejected"
            else -> "cancelled"
        }

        var mentorRoleLabel: String? = null
        var menteeRoleLabel: String? = null
        if (nextStatus == "accepted") {
            ensureInviteSlotAvailable(tenantRows, row.mentorUserId, row.menteeUserId, row.id)
            val labels = fetchMentorshipLabels(client, cleanTenantId)
            val currentSide = if (row.mentorUserId == cleanCurrentUserId) {
                SettingsMentorshipRoleSide.Mentor
            } else {
                SettingsMentorshipRoleSide.Mentee
            }
            val mentorOptions = SettingsMentorshipRoleOptions.resolve(labels, SettingsMentorshipRoleSide.Mentor)
            val menteeOptions = SettingsMentorshipRoleOptions.resolve(labels, SettingsMentorshipRoleSide.Mentee)
            val chosen = selectedRoleLabel.trim()
            mentorRoleLabel = if (currentSide == SettingsMentorshipRoleSide.Mentor && chosen.isNotBlank()) {
                chosen
            } else {
                mentorOptions.first()
            }
            menteeRoleLabel = if (currentSide == SettingsMentorshipRoleSide.Mentee && chosen.isNotBlank()) {
                chosen
            } else {
                menteeOptions.first()
            }
        }

        val nowIso = Instant.now().toString()
        val patch = buildJsonObject {
            put("status", JsonPrimitive(nextStatus))
            put("mentor_role_label", mentorRoleLabel?.let(::JsonPrimitive) ?: JsonNull)
            put("mentee_role_label", menteeRoleLabel?.let(::JsonPrimitive) ?: JsonNull)
            put("responded_at", JsonPrimitive(nowIso))
            put("updated_at", JsonPrimitive(nowIso))
        }

        client.from(TenantMentorshipsTable).update(patch) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("id", cleanRelationshipId)
            }
        }
        Unit
    }

    override suspend fun updateMentorshipRoleLabel(
        tenantId: String,
        currentUserId: String,
        relationshipId: String,
        roleSide: SettingsMentorshipRoleSide,
        roleLabel: String,
    ) = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanCurrentUserId = currentUserId.trim()
        val cleanRelationshipId = relationshipId.trim()
        val requestedLabel = roleLabel.trim()
        require(
            cleanTenantId.isNotBlank() && cleanCurrentUserId.isNotBlank() &&
                cleanRelationshipId.isNotBlank() && requestedLabel.isNotBlank(),
        ) {
            "Rótulo de apadrinhamento inválido."
        }

        val client = clientProvider()
        val row = fetchTenantMentorshipRows(client, cleanTenantId)
            .firstOrNull { it.id == cleanRelationshipId }
            ?: throw IllegalStateException("Vínculo não encontrado.")

        if (row.status != "accepted") {
            throw IllegalStateException("So da para editar o rotulo de um vinculo ativo.")
        }
        val editableSide = when (cleanCurrentUserId) {
            row.mentorUserId -> SettingsMentorshipRoleSide.Mentor
            row.menteeUserId -> SettingsMentorshipRoleSide.Mentee
            else -> throw IllegalStateException("Sem permissão para editar esse rótulo.")
        }
        if (roleSide != editableSide) {
            throw IllegalStateException("Você só pode editar o seu próprio rótulo.")
        }

        val labels = fetchMentorshipLabels(client, cleanTenantId)
        val validOptions = SettingsMentorshipRoleOptions.resolve(labels, editableSide)
        val nextRoleLabel = validOptions
            .firstOrNull { it.equals(requestedLabel, ignoreCase = true) }
            ?: validOptions.first()

        val column = if (editableSide == SettingsMentorshipRoleSide.Mentor) {
            "mentor_role_label"
        } else {
            "mentee_role_label"
        }
        client.from(TenantMentorshipsTable).update(
            buildJsonObject {
                put(column, JsonPrimitive(nextRoleLabel))
                put("updated_at", JsonPrimitive(Instant.now().toString()))
            },
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("id", cleanRelationshipId)
            }
        }
        Unit
    }

    override suspend fun getTurmaLeaderPending(
        tenantId: String,
        userId: String,
        userClass: String,
        canManageAll: Boolean,
    ): SettingsTurmaLeaderPending = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val leaderTurma = userClass.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext SettingsTurmaLeaderPending(
                leaderTurma = leaderTurma,
                canManageAll = canManageAll,
            )
        }

        val client = clientProvider()
        val requests = client.from(TenantJoinRequestsTable)
            .select(columns = Columns.raw(JoinRequestColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("status", "pending")
                }
                order(column = "requested_at", order = Order.DESCENDING)
                limit(count = 120)
            }
            .decodeList<TenantJoinRequestRow>()
            .filter { it.id.isNotBlank() }

        val requesterIds = requests
            .mapNotNull { it.requesterUserId.trim().takeIf(String::isNotBlank) }
            .distinct()
        val requesters = if (requesterIds.isEmpty()) {
            emptyMap()
        } else {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserPreviewColumns)) {
                    filter { isIn("uid", requesterIds) }
                }
                .decodeList<UserPreviewRow>()
                .associateBy { it.uid }
        }

        val inviteIds = requests
            .mapNotNull { it.inviteId.trim().takeIf(String::isNotBlank) }
            .distinct()
        val invites = if (inviteIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                client.from(TenantInvitesTable)
                    .select(columns = Columns.raw("id,token,created_by")) {
                        filter { isIn("id", inviteIds) }
                    }
                    .decodeList<SettingsTenantInviteLookupRow>()
                    .associateBy { it.id }
            }.getOrElse { emptyMap() }
        }

        val inviterIds = invites.values
            .mapNotNull { it.createdBy.trim().takeIf(String::isNotBlank) }
            .distinct()
        val inviters = if (inviterIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                client.from(UsersTable)
                    .select(columns = Columns.raw(UserPreviewColumns)) {
                        filter { isIn("uid", inviterIds) }
                    }
                    .decodeList<UserPreviewRow>()
                    .associateBy { it.uid }
            }.getOrElse { emptyMap() }
        }

        val mapped = requests.map { request ->
            val requester = requesters[request.requesterUserId]
            val invite = invites[request.inviteId]
            val inviter = invite?.createdBy?.let(inviters::get)
            SettingsTurmaLeaderRequest(
                id = request.id,
                requesterUserId = request.requesterUserId,
                requesterName = requester?.nome.orEmpty().ifBlank { request.requesterName },
                requesterEmail = requester?.email.orEmpty().ifBlank { request.requesterEmail },
                requesterClass = requester?.turma.orEmpty().ifBlank { request.requesterTurma },
                requesterPhotoUrl = resolveRemoteImageUrl(
                    requester?.foto.orEmpty().ifBlank { request.requesterPhoto },
                ).orEmpty(),
                requestedAtLabel = formatIsoDateTime(request.requestedAt),
                inviteToken = invite?.token.orEmpty(),
                inviterName = inviter?.nome.orEmpty(),
                inviterEmail = inviter?.email.orEmpty(),
            )
        }
            // Mesma regra do route handler: sem `canManageAll`, só a própria turma.
            .filter { canManageAll || it.requesterClass == leaderTurma }

        SettingsTurmaLeaderPending(
            requests = mapped,
            leaderTurma = leaderTurma,
            canManageAll = canManageAll,
        )
    }

    override suspend fun getSupportTickets(
        userId: String,
        maxResults: Int,
    ): List<SettingsSupportTicket> = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanUserId.isBlank()) {
            return@withContext emptyList()
        }

        runCatching {
            clientProvider().from(SupportRequestsTable)
                .select(columns = Columns.raw(SupportColumns)) {
                    filter { eq("userId", cleanUserId) }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = maxResults.coerceIn(1, 50).toLong())
                }
                .decodeList<SettingsSupportRequestRow>()
                .filter { it.id.isNotBlank() }
                .map { row ->
                    SettingsSupportTicket(
                        id = row.id,
                        category = SettingsSupportCategory.fromRemote(row.category).label,
                        subject = row.subject.trim().ifBlank { "Sem assunto" },
                        message = row.message.trim(),
                        isResolved = row.status.trim().lowercase() == "resolved",
                        response = row.response.orEmpty().trim(),
                        createdAtLabel = formatIsoDateTime(row.createdAt),
                    )
                }
        }.getOrElse { emptyList() }
    }

    override suspend fun toggleAccountStatus(
        userId: String,
        isCurrentlyActive: Boolean,
        currentRole: String,
        savedRole: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        require(cleanUserId.isNotBlank()) { "Usuário inválido para alteração de status." }

        val cleanCurrentRole = currentRole.trim().ifBlank { "user" }
        val cleanSavedRole = savedRole.trim()
        val nextStatus = if (isCurrentlyActive) "paused" else "ativo"
        val nextRole = if (isCurrentlyActive) {
            "inactive"
        } else {
            cleanSavedRole.ifBlank { cleanCurrentRole }
        }

        val patch = buildJsonObject {
            put("status", JsonPrimitive(nextStatus))
            put("role", JsonPrimitive(nextRole))
            put("saved_role", if (isCurrentlyActive) JsonPrimitive(cleanCurrentRole) else JsonNull)
            put("updatedAt", JsonPrimitive(Instant.now().toString()))
        }

        clientProvider().from(UsersTable).update(patch) {
            filter { eq("uid", cleanUserId) }
        }
        // true quando a conta ficou pausada.
        isCurrentlyActive
    }

    override suspend fun softDeleteAccount(
        userId: String,
        photoUrl: String,
    ) = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        require(cleanUserId.isNotBlank()) { "Usuário inválido para exclusão." }

        val nowIso = Instant.now().toString()
        val patch = buildJsonObject {
            put("nome", JsonPrimitive("Usuário Excluído"))
            put("email", JsonPrimitive("deleted_$cleanUserId@usc.invalid"))
            put(
                "foto",
                JsonPrimitive(photoUrl.trim().ifBlank { "https://github.com/shadcn.png" }),
            )
            put("status", JsonPrimitive("deleted"))
            put("role", JsonPrimitive("banned"))
            put("turma", JsonPrimitive("N/A"))
            put("deletedAt", JsonPrimitive(nowIso))
            put("cpf", JsonNull)
            put("telefone", JsonNull)
            put("instagram", JsonNull)
            put("linkedin", JsonNull)
            put("saved_role", JsonNull)
            put("updatedAt", JsonPrimitive(nowIso))
        }

        clientProvider().from(UsersTable).update(patch) {
            filter { eq("uid", cleanUserId) }
        }
        Unit
    }

    override suspend fun submitSupportTicket(
        tenantId: String,
        userId: String,
        userName: String,
        userEmail: String,
        category: SettingsSupportCategory,
        subject: String,
        message: String,
    ) = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        val cleanSubject = subject.trim().take(SettingsSupportCategory.SubjectMaxLength)
        val cleanMessage = message.trim().take(800)
        require(cleanUserId.isNotBlank()) { "Usuário inválido para abrir chamado." }
        require(cleanSubject.isNotBlank() && cleanMessage.isNotBlank()) {
            "Assunto e mensagem são obrigatórios."
        }

        val nowIso = Instant.now().toString()
        val payload = buildJsonObject {
            put("userId", JsonPrimitive(cleanUserId))
            put("userName", JsonPrimitive(userName.trim().take(80).ifBlank { "Usuário" }))
            put("userEmail", JsonPrimitive(userEmail.trim().take(120)))
            put("category", JsonPrimitive(category.remoteValue))
            put("subject", JsonPrimitive(cleanSubject))
            put("message", JsonPrimitive(cleanMessage))
            put("status", JsonPrimitive("pending"))
            put("readByAdmin", JsonPrimitive(false))
            put("createdAt", JsonPrimitive(nowIso))
            put("updatedAt", JsonPrimitive(nowIso))
            tenantId.trim().takeIf(String::isNotBlank)?.let { put("tenant_id", JsonPrimitive(it)) }
        }

        clientProvider().from(SupportRequestsTable).insert(payload)
        Unit
    }

    /**
     * Espelha `ensureInviteSlotAvailable`: 1 afilhado por padrinho e 1 padrinho por afilhado,
     * contando vínculos `pending` e `accepted`.
     */
    private fun ensureInviteSlotAvailable(
        tenantRows: List<TenantMentorshipRow>,
        mentorUserId: String,
        menteeUserId: String,
        ignoreRelationshipId: String?,
    ) {
        val activeRows = tenantRows.filter { row ->
            row.id != ignoreRelationshipId && (row.status == "pending" || row.status == "accepted")
        }
        if (activeRows.any { it.mentorUserId == mentorUserId }) {
            throw IllegalStateException("Esse perfil ja tem um afilhado/afilhada ativo ou pendente.")
        }
        if (activeRows.any { it.menteeUserId == menteeUserId }) {
            throw IllegalStateException("Esse perfil ja tem um padrinho/madrinha ativo ou pendente.")
        }
    }

    private suspend fun fetchTenantMentorshipRows(
        client: SupabaseClient,
        tenantId: String,
    ): List<TenantMentorshipRow> {
        return runCatching {
            client.from(TenantMentorshipsTable)
                .select(columns = Columns.raw(MentorshipColumns)) {
                    filter { eq("tenant_id", tenantId) }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(count = 500)
                }
                .decodeList<TenantMentorshipRow>()
        }.getOrElse { emptyList() }
    }

    private suspend fun readUserExtra(
        client: SupabaseClient,
        userId: String,
    ): JsonObject {
        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid,extra")) {
                    filter { eq("uid", userId) }
                    limit(count = 1)
                }
                .decodeList<SettingsUserExtraRow>()
                .firstOrNull()
                ?.extra
        }.getOrNull() ?: JsonObject(emptyMap())
    }

    private suspend fun readInviteQuota(
        client: SupabaseClient,
        userId: String,
        tenantId: String,
    ): SettingsInviteQuota = readUserExtra(client, userId).toInviteQuota(tenantId)

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
        const val SupportRequestsTable = "support_requests"
        const val SupportColumns =
            "id,userId,userName,userEmail,category,subject,message,status,response,createdAt,updatedAt"
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
private data class SettingsUserExtraRow(
    val uid: String = "",
    val extra: JsonObject? = null,
)

@Serializable
private data class SettingsTenantInviteLookupRow(
    val id: String = "",
    val token: String = "",
    @SerialName("created_by") val createdBy: String = "",
)

@Serializable
private data class SettingsSupportRequestRow(
    val id: String = "",
    @SerialName("userId") val userId: String = "",
    @SerialName("userName") val userName: String = "",
    @SerialName("userEmail") val userEmail: String = "",
    val category: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "",
    val response: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = "",
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
        photoUrl = resolveRemoteImageUrl(foto).orEmpty(),
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

private const val InviteQuotaExtraKey = "memberInviteQuotaByTenant"

/**
 * Espelha `resolveTenantInviteQuotaState`: lê
 * `users.extra.memberInviteQuotaByTenant[tenantId]` e decide entre pendente, liberado e ocioso.
 */
private fun JsonObject.toInviteQuota(tenantId: String): SettingsInviteQuota {
    val entry = (this[InviteQuotaExtraKey] as? JsonObject)?.get(tenantId) as? JsonObject
        ?: return SettingsInviteQuota()

    val requestedAt = entry.quotaString("requestedAt")
    val unlockAtRaw = entry.quotaString("unlockAt")
    val bonusDayKey = entry.quotaString("bonusDayKey")
    val unlockAtMillis = unlockAtRaw.toInstantOrNull()?.toEpochMilli() ?: 0L
    val now = Instant.now()
    val todayKey = now.toSaoPauloDayKey()

    return when {
        unlockAtMillis > now.toEpochMilli() -> SettingsInviteQuota(
            requestedAt = requestedAt,
            unlockAtMillis = unlockAtMillis,
            bonusDayKey = bonusDayKey,
            status = SettingsInviteQuotaStatus.Pending,
            bonusLimit = 0,
        )

        bonusDayKey.isNotBlank() && bonusDayKey == todayKey -> SettingsInviteQuota(
            requestedAt = requestedAt,
            unlockAtMillis = unlockAtMillis,
            bonusDayKey = bonusDayKey,
            status = SettingsInviteQuotaStatus.Granted,
            bonusLimit = SettingsInviteDashboard.BonusLimit,
        )

        else -> SettingsInviteQuota(
            requestedAt = requestedAt,
            unlockAtMillis = unlockAtMillis,
            bonusDayKey = bonusDayKey,
            status = SettingsInviteQuotaStatus.Idle,
            bonusLimit = 0,
        )
    }
}

private fun JsonObject.quotaString(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

/** `resolveInviteQuotaDayKey`: chave `yyyy-MM-dd` no fuso de São Paulo. */
private fun Instant.toSaoPauloDayKey(): String =
    SettingsLocalDayFormatter.format(atZone(SettingsSaoPauloZone).toLocalDate())

private val SettingsSaoPauloZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val SettingsLocalDayFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val SettingsDisplayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm")
    .withZone(SettingsSaoPauloZone)
