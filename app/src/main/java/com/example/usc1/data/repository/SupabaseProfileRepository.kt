package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.ProfileBundle
import com.example.usc1.domain.model.ProfileEventItem
import com.example.usc1.domain.model.ProfileFollowUser
import com.example.usc1.domain.model.ProfileLeagueItem
import com.example.usc1.domain.model.ProfilePost
import com.example.usc1.domain.model.ProfileTrainingItem
import com.example.usc1.domain.model.UserProfileDetail
import com.example.usc1.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Espelha `web-reference/src/lib/profilePublicService.ts`:
 * users + posts + eventos_rsvps/eventos + treinos_rsvps/treinos +
 * ligas_membros/ligas_config + users_followers/users_following + profile_affinities.
 */
class SupabaseProfileRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : ProfileRepository {

    override suspend fun getProfileBundle(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
    ): ProfileBundle = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para carregar o perfil.")
        }
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }
        val cleanTargetId = targetUserId.trim().ifBlank { viewerUserId.trim() }
        val cleanViewerId = viewerUserId.trim()
        if (cleanTargetId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para abrir o perfil.")
        }

        val userRow = fetchUserRow(client, cleanTenantId, cleanTargetId)
            ?: throw IllegalStateException("Perfil não encontrado nesta atlética.")
        val profile = userRow.toDomain()

        val posts = runCatching { fetchPosts(client, cleanTenantId, cleanTargetId) }.getOrDefault(emptyList())
        val eventos = runCatching { fetchEvents(client, cleanTenantId, cleanTargetId) }.getOrDefault(emptyList())
        val treinos = runCatching { fetchTrainings(client, cleanTenantId, cleanTargetId) }.getOrDefault(emptyList())
        val ligas = runCatching { fetchLeagues(client, cleanTenantId, cleanTargetId) }.getOrDefault(emptyList())

        val followersCount = runCatching { countFollowRows(client, FollowersTable, cleanTenantId, cleanTargetId) }
            .getOrDefault(profile.followersCount)
        val followingCount = runCatching { countFollowRows(client, FollowingTable, cleanTenantId, cleanTargetId) }
            .getOrDefault(profile.followingCount)

        val isOwnProfile = cleanViewerId.isNotBlank() && cleanViewerId == cleanTargetId
        val isFollowing = if (isOwnProfile || cleanViewerId.isBlank()) {
            false
        } else {
            runCatching { checkIsFollowing(client, cleanTenantId, cleanTargetId, cleanViewerId) }.getOrDefault(false)
        }
        val affinitySent = if (isOwnProfile || cleanViewerId.isBlank() || cleanTenantId.isBlank()) {
            false
        } else {
            runCatching { checkAffinity(client, cleanTenantId, cleanViewerId, cleanTargetId) }.getOrDefault(false)
        }

        ProfileBundle(
            profile = profile.copy(
                followersCount = followersCount,
                followingCount = followingCount,
            ),
            posts = posts,
            eventos = eventos,
            treinos = treinos,
            ligas = ligas,
            followersCount = followersCount,
            followingCount = followingCount,
            isFollowing = isFollowing,
            isOwnProfile = isOwnProfile,
            affinitySent = affinitySent,
        )
    }

    override suspend fun toggleFollow(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
        follow: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = tenantId.trim()
        val cleanTargetId = targetUserId.trim()
        val cleanViewerId = viewerUserId.trim()
        if (cleanTargetId.isBlank() || cleanViewerId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para seguir este perfil.")
        }
        if (cleanTargetId == cleanViewerId) {
            throw IllegalStateException("Você não pode seguir o próprio perfil.")
        }

        if (!follow) {
            client.from(FollowersTable).delete {
                filter {
                    eq("userId", cleanTargetId)
                    eq("uid", cleanViewerId)
                    if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
                }
            }
            client.from(FollowingTable).delete {
                filter {
                    eq("userId", cleanViewerId)
                    eq("uid", cleanTargetId)
                    if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
                }
            }
            return@withContext
        }

        val viewer = fetchFollowIdentity(client, cleanTenantId, cleanViewerId)
        val target = fetchFollowIdentity(client, cleanTenantId, cleanTargetId)
        val now = Instant.now().toString()

        insertWithOptionalColumnFallback(
            client = client,
            table = FollowersTable,
            nonRemovableColumns = setOf("userId", "uid"),
            payload = jsonPayloadOf(
                "userId" to cleanTargetId,
                "uid" to cleanViewerId,
                "nome" to viewer.nome,
                "foto" to viewer.foto,
                "turma" to viewer.turma,
                "tenant_id" to cleanTenantId.takeIf(String::isNotBlank),
                "followedAt" to now,
            ),
        )
        insertWithOptionalColumnFallback(
            client = client,
            table = FollowingTable,
            nonRemovableColumns = setOf("userId", "uid"),
            payload = jsonPayloadOf(
                "userId" to cleanViewerId,
                "uid" to cleanTargetId,
                "nome" to target.nome,
                "foto" to target.foto,
                "turma" to target.turma,
                "tenant_id" to cleanTenantId.takeIf(String::isNotBlank),
                "followedAt" to now,
            ),
        )
    }

    override suspend fun getFollowList(
        tenantId: String,
        targetUserId: String,
        followers: Boolean,
    ): List<ProfileFollowUser> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = tenantId.trim()
        val cleanTargetId = targetUserId.trim()
        if (cleanTargetId.isBlank()) return@withContext emptyList()

        val table = if (followers) FollowersTable else FollowingTable
        runCatching {
            client.from(table)
                .select(columns = Columns.raw("uid,nome,foto,turma")) {
                    filter {
                        eq("userId", cleanTargetId)
                        if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
                    }
                    limit(count = FollowListPageSize)
                }
                .decodeList<ProfileFollowRowDto>()
                .mapNotNull { row ->
                    val uid = row.uid.orEmpty().trim()
                    if (uid.isBlank()) return@mapNotNull null
                    ProfileFollowUser(
                        uid = uid,
                        nome = row.nome.orEmpty().trim().ifBlank { "Atleta" },
                        foto = resolveRemoteImageUrl(row.foto),
                        turma = row.turma.orEmpty().trim().ifBlank { "Geral" },
                    )
                }
        }.getOrDefault(emptyList())
    }

    override suspend fun toggleAffinity(
        tenantId: String,
        targetUserId: String,
        viewerUserId: String,
        send: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = tenantId.trim()
        val cleanTargetId = targetUserId.trim()
        val cleanViewerId = viewerUserId.trim()
        if (cleanTenantId.isBlank() || cleanTargetId.isBlank() || cleanViewerId.isBlank()) {
            throw IllegalStateException("Entre na atlética para enviar um crush.")
        }
        if (cleanTargetId == cleanViewerId) {
            throw IllegalStateException("Você não pode enviar crush para si mesmo.")
        }

        if (!send) {
            client.from(AffinitiesTable).delete {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("from_user_id", cleanViewerId)
                    eq("to_user_id", cleanTargetId)
                }
            }
            return@withContext
        }

        val now = Instant.now().toString()
        insertWithOptionalColumnFallback(
            client = client,
            table = AffinitiesTable,
            nonRemovableColumns = setOf("tenant_id", "from_user_id", "to_user_id"),
            payload = jsonPayloadOf(
                "tenant_id" to cleanTenantId,
                "from_user_id" to cleanViewerId,
                "to_user_id" to cleanTargetId,
                "created_at" to now,
                "updated_at" to now,
            ),
        )
    }

    private suspend fun fetchUserRow(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): ProfileUserRow? {
        val scoped = runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(ProfileUserColumns)) {
                    filter {
                        eq("uid", uid)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<ProfileUserRow>()
                .firstOrNull()
        }.getOrNull()
        if (scoped != null) return scoped

        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(ProfileUserColumns)) {
                    filter { eq("uid", uid) }
                    limit(count = 1)
                }
                .decodeList<ProfileUserRow>()
                .firstOrNull()
        }.getOrNull()
    }

    private suspend fun fetchPosts(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): List<ProfilePost> {
        return client.from(PostsTable)
            .select(columns = Columns.raw("id,texto,imagem,likes,comentarios,userId,createdAt")) {
                filter {
                    eq("userId", uid)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxPostResults)
            }
            .decodeList<ProfilePostRow>()
            .mapNotNull { row ->
                val id = row.id.orEmpty().trim()
                if (id.isBlank()) return@mapNotNull null
                ProfilePost(
                    id = id,
                    texto = row.texto.orEmpty().trim(),
                    imagem = resolveRemoteImageUrl(row.imagem),
                    likes = row.likes.orEmpty().size,
                    comentarios = (row.comentarios ?: 0).coerceAtLeast(0),
                    createdAt = row.createdAt.orEmpty(),
                    timeLabel = formatRelativeTime(row.createdAt.orEmpty()),
                )
            }
    }

    private suspend fun fetchEvents(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): List<ProfileEventItem> {
        val eventIds = client.from(EventRsvpsTable)
            .select(columns = Columns.raw("eventoId")) {
                filter {
                    eq("userId", uid)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxEventResults * 4)
            }
            .decodeList<ProfileEventRsvpRow>()
            .mapNotNull { it.eventoId?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        if (eventIds.isEmpty()) return emptyList()

        return client.from(EventsTable)
            .select(columns = Columns.raw("id,titulo,data,local,imagem")) {
                filter {
                    isIn("id", eventIds)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxEventResults)
            }
            .decodeList<ProfileEventRow>()
            .mapNotNull { row ->
                val id = row.id.orEmpty().trim()
                if (id.isBlank()) return@mapNotNull null
                ProfileEventItem(
                    id = id,
                    titulo = row.titulo.orEmpty().trim().ifBlank { "Evento" },
                    data = row.data.orEmpty().trim(),
                    local = row.local.orEmpty().trim(),
                    imagem = resolveRemoteImageUrl(row.imagem),
                )
            }
            .sortedBy { it.data }
    }

    private suspend fun fetchTrainings(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): List<ProfileTrainingItem> {
        val trainingIds = client.from(TrainingRsvpsTable)
            .select(columns = Columns.raw("treinoId")) {
                filter {
                    eq("userId", uid)
                    eq("status", "going")
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxTrainingResults * 4)
            }
            .decodeList<ProfileTrainingRsvpRow>()
            .mapNotNull { it.treinoId?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        if (trainingIds.isEmpty()) return emptyList()

        return client.from(TrainingsTable)
            .select(columns = Columns.raw("id,modalidade,dia,horario,imagem,local")) {
                filter {
                    isIn("id", trainingIds)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxTrainingResults)
            }
            .decodeList<ProfileTrainingRow>()
            .mapNotNull { row ->
                val id = row.id.orEmpty().trim()
                if (id.isBlank()) return@mapNotNull null
                ProfileTrainingItem(
                    id = id,
                    modalidade = row.modalidade.orEmpty().trim().ifBlank { "Treino" },
                    dia = row.dia.orEmpty().trim(),
                    horario = row.horario.orEmpty().trim(),
                    local = row.local.orEmpty().trim(),
                    imagem = resolveRemoteImageUrl(row.imagem),
                )
            }
    }

    private suspend fun fetchLeagues(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): List<ProfileLeagueItem> {
        val leagueIds = client.from(LeagueMembersTable)
            .select(columns = Columns.raw("ligaId")) {
                filter {
                    eq("userId", uid)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxLeagueResults * 4)
            }
            .decodeList<ProfileLeagueMemberRow>()
            .mapNotNull { it.ligaId?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        if (leagueIds.isEmpty()) return emptyList()

        return client.from(LeaguesTable)
            .select(columns = Columns.raw("id,nome,sigla,foto,logo,logoUrl,membros")) {
                filter {
                    isIn("id", leagueIds)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = MaxLeagueResults)
            }
            .decodeList<ProfileLeagueRow>()
            .mapNotNull { row ->
                val id = row.id.orEmpty().trim()
                if (id.isBlank()) return@mapNotNull null
                ProfileLeagueItem(
                    id = id,
                    nome = row.nome.orEmpty().trim().ifBlank { "Liga" },
                    sigla = row.sigla.orEmpty().trim(),
                    logo = resolveRemoteImageUrl(
                        listOf(row.logoUrl, row.logo, row.foto)
                            .firstOrNull { !it.isNullOrBlank() },
                    ),
                    membros = row.membros ?: 0,
                )
            }
    }

    private suspend fun countFollowRows(
        client: SupabaseClient,
        table: String,
        tenantId: String,
        uid: String,
    ): Int {
        return client.from(table)
            .select(columns = Columns.raw("uid")) {
                filter {
                    eq("userId", uid)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = FollowCountLimit)
            }
            .decodeList<ProfileFollowRowDto>()
            .size
    }

    private suspend fun checkIsFollowing(
        client: SupabaseClient,
        tenantId: String,
        targetUid: String,
        viewerUid: String,
    ): Boolean {
        return client.from(FollowersTable)
            .select(columns = Columns.raw("uid")) {
                filter {
                    eq("userId", targetUid)
                    eq("uid", viewerUid)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<ProfileFollowRowDto>()
            .isNotEmpty()
    }

    private suspend fun checkAffinity(
        client: SupabaseClient,
        tenantId: String,
        viewerUid: String,
        targetUid: String,
    ): Boolean {
        return client.from(AffinitiesTable)
            .select(columns = Columns.raw("id")) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("from_user_id", viewerUid)
                    eq("to_user_id", targetUid)
                }
                limit(count = 1)
            }
            .decodeList<ProfileAffinityRowDto>()
            .isNotEmpty()
    }

    private suspend fun fetchFollowIdentity(
        client: SupabaseClient,
        tenantId: String,
        uid: String,
    ): ProfileFollowUser {
        val row = runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid,nome,foto,turma")) {
                    filter {
                        eq("uid", uid)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<ProfileFollowRowDto>()
                .firstOrNull()
        }.getOrNull()
        return ProfileFollowUser(
            uid = uid,
            nome = row?.nome.orEmpty().trim().ifBlank { "Atleta" },
            foto = row?.foto.orEmpty().trim(),
            turma = row?.turma.orEmpty().trim().ifBlank { "Geral" },
        )
    }

    private companion object {
        const val UsersTable = "users"
        const val PostsTable = "posts"
        const val EventRsvpsTable = "eventos_rsvps"
        const val EventsTable = "eventos"
        const val TrainingRsvpsTable = "treinos_rsvps"
        const val TrainingsTable = "treinos"
        const val LeagueMembersTable = "ligas_membros"
        const val LeaguesTable = "ligas_config"
        const val FollowersTable = "users_followers"
        const val FollowingTable = "users_following"
        const val AffinitiesTable = "profile_affinities"

        const val MaxPostResults = 10L
        const val MaxEventResults = 12L
        const val MaxTrainingResults = 12L
        const val MaxLeagueResults = 12L
        const val FollowCountLimit = 1000L
        const val FollowListPageSize = 100L

        const val ProfileUserColumns =
            "uid,nome,apelido,foto,turma,bio,instagram,instagramPublico,telefone,cidadeOrigem," +
                "dataNascimento,role,tenant_id,tenant_role,status,profile_public,profile_photo_public," +
                "allow_profile_discovery,whatsappPublico,idadePublica,relacionamentoPublico,signo," +
                "signoPublico,ascendente,ascendentePublico,lugarEspecial,comidaPreferida," +
                "musicaPreferida,corPreferida,esportes,pets,statusRelacionamento,plano,plano_cor," +
                "plano_icon,patente,patente_icon,patente_cor,tier,level,xp,stats"
    }
}

private val ProfileDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.of("America/Sao_Paulo"))

private fun formatRelativeTime(value: String): String {
    val instant = parseProfileInstant(value) ?: return ""
    val duration = Duration.between(instant, Instant.now()).takeIf { !it.isNegative } ?: Duration.ZERO
    val minutes = duration.toMinutes()
    return when {
        minutes < 1 -> "Agora"
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> "${duration.toHours()} h"
        minutes < 7 * 24 * 60 -> "${duration.toDays()} dias"
        else -> ProfileDateFormatter.format(instant)
    }
}

private fun parseProfileInstant(value: String): Instant? {
    val clean = value.trim()
    if (clean.isBlank()) return null
    return runCatching { OffsetDateTime.parse(clean).toInstant() }
        .getOrElse { runCatching { Instant.parse(clean) }.getOrNull() }
}

private fun jsonPayloadOf(vararg pairs: Pair<String, Any?>): LinkedHashMap<String, JsonElement> {
    val payload = LinkedHashMap<String, JsonElement>()
    pairs.forEach { (key, value) ->
        when (value) {
            null -> Unit
            is JsonElement -> payload[key] = value
            is String -> if (value.isNotBlank()) payload[key] = JsonPrimitive(value)
            is Boolean -> payload[key] = JsonPrimitive(value)
            is Number -> payload[key] = JsonPrimitive(value)
            else -> payload[key] = JsonPrimitive(value.toString())
        }
    }
    return payload
}

private suspend fun insertWithOptionalColumnFallback(
    client: SupabaseClient,
    table: String,
    nonRemovableColumns: Set<String>,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from(table).upsert(JsonObject(mutablePayload))
            return
        } catch (error: Throwable) {
            val missingColumn = extractProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in nonRemovableColumns ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                throw error
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private fun extractProblematicColumn(error: Throwable): String? {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
    val patterns = listOf(
        Regex("column\\s+[a-z0-9_]+\\.([a-zA-Z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("column\\s+[\"']?([a-zA-Z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("could not find the [\"']?([a-zA-Z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
    )
    return patterns.firstNotNullOfOrNull { pattern -> pattern.find(message)?.groupValues?.getOrNull(1) }
}

@Serializable
private data class ProfileUserRow(
    val uid: String = "",
    val nome: String? = null,
    val apelido: String? = null,
    val foto: String? = null,
    val turma: String? = null,
    val bio: String? = null,
    val instagram: String? = null,
    val instagramPublico: Boolean? = null,
    val telefone: String? = null,
    val cidadeOrigem: String? = null,
    val dataNascimento: String? = null,
    val role: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    val status: String? = null,
    @SerialName("profile_public") val profilePublic: Boolean? = null,
    @SerialName("profile_photo_public") val profilePhotoPublic: Boolean? = null,
    @SerialName("allow_profile_discovery") val allowProfileDiscovery: Boolean? = null,
    val whatsappPublico: Boolean? = null,
    val idadePublica: Boolean? = null,
    val relacionamentoPublico: Boolean? = null,
    val signo: String? = null,
    val signoPublico: Boolean? = null,
    val ascendente: String? = null,
    val ascendentePublico: Boolean? = null,
    val lugarEspecial: List<String>? = null,
    val comidaPreferida: List<String>? = null,
    val musicaPreferida: List<String>? = null,
    val corPreferida: String? = null,
    val esportes: List<String>? = null,
    val pets: String? = null,
    val statusRelacionamento: String? = null,
    val plano: String? = null,
    @SerialName("plano_cor") val planoCor: String? = null,
    @SerialName("plano_icon") val planoIcon: String? = null,
    val patente: String? = null,
    @SerialName("patente_icon") val patenteIcon: String? = null,
    @SerialName("patente_cor") val patenteCor: String? = null,
    val tier: String? = null,
    val level: Int? = null,
    val xp: Int? = null,
    val stats: JsonElement? = null,
) {
    fun toDomain(): UserProfileDetail {
        val statsObject = stats as? JsonObject
        return UserProfileDetail(
            uid = uid.trim(),
            nome = nome.orEmpty().trim().ifBlank { "Sem Nome" },
            apelido = apelido.orEmpty().trim(),
            foto = resolveRemoteImageUrl(foto),
            turma = turma.orEmpty().trim(),
            bio = bio.orEmpty().trim(),
            cidadeOrigem = cidadeOrigem.orEmpty().trim(),
            dataNascimento = dataNascimento.orEmpty().trim(),
            instagram = instagram.orEmpty().trim(),
            instagramPublico = instagramPublico ?: false,
            telefone = telefone.orEmpty().trim(),
            whatsappPublico = whatsappPublico ?: false,
            idadePublica = idadePublica ?: true,
            relacionamentoPublico = relacionamentoPublico ?: false,
            statusRelacionamento = statusRelacionamento.orEmpty().trim(),
            signo = signo.orEmpty().trim(),
            signoPublico = signoPublico ?: false,
            ascendente = ascendente.orEmpty().trim(),
            ascendentePublico = ascendentePublico ?: false,
            lugarEspecial = lugarEspecial.orEmpty().filter(String::isNotBlank),
            comidaPreferida = comidaPreferida.orEmpty().filter(String::isNotBlank),
            musicaPreferida = musicaPreferida.orEmpty().filter(String::isNotBlank),
            corPreferida = corPreferida.orEmpty().trim(),
            esportes = esportes.orEmpty().filter(String::isNotBlank),
            pets = pets.orEmpty().trim(),
            role = role.orEmpty().trim(),
            tenantId = tenantId.orEmpty().trim(),
            tenantRole = tenantRole.orEmpty().trim(),
            status = status.orEmpty().trim(),
            profilePublic = profilePublic ?: true,
            profilePhotoPublic = profilePhotoPublic ?: true,
            allowProfileDiscovery = allowProfileDiscovery ?: true,
            plano = plano.orEmpty().trim(),
            planoCor = planoCor.orEmpty().trim(),
            planoIcon = planoIcon.orEmpty().trim(),
            patente = patente.orEmpty().trim(),
            patenteIcon = patenteIcon.orEmpty().trim(),
            patenteCor = patenteCor.orEmpty().trim(),
            tier = tier.orEmpty().trim(),
            level = level ?: 0,
            xp = xp ?: 0,
            followersCount = statsObject.intValue("followersCount"),
            followingCount = statsObject.intValue("followingCount"),
            arenaWins = statsObject.intValue("arenaWins"),
            arenaLosses = statsObject.intValue("arenaLosses"),
        )
    }
}

private fun JsonObject?.intValue(key: String): Int {
    return (this?.get(key) as? JsonPrimitive)?.intOrNull?.coerceAtLeast(0) ?: 0
}

@Serializable
private data class ProfilePostRow(
    val id: String? = null,
    val texto: String? = null,
    val imagem: String? = null,
    val likes: List<String>? = null,
    val comentarios: Int? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class ProfileEventRsvpRow(
    @SerialName("eventoId") val eventoId: String? = null,
)

@Serializable
private data class ProfileEventRow(
    val id: String? = null,
    val titulo: String? = null,
    val data: String? = null,
    val local: String? = null,
    val imagem: String? = null,
)

@Serializable
private data class ProfileTrainingRsvpRow(
    @SerialName("treinoId") val treinoId: String? = null,
)

@Serializable
private data class ProfileTrainingRow(
    val id: String? = null,
    val modalidade: String? = null,
    val dia: String? = null,
    val horario: String? = null,
    val imagem: String? = null,
    val local: String? = null,
)

@Serializable
private data class ProfileLeagueMemberRow(
    @SerialName("ligaId") val ligaId: String? = null,
)

@Serializable
private data class ProfileLeagueRow(
    val id: String? = null,
    val nome: String? = null,
    val sigla: String? = null,
    val foto: String? = null,
    val logo: String? = null,
    val logoUrl: String? = null,
    val membros: Int? = null,
)

@Serializable
private data class ProfileFollowRowDto(
    val uid: String? = null,
    val nome: String? = null,
    val foto: String? = null,
    val turma: String? = null,
)

@Serializable
private data class ProfileAffinityRowDto(
    val id: String? = null,
)
