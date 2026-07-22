package com.example.usc1.data.repository

import android.util.Log
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.GuestSessionPolicy
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AuthRepository {
    private val _session = MutableStateFlow(UserSession())
    override val session: StateFlow<UserSession> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): UserSession {
        throw UnsupportedOperationException(
            "O web app não usa login por e-mail e senha. Use o login com Google.",
        )
    }

    override suspend fun signInWithGoogle(): UserSession = withContext(Dispatchers.IO) {
        val client = clientProvider()
        Log.d(Tag, "login Google iniciado")
        Log.d(Tag, "redirect usado: ${SupabaseClientProvider.AndroidAuthRedirectUrl}")
        client.auth.signInWith(
            provider = Google,
            redirectUrl = SupabaseClientProvider.AndroidAuthRedirectUrl,
        )
        Log.d(
            Tag,
            "sessão após iniciar OAuth: ${if (client.auth.currentSessionOrNull() != null) "sim" else "não"}",
        )
        hydrateCurrentSession(client)
    }

    override suspend fun loginAsGuest(): UserSession {
        val guestSession = UserSession(
            user = AuthUser(
                id = "guest_virtual_android",
                name = "Visitante USC",
                email = "visitante@usc.app",
                avatarUrl = null,
                role = UserRole.Guest,
                status = UserStatus.Ativo,
            ),
            tenant = null,
            status = AuthStatus.Authenticated,
        )
        _session.value = guestSession
        return guestSession
    }

    override suspend fun selectGuestTenant(tenant: TenantContext): UserSession {
        return GuestSessionPolicy.attachTenant(_session.value, tenant).also { nextSession ->
            _session.value = nextSession
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        inviteCode: String?,
    ): UserSession = withContext(Dispatchers.IO) {
        val token = inviteCode?.trim().orEmpty()
        require(token.isNotBlank()) {
            "Informe o código de convite enviado pela atlética."
        }

        val client = clientProvider()
        hydrateCurrentSession(client)
        client.postgrest.rpc(
            function = "tenant_request_join_with_invite",
            parameters = buildJsonObject {
                put("p_token", token)
            },
        )
        hydrateCurrentSession(client)
    }

    override suspend fun refreshSession(): UserSession = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            return@withContext UserSession().also { _session.value = it }
        }
        hydrateCurrentSession(clientProvider())
    }

    override suspend fun requireInvite(): UserSession {
        throw UnsupportedOperationException("Status de convite agora vem do Supabase.")
    }

    override suspend fun markBanned(): UserSession {
        throw UnsupportedOperationException("Status de bloqueio agora vem do Supabase.")
    }

    override suspend fun signOut(): UserSession = withContext(Dispatchers.IO) {
        if (SupabaseClientProvider.config.isConfigured) {
            runCatching { clientProvider().auth.signOut() }
        }
        UserSession().also { _session.value = it }
    }

    private suspend fun hydrateCurrentSession(client: SupabaseClient): UserSession {
        val authSession = client.auth.currentSessionOrNull()
        val authUser = authSession?.user ?: return UserSession().also {
            Log.d(Tag, "usuário autenticado: não")
            _session.value = it
        }
        Log.d(Tag, "usuário autenticado: sim")
        val userId = authUser.id
        val email = authUser.email.orEmpty()
        val row = fetchOrCreateUserRow(client, userId, email)
        val membership = fetchMembership(client, userId, row.tenantId)
        val tenant = fetchTenant(client, membership?.tenantId ?: row.tenantId)
        val nextSession = mapSession(row, membership, tenant, userId, email)
        Log.d(Tag, "tenant ativo resolvido: ${if (nextSession.tenant != null) "sim" else "não"}")
        _session.value = nextSession
        return nextSession
    }

    private suspend fun fetchOrCreateUserRow(
        client: SupabaseClient,
        userId: String,
        email: String,
    ): AuthUserRow {
        val existing = fetchUserRow(client, userId)
        if (existing != null) return existing

        val fallbackName = email.substringBefore("@").ifBlank { "Sem Nome" }
        client.from(UsersTable).insert(
            NewUserInsertRow(
                uid = userId,
                nome = fallbackName,
                email = email,
                foto = DefaultAvatarUrl,
            ),
        )
        return fetchUserRow(client, userId)
            ?: AuthUserRow(
                uid = userId,
                nome = fallbackName,
                email = email,
                foto = DefaultAvatarUrl,
                role = "guest",
                status = "ativo",
            )
    }

    private suspend fun fetchUserRow(client: SupabaseClient, userId: String): AuthUserRow? {
        return client.from(UsersTable)
            .select(columns = Columns.raw(UserSessionColumns)) {
                filter {
                    eq("uid", userId)
                }
                limit(count = 1)
            }
            .decodeList<AuthUserRow>()
            .firstOrNull()
    }

    private suspend fun fetchMembership(
        client: SupabaseClient,
        userId: String,
        preferredTenantId: String?,
    ): TenantMembershipRow? {
        val rows = client.from(TenantMembershipsTable)
            .select(columns = Columns.raw(TenantMembershipColumns)) {
                filter {
                    eq("user_id", userId)
                }
                limit(count = 20)
            }
            .decodeList<TenantMembershipRow>()

        Log.d(Tag, "memberships encontrados: ${rows.size}")
        val cleanPreferredTenantId = preferredTenantId.orEmpty().trim()
        return rows.firstOrNull { it.tenantId == cleanPreferredTenantId }
            ?: rows.firstOrNull { it.status == TenantMembershipStatus.Pending.remoteValue }
            ?: rows.firstOrNull { it.status == TenantMembershipStatus.Approved.remoteValue }
            ?: rows.firstOrNull()
    }

    private suspend fun fetchTenant(client: SupabaseClient, tenantId: String?): TenantRow? {
        val cleanTenantId = tenantId.orEmpty().trim()
        if (cleanTenantId.isBlank()) return null

        return client.from(TenantsTable)
            .select(columns = Columns.raw(TenantColumns)) {
                filter {
                    eq("id", cleanTenantId)
                }
                limit(count = 1)
            }
            .decodeList<TenantRow>()
            .firstOrNull()
    }

    private fun mapSession(
        row: AuthUserRow,
        membership: TenantMembershipRow?,
        tenant: TenantRow?,
        fallbackUid: String,
        fallbackEmail: String,
    ): UserSession {
        val platformRole = UserRole.fromRemote(row.role)
        val membershipRole = UserRole.fromRemote(membership?.role ?: row.tenantRole)
        val effectiveRole = if (platformRole == UserRole.Master) {
            UserRole.Master
        } else {
            membershipRole.takeUnless { it == UserRole.Visitante } ?: platformRole
        }
        val userStatus = UserStatus.fromRemote(row.status)
        val membershipStatus = TenantMembershipStatus.fromRemote(
            membership?.status ?: row.tenantStatus,
        )
        val tenantContext = tenant?.toTenantContext(membershipStatus)
        val authStatus = when {
            userStatus.isBlocked -> AuthStatus.Banned
            membershipStatus == TenantMembershipStatus.Pending -> AuthStatus.WaitingApproval
            platformRole == UserRole.Master -> AuthStatus.Authenticated
            tenantContext == null -> AuthStatus.InviteRequired
            membershipStatus == TenantMembershipStatus.Approved -> AuthStatus.Authenticated
            else -> AuthStatus.InviteRequired
        }

        return UserSession(
            user = AuthUser(
                id = row.uid.ifBlank { fallbackUid },
                name = row.nome.ifBlank { fallbackEmail.substringBefore("@").ifBlank { "Sem Nome" } },
                email = row.email.orEmpty().ifBlank { fallbackEmail },
                avatarUrl = row.foto?.ifBlank { null },
                registrationNumber = row.matricula.orEmpty().trim(),
                classCode = row.turma.orEmpty().trim(),
                planName = row.plano.orEmpty().trim(),
                planBadge = row.planoBadge.orEmpty().trim(),
                planColorKey = row.planoCor.orEmpty().trim().ifBlank { "zinc" },
                planIconKey = row.planoIcon.orEmpty().trim().ifBlank { "ghost" },
                planStatus = row.planoStatus.orEmpty().trim(),
                role = effectiveRole,
                status = userStatus,
            ),
            tenant = tenantContext,
            status = authStatus,
        )
    }

    private fun TenantRow.toTenantContext(
        membershipStatus: TenantMembershipStatus,
    ): TenantContext {
        return TenantContext(
            id = id,
            slug = slug,
            name = nome.ifBlank { slug.ifBlank { "Atlética" } },
            acronym = sigla.orEmpty().trim(),
            course = curso.orEmpty().trim(),
            logoUrl = logoUrl?.ifBlank { null },
            palette = TenantPalette.entries.firstOrNull { it.remoteValue == paletteKey } ?: TenantPalette.Green,
            membershipStatus = membershipStatus,
        )
    }

    private companion object {
        const val Tag = "USCAuth"
        const val UsersTable = "users"
        const val TenantMembershipsTable = "tenant_memberships"
        const val TenantsTable = "tenants"
        const val DefaultAvatarUrl = "https://github.com/shadcn.png"
        const val UserSessionColumns =
            "uid,nome,email,foto,role,status,tenant_id,tenant_role,tenant_status," +
                "matricula,turma,plano,plano_badge,plano_cor,plano_icon,plano_status"
        const val TenantMembershipColumns = "tenant_id,role,status,updated_at"
        const val TenantColumns = "id,nome,slug,sigla,curso,logo_url,palette_key,status"
    }
}

@Serializable
private data class AuthUserRow(
    val uid: String = "",
    val nome: String = "",
    val email: String? = null,
    val foto: String? = null,
    val role: String? = null,
    val status: String? = null,
    val matricula: String? = null,
    val turma: String? = null,
    val plano: String? = null,
    @SerialName("plano_badge") val planoBadge: String? = null,
    @SerialName("plano_cor") val planoCor: String? = null,
    @SerialName("plano_icon") val planoIcon: String? = null,
    @SerialName("plano_status") val planoStatus: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    @SerialName("tenant_status") val tenantStatus: String? = null,
)

@Serializable
private data class TenantMembershipRow(
    @SerialName("tenant_id") val tenantId: String = "",
    val role: String? = null,
    val status: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class TenantRow(
    val id: String = "",
    val nome: String = "",
    val slug: String = "",
    val sigla: String? = null,
    val curso: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("palette_key") val paletteKey: String? = null,
    val status: String? = null,
)

@Serializable
private data class NewUserInsertRow(
    val uid: String,
    val nome: String,
    val email: String,
    val foto: String,
    val role: String = "guest",
    val status: String = "ativo",
    val stats: JsonObject = buildJsonObject { },
    val plano: String = "Visitante",
    @SerialName("plano_badge") val planoBadge: String = "Visitante",
    @SerialName("plano_cor") val planoCor: String = "zinc",
    @SerialName("plano_icon") val planoIcon: String = "ghost",
)
