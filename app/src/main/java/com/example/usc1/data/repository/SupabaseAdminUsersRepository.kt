package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminPermissionRole
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserRoleUpdate
import com.example.usc1.domain.model.AdminUserProfile
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUserTurmaLeaderUpdate
import com.example.usc1.domain.model.AdminUserUpdate
import com.example.usc1.domain.model.AdminUsersFilters
import com.example.usc1.domain.model.AdminUsersPage
import com.example.usc1.domain.repository.AdminUsersRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminUsersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminUsersRepository {
    override suspend fun getUsersPage(
        pageSize: Int,
        cursorId: String?,
        filters: AdminUsersFilters,
        forceRefresh: Boolean,
    ): AdminUsersPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePageSize = pageSize.coerceIn(1, MaxUsersResults)
        val offset = cursorId?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val candidateUserIds = fetchTenantUserIds(client, tenantId)

        if (candidateUserIds.isEmpty()) {
            return@withContext AdminUsersPage(
                users = emptyList(),
                nextCursor = null,
                hasMore = false,
                tenantId = tenantId,
            )
        }

        val rows = client.from(UsersTable)
            .select(columns = Columns.raw(UserListColumns)) {
                filter {
                    isIn("uid", candidateUserIds)
                    applySearchOrLetters(filters)
                }
                order(column = "nome", order = Order.ASCENDING)
                order(column = "uid", order = Order.ASCENDING)
                range(offset.toLong()..(offset + safePageSize).toLong())
            }
            .decodeList<AdminUserRow>()

        val pageUserIds = rows.mapNotNull { it.uid.trim().takeIf(String::isNotBlank) }
        val membershipRoles = fetchMembershipRoles(client, tenantId, pageUserIds)
        val users = rows.mapNotNull { row ->
            row.toDomain(
                tenantId = tenantId,
                membershipRole = membershipRoles[row.uid.trim()],
            )
        }

        val hasMore = users.size > safePageSize
        AdminUsersPage(
            users = users.take(safePageSize),
            nextCursor = if (hasMore) (offset + safePageSize).toString() else null,
            hasMore = hasMore,
            tenantId = tenantId,
        )
    }

    override suspend fun getUserProfile(
        userId: String,
        forceRefresh: Boolean,
    ): AdminUserProfile? = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return@withContext null
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        ensureUserInTenant(client, cleanUserId, tenantId)

        client.from(UsersTable)
            .select(columns = Columns.raw(UserProfileColumns)) {
                filter {
                    eq("uid", cleanUserId)
                }
                limit(count = 1)
            }
            .decodeList<AdminUserProfileRow>()
            .firstOrNull()
            ?.toDomain(tenantId)
    }

    override suspend fun updateUser(payload: AdminUserUpdate): Unit = withContext(Dispatchers.IO) {
        val cleanUserId = payload.userId.trim()
        if (cleanUserId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        ensureUserInTenant(client, cleanUserId, tenantId)

        client.from(UsersTable)
            .update(
                mapOf(
                    "nome" to payload.nome.trim().take(120),
                    "telefone" to payload.telefone.trim().take(30),
                    "matricula" to payload.matricula.trim().take(40),
                    "turma" to payload.turma.trim().take(30),
                    "status" to payload.status.remoteValue,
                    "tier" to payload.plano.remoteValue,
                    "updatedAt" to java.time.OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("uid", cleanUserId)
                }
            }
    }

    override suspend fun setUserStatus(userId: String, status: AdminUserStatus): Unit = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        ensureUserInTenant(client, cleanUserId, tenantId)

        client.from(UsersTable)
            .update(
                mapOf(
                    "status" to status.remoteValue,
                    "updatedAt" to java.time.OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("uid", cleanUserId)
                }
            }
    }

    override suspend fun updateUserRole(payload: AdminUserRoleUpdate): Unit = withContext(Dispatchers.IO) {
        val cleanUserId = payload.targetUserId.trim()
        val role = AdminPermissionRole.normalize(payload.role)
        if (cleanUserId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        ensureUserInTenant(client, cleanUserId, tenantId)

        val now = java.time.OffsetDateTime.now().toString()
        var finalRole = role
        val primaryPatch = buildUserRolePatch(role = role, tenantId = tenantId, now = now)
        val primaryResult = runCatching {
            updateUserWithColumnFallback(client, cleanUserId, primaryPatch)
        }

        if (primaryResult.isFailure) {
            val fallbackRole = toFallbackTenantRole(role)
            val canRetry = fallbackRole != role && primaryResult.exceptionOrNull()?.message.isRoleConstraintError()
            if (!canRetry) {
                primaryResult.getOrThrow()
            }
            finalRole = fallbackRole
            updateUserWithColumnFallback(
                client = client,
                userId = cleanUserId,
                patch = buildUserRolePatch(role = fallbackRole, tenantId = tenantId, now = now),
            )
        }

        runCatching {
            client.from(TenantMembershipsTable)
                .update(
                    mapOf(
                        "role" to finalRole,
                        "status" to "approved",
                        "updated_at" to now,
                    ),
                ) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("user_id", cleanUserId)
                    }
                }
        }

        runCatching {
            client.from(ActivityLogsTable)
                .insert(
                    mapOf(
                        "tenant_id" to tenantId,
                        "userId" to payload.actorUserId.trim().ifBlank { "sistema" },
                        "userName" to payload.actorName.trim().ifBlank { "Admin Master" },
                        "action" to "UPDATE",
                        "resource" to "Permissoes - Cargos",
                        "details" to "Alterou cargo do usuário $cleanUserId para $finalRole",
                        "timestamp" to now,
                    ),
                )
        }
    }

    override suspend fun setUserTurmaLeader(payload: AdminUserTurmaLeaderUpdate): Unit = withContext(Dispatchers.IO) {
        val cleanUserId = payload.targetUserId.trim()
        if (cleanUserId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val actorUserId = client.auth.currentSessionOrNull()?.user?.id?.trim().orEmpty()
        if (actorUserId.isBlank()) {
            throw IllegalStateException("Sessão inválida para definir líder de turma.")
        }

        val actor = client.from(UsersTable)
            .select(columns = Columns.raw("uid,role,tenant_id,tenant_role,tenant_status")) {
                filter {
                    eq("uid", actorUserId)
                }
                limit(count = 1)
            }
            .decodeList<AdminActorUserRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Sessão inválida para definir líder de turma.")

        if (!actor.canAssignTurmaLeader()) {
            throw IllegalStateException("Apenas o master tenant pode definir líderes de turma.")
        }

        if (!actor.isPlatformMaster()) {
            ensureUserInTenant(client, cleanUserId, tenantId)
        }

        val currentExtra = client.from(UsersTable)
            .select(columns = Columns.raw("extra")) {
                filter {
                    eq("uid", cleanUserId)
                }
                limit(count = 1)
            }
            .decodeList<UserExtraRow>()
            .firstOrNull()
            ?.extra
            .asJsonObjectOrEmpty()

        val nextExtra = buildJsonObject {
            currentExtra.forEach { (key, value) ->
                put(key, value)
            }
            put("turmaLeader", JsonPrimitive(payload.enabled))
        }

        updateUserWithColumnFallback(
            client = client,
            userId = cleanUserId,
            patch = mapOf(
                "extra" to nextExtra,
                "updatedAt" to java.time.OffsetDateTime.now().toString(),
            ),
        )
    }

    override suspend fun deleteUser(userId: String): Unit = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        ensureUserInTenant(client, cleanUserId, tenantId)

        client.from(UsersTable)
            .delete {
                filter {
                    eq("uid", cleanUserId)
                }
            }
    }

    private suspend fun fetchTenantUserIds(
        client: SupabaseClient,
        tenantId: String,
    ): List<String> {
        return client.from(TenantMembershipsTable)
            .select(columns = Columns.raw("user_id")) {
                filter {
                    eq("tenant_id", tenantId)
                    isIn("status", TenantDirectoryStatuses)
                }
                limit(count = MaxUsersResults.toLong())
            }
            .decodeList<TenantMembershipUserIdRow>()
            .mapNotNull { it.userId.trim().takeIf(String::isNotBlank) }
            .distinct()
    }

    private suspend fun fetchMembershipRoles(
        client: SupabaseClient,
        tenantId: String,
        userIds: List<String>,
    ): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        return client.from(TenantMembershipsTable)
            .select(columns = Columns.raw("user_id,role")) {
                filter {
                    eq("tenant_id", tenantId)
                    isIn("status", TenantDirectoryStatuses)
                    isIn("user_id", userIds)
                }
                limit(count = userIds.size.toLong())
            }
            .decodeList<TenantMembershipRoleRow>()
            .mapNotNull { row ->
                val cleanUserId = row.userId.trim()
                if (cleanUserId.isBlank()) null else cleanUserId to row.role.orEmpty()
            }
            .toMap()
    }

    private suspend fun ensureUserInTenant(
        client: SupabaseClient,
        userId: String,
        tenantId: String,
    ) {
        val exists = client.from(TenantMembershipsTable)
            .select(columns = Columns.raw("id")) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("user_id", userId)
                    isIn("status", TenantDirectoryStatuses)
                }
                limit(count = 1)
            }
            .decodeList<TenantMembershipExistsRow>()
            .isNotEmpty()

        if (!exists) {
            throw IllegalStateException("Usuário fora do tenant ativo.")
        }
    }

    private suspend fun updateUserWithColumnFallback(
        client: SupabaseClient,
        userId: String,
        patch: Map<String, Any?>,
    ) {
        val mutablePatch = patch.toMutableMap()
        while (mutablePatch.isNotEmpty()) {
            val result = runCatching {
                client.from(UsersTable)
                    .update(mutablePatch) {
                        filter {
                            eq("uid", userId)
                        }
                    }
            }
            if (result.isSuccess) return
            val missingColumn = extractMissingColumn(result.exceptionOrNull()).trim()
            val removableKey = mutablePatch.keys.firstOrNull { it.equals(missingColumn, ignoreCase = true) }
            if (removableKey == null || removableKey == "id") {
                result.getOrThrow()
            }
            mutablePatch.remove(removableKey)
        }
    }

    private fun buildUserRolePatch(
        role: String,
        tenantId: String,
        now: String,
    ): Map<String, Any?> {
        val patch = mutableMapOf<String, Any?>(
            "role" to role,
            "tenant_role" to role,
            "tenant_status" to "approved",
            "tenant_id" to tenantId,
            "updatedAt" to now,
        )
        if (AdminPermissionRole.requiresAdminLegalAcceptance(role)) {
            patch["legal_admin_required_at"] = now
            patch["legal_admin_required_reason"] = "role:$role"
            patch["legal_admin_accepted_at"] = null
        }
        return patch
    }

    private fun toFallbackTenantRole(value: String): String {
        return when (val normalized = value.trim().lowercase()) {
            "admin_geral" -> "admin_tenant"
            "admin_tenant" -> "admin_geral"
            "master_tenant" -> "master"
            "master" -> "master_tenant"
            "visitante",
            "user",
            "mini_vendor",
            "vendas",
            "treinador",
            "empresa",
            "admin_treino",
            "admin_gestor",
            -> normalized
            else -> "user"
        }
    }

    private fun String?.isRoleConstraintError(): Boolean {
        val message = orEmpty().lowercase()
        return "tenant_role" in message || "role" in message || "check constraint" in message
    }

    private fun extractMissingColumn(error: Throwable?): String {
        val message = error?.message.orEmpty()
        val patterns = listOf(
            Regex("column\\s+[a-z0-9_]+\\.([a-z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("column\\s+([a-z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("could not find the [\"']?([a-z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.groupValues?.getOrNull(1)
        }.orEmpty()
    }

    private fun io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder.applySearchOrLetters(
        filters: AdminUsersFilters,
    ) {
        val term = filters.search.trim()
            .replace(Regex("[*,()]"), " ")
            .replace(Regex("\\s+"), " ")
            .take(80)
        if (term.isNotBlank()) {
            val pattern = "*$term*"
            or {
                ilike("nome", pattern)
                ilike("email", pattern)
                ilike("matricula", pattern)
                ilike("turma", pattern)
            }
            return
        }

        val letters = filters.letterGroup.letters
        if (letters.isNotEmpty()) {
            or {
                letters.forEach { letter ->
                    ilike("nome", "$letter*")
                }
            }
        }
    }

    private fun AdminUserRow.toDomain(
        tenantId: String,
        membershipRole: String?,
    ): AdminUserListItem? {
        val cleanId = uid.trim()
        if (cleanId.isBlank()) return null
        val roleLabel = tenantRole?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: role?.trim()?.takeIf { it.isNotBlank() }
            ?: membershipRole?.trim().orEmpty()
        return AdminUserListItem(
            id = cleanId,
            nome = nome.trim().ifBlank { "Sem Nome" },
            email = email?.trim().orEmpty().ifBlank { "---" },
            telefone = telefone?.trim().orEmpty(),
            turma = turma?.trim().orEmpty().ifBlank { "---" },
            matricula = matricula?.trim().orEmpty().ifBlank { "---" },
            status = AdminUserStatus.fromRemote(status),
            plano = AdminUserPlan.fromRemote(tier),
            foto = foto?.trim().orEmpty(),
            xp = xp ?: 0,
            role = roleLabel,
            tenantId = tenantId,
            isTurmaLeader = extra.isTurmaLeader(),
        )
    }

    private fun AdminUserProfileRow.toDomain(tenantId: String): AdminUserProfile? {
        val cleanId = uid.trim()
        if (cleanId.isBlank()) return null
        val roleLabel = tenantRole?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: role?.trim().orEmpty()

        return AdminUserProfile(
            id = cleanId,
            nome = nome.trim().ifBlank { "Sem Nome" },
            email = email?.trim().orEmpty(),
            foto = foto?.trim().orEmpty(),
            matricula = matricula?.trim().orEmpty(),
            turma = turma?.trim().orEmpty(),
            telefone = telefone?.trim().orEmpty(),
            status = AdminUserStatus.fromRemote(status),
            level = level ?: 0,
            xp = xp ?: 0,
            sharkCoins = sharkCoins ?: 0,
            planoBadge = planoBadge?.trim().orEmpty(),
            tier = AdminUserPlan.fromRemote(tier),
            patente = patente?.trim().orEmpty(),
            role = roleLabel.ifBlank { "visitante" },
        )
    }

    private companion object {
        const val MaxUsersResults = 520
        const val UsersTable = "users"
        const val TenantMembershipsTable = "tenant_memberships"
        const val ActivityLogsTable = "activity_logs"
        val TenantDirectoryStatuses = listOf("approved", "pending", "disabled")
        const val UserListColumns =
            "uid,nome,email,telefone,turma,matricula,status,tier,foto,xp,role,tenant_id,tenant_role,tenant_status,extra"
        const val UserProfileColumns =
            "uid,nome,email,foto,matricula,turma,telefone,status,level,xp,sharkCoins,plano_badge,tier,patente,createdAt,role,tenant_role,tenant_status"
    }
}

@Serializable
private data class AdminUserRow(
    val uid: String = "",
    val nome: String = "",
    val email: String? = null,
    val telefone: String? = null,
    val turma: String? = null,
    val matricula: String? = null,
    val status: String? = null,
    val tier: String? = null,
    val foto: String? = null,
    val xp: Long? = null,
    val role: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    @SerialName("tenant_status") val tenantStatus: String? = null,
    val extra: JsonElement? = null,
)

@Serializable
private data class AdminUserProfileRow(
    val uid: String = "",
    val nome: String = "",
    val email: String? = null,
    val foto: String? = null,
    val matricula: String? = null,
    val turma: String? = null,
    val telefone: String? = null,
    val status: String? = null,
    val level: Long? = null,
    val xp: Long? = null,
    @SerialName("sharkCoins") val sharkCoins: Long? = null,
    @SerialName("plano_badge") val planoBadge: String? = null,
    val tier: String? = null,
    val patente: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val role: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    @SerialName("tenant_status") val tenantStatus: String? = null,
)

@Serializable
private data class TenantMembershipUserIdRow(
    @SerialName("user_id") val userId: String = "",
)

@Serializable
private data class TenantMembershipRoleRow(
    @SerialName("user_id") val userId: String = "",
    val role: String? = null,
)

@Serializable
private data class TenantMembershipExistsRow(
    val id: String = "",
)

@Serializable
private data class UserExtraRow(
    val extra: JsonElement? = null,
)

@Serializable
private data class AdminActorUserRow(
    val uid: String = "",
    val role: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    @SerialName("tenant_status") val tenantStatus: String? = null,
) {
    fun isPlatformMaster(): Boolean {
        return role?.trim()?.lowercase() == "master"
    }

    fun canAssignTurmaLeader(): Boolean {
        return isPlatformMaster() || effectiveRole() == "master_tenant"
    }

    private fun effectiveRole(): String {
        return tenantRole?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: role?.trim()?.lowercase().orEmpty()
    }
}

private fun JsonElement?.isTurmaLeader(): Boolean {
    val obj = this as? JsonObject ?: return false
    val value = obj["turmaLeader"]
    if (value == null || value is JsonNull) return false
    return value.jsonPrimitive.booleanOrNull ?: false
}

private fun JsonElement?.asJsonObjectOrEmpty(): JsonObject {
    return this as? JsonObject ?: JsonObject(emptyMap())
}
