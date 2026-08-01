package com.example.usc1.data.repository

import com.example.usc1.R
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.AlbumRepository
import com.example.usc1.ui.album.AlbumPhoto
import com.example.usc1.ui.album.AlbumRankingEntry
import com.example.usc1.ui.album.AlbumTurma
import com.example.usc1.ui.album.AlbumUiState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class SupabaseAlbumRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AlbumRepository {
    override suspend fun getAlbumHub(
        tenantId: String,
        userId: String,
        currentUserClass: String,
    ): AlbumUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext AlbumUiState(
                errorMessage = "Supabase não configurado para carregar o álbum.",
            )
        }

        val client = clientProvider()
        val configRows = fetchAppConfigRows(client, cleanTenantId)
        val uiConfig = configRows.pick(AlbumUiConfigDocId, cleanTenantId)
        val turmaConfig = configRows.pick(TurmasConfigDocId, cleanTenantId)
        val configuredTurmas = turmaConfig?.data?.jsonArray("turmas")
            ?.mapNotNull { (it as? JsonObject).toTurmaConfig() }
            ?.sortedWith(compareBy<AlbumTurma> { turmaSortWeight(it.id) }.thenBy { it.id })
            .orEmpty()
            .ifEmpty { defaultTurmas() }
            .filterNot { it.hidden }

        val users = fetchUsers(client, cleanTenantId)
        val rankings = fetchRankings(client, cleanTenantId)
        val captures = if (cleanUserId.isBlank()) emptyList() else fetchCaptures(client, cleanTenantId, cleanUserId)
        val collectedIds = captures.mapNotNull { it.targetUserId.trim().takeIf(String::isNotBlank) }.toSet()

        val membersByTurma = users
            .groupingBy { normalizeTurmaId(it.turma.orEmpty()) }
            .eachCount()
        val scoreByTurma = rankings
            .groupBy { normalizeTurmaId(it.turma) }
            .mapValues { (_, rows) -> rows.sumOf { it.totalCollected } }

        val turmas = configuredTurmas.map { turma ->
            val turmaId = normalizeTurmaId(turma.id).ifBlank { turma.id }
            turma.copy(
                id = turmaId,
                score = scoreByTurma[turmaId] ?: 0,
                members = membersByTurma[turmaId] ?: 0,
            )
        }

        val photos = users.map { user ->
            val turmaId = normalizeTurmaId(user.turma.orEmpty())
            AlbumPhoto(
                id = user.uid.trim(),
                title = user.nome.orEmpty().trim().ifBlank { "Integrante" },
                imageRes = photoDrawableForTurma(turmaId),
                imageUrl = safeImageUrl(user.foto),
                turma = turmaId,
                subtitle = user.apelido.orEmpty().trim().ifBlank { turmaId },
                collected = collectedIds.contains(user.uid.trim()),
                publicProfileUrl = user.uid.trim().takeIf(String::isNotBlank),
                bio = user.bio.orEmpty().trim(),
                origin = user.cidadeOrigem.orEmpty().trim(),
                ageLabel = albumAgeLabel(user.dataNascimento, user.idadePublica),
                relationship = if (user.relacionamentoPublico == true) {
                    user.statusRelacionamento.orEmpty().trim()
                } else {
                    ""
                },
                instagram = if (user.instagramPublico == true) {
                    user.instagram.orEmpty().trim().removePrefix("@")
                } else {
                    ""
                },
                pets = user.pets.orEmpty().trim(),
                sports = user.esportes.orEmpty().map(String::trim).filter(String::isNotBlank),
                profileVisible = user.profilePublic != false,
            )
        }

        AlbumUiState(
            title = firstNotBlank(uiConfig?.titulo, uiConfig?.data.string("titulo"), "Álbum"),
            subtitle = firstNotBlank(
                uiConfig?.subtitulo,
                uiConfig?.data.string("subtitulo"),
                "Escolha a turma para abrir somente o que você precisa",
            ),
            heroHeadline = firstNotBlank(
                uiConfig?.data.string("headline"),
                uiConfig?.data.string("heroHeadline"),
                "Escolha a turma e domine o álbum",
            ),
            heroCoverUrl = safeImageUrl(firstNotBlank(uiConfig?.capa, uiConfig?.data.string("capa"))),
            turmas = turmas,
            photos = photos,
            ranking = rankings,
            currentUserCollected = captures.size,
            isLoading = false,
            errorMessage = null,
        )
    }

    private suspend fun fetchAppConfigRows(
        client: SupabaseClient,
        tenantId: String,
    ): List<AlbumAppConfigRow> {
        val ids = listOf(
            buildTenantScopedRowId(tenantId, TurmasConfigDocId),
            buildTenantScopedRowId(tenantId, AlbumUiConfigDocId),
            TurmasConfigDocId,
            AlbumUiConfigDocId,
        ).filter(String::isNotBlank).distinct()

        return client.from(AppConfigTable)
            .select(columns = Columns.raw(AppConfigColumns)) {
                filter {
                    isIn("id", ids)
                }
                limit(count = ids.size.toLong())
            }
            .decodeList<AlbumAppConfigRow>()
    }

    private suspend fun fetchUsers(
        client: SupabaseClient,
        tenantId: String,
    ): List<AlbumUserRow> {
        return client.from(UsersTable)
            .select(columns = Columns.raw(UserColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "turma", order = Order.ASCENDING)
                limit(count = MaxUsers.toLong())
            }
            .decodeList<AlbumUserRow>()
    }

    private suspend fun fetchRankings(
        client: SupabaseClient,
        tenantId: String,
    ): List<AlbumRankingEntry> {
        return client.from(AlbumRankingsTable)
            .select(columns = Columns.raw(RankingColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "totalColetado", order = Order.DESCENDING)
                limit(count = MaxRanking.toLong())
            }
            .decodeList<AlbumRankingRow>()
            .mapNotNull { it.toEntry() }
    }

    private suspend fun fetchCaptures(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<AlbumCaptureRow> {
        return client.from(AlbumCapturesTable)
            .select(columns = Columns.raw(CaptureColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("collectorUserId", userId)
                }
                order(column = "dataColada", order = Order.DESCENDING)
                limit(count = MaxCaptures.toLong())
            }
            .decodeList<AlbumCaptureRow>()
    }

    private fun List<AlbumAppConfigRow>.pick(baseId: String, tenantId: String): AlbumAppConfigRow? {
        val scoped = buildTenantScopedRowId(tenantId, baseId)
        return firstOrNull { it.id == scoped } ?: firstOrNull { it.id == baseId }
    }

    private fun JsonObject?.jsonArray(key: String): JsonArray? = this?.get(key) as? JsonArray

    private fun JsonObject?.string(key: String): String {
        val primitive = this?.get(key) as? JsonPrimitive ?: return ""
        return primitive.contentOrNull?.trim().orEmpty()
    }

    private fun JsonObject?.boolean(key: String, fallback: Boolean = false): Boolean {
        val primitive = this?.get(key) as? JsonPrimitive ?: return fallback
        return primitive.booleanOrNull ?: fallback
    }

    private fun JsonObject?.toTurmaConfig(): AlbumTurma? {
        val id = normalizeTurmaId(string("id"))
        if (id.isBlank()) return null
        return AlbumTurma(
            id = id,
            name = firstNotBlank(string("nome"), "Turma ${id.filter(Char::isDigit).ifBlank { id }}"),
            slug = firstNotBlank(string("slug"), id.lowercase(Locale.ROOT)),
            mascot = firstNotBlank(string("mascote"), "Mascote"),
            score = 0,
            members = 0,
            coverRes = coverDrawableForTurma(id),
            photoRes = photoDrawableForTurma(id),
            coverUrl = safeImageUrl(string("capa")),
            logoUrl = safeImageUrl(string("logo")),
            hidden = boolean("hidden"),
        )
    }

    private fun AlbumRankingRow.toEntry(): AlbumRankingEntry? {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return null
        return AlbumRankingEntry(
            id = id.trim().ifBlank { cleanUserId },
            userId = cleanUserId,
            name = nome.trim().ifBlank { "Integrante" },
            photoUrl = safeImageUrl(foto),
            turma = normalizeTurmaId(turma),
            totalCollected = totalColetado.coerceAtLeast(0),
            scansT8 = scansT8.coerceAtLeast(0),
        )
    }

    private fun defaultTurmas(): List<AlbumTurma> = listOf(
        turma("T1", "Turma I", "Jacaré"),
        turma("T2", "Turma II", "Cavalo Marinho"),
        turma("T3", "Turma III", "Tartaruga"),
        turma("T4", "Turma IV", "Baleia"),
        turma("T5", "Turma V", "Pinguim"),
        turma("T6", "Turma VI", "Lagosta"),
        turma("T7", "Turma VII", "Urso Polar"),
        turma("T8", "Turma VIII", "Calouros"),
        turma("T9", "Turma IX", "Cardume"),
    )

    private fun turma(id: String, name: String, mascot: String): AlbumTurma {
        return AlbumTurma(
            id = id,
            name = name,
            slug = id.lowercase(Locale.ROOT),
            mascot = mascot,
            score = 0,
            members = 0,
            coverRes = coverDrawableForTurma(id),
            photoRes = photoDrawableForTurma(id),
        )
    }

    private fun normalizeTurmaId(value: String): String {
        val clean = value.trim().uppercase(Locale.ROOT)
        if (clean.isBlank()) return ""
        if (Regex("^T\\d{1,3}$").matches(clean)) {
            return "T${clean.drop(1).toIntOrNull() ?: clean.drop(1)}"
        }
        val digits = clean.filter(Char::isDigit)
        return if (digits.isBlank()) clean else "T${digits.toIntOrNull() ?: digits}"
    }

    private fun turmaSortWeight(turma: AlbumTurma): Int = turmaSortWeight(turma.id)

    private fun turmaSortWeight(turmaId: String): Int = turmaId.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE

    private fun coverDrawableForTurma(id: String): Int {
        return when (normalizeTurmaId(id)) {
            "T1" -> R.drawable.capa_t1
            "T2" -> R.drawable.capa_t2
            "T3" -> R.drawable.capa_t3
            "T4" -> R.drawable.capa_t4
            "T5" -> R.drawable.capa_t5
            "T6" -> R.drawable.capa_t6
            "T7" -> R.drawable.capa_t7
            "T8" -> R.drawable.capa_t8
            "T9" -> R.drawable.capa_t9
            else -> R.drawable.capa_t8
        }
    }

    private fun photoDrawableForTurma(id: String): Int {
        return when (normalizeTurmaId(id)) {
            "T1" -> R.drawable.turma1
            "T2" -> R.drawable.turma2
            "T3" -> R.drawable.turma3
            "T4" -> R.drawable.turma4
            "T5" -> R.drawable.turma5
            "T6" -> R.drawable.turma6
            "T7" -> R.drawable.turma7
            "T8" -> R.drawable.turma8
            "T9" -> R.drawable.turma9
            else -> R.drawable.turma8
        }
    }

    private fun safeImageUrl(value: String?): String? {
        return resolveRemoteImageUrl(value)
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        val cleanTenantId = tenantId.trim()
        val cleanBaseId = baseId.trim()
        return if (cleanTenantId.isBlank()) cleanBaseId else "tenant:$cleanTenantId::$cleanBaseId"
    }

    private companion object {
        const val AppConfigTable = "app_config"
        const val UsersTable = "users"
        const val AlbumRankingsTable = "album_rankings"
        const val AlbumCapturesTable = "album_captures"
        const val TurmasConfigDocId = "turmas_config"
        const val AlbumUiConfigDocId = "album_ui"
        const val MaxUsers = 500
        const val MaxRanking = 100
        const val MaxCaptures = 500
        const val AppConfigColumns = "id,capa,titulo,subtitulo,data"
        const val UserColumns =
            "uid,nome,apelido,turma,foto,bio,instagram,instagramPublico,cidadeOrigem," +
                "statusRelacionamento,relacionamentoPublico,dataNascimento,idadePublica," +
                "pets,esportes,profile_public,tenant_id"
        const val RankingColumns = "id,userId,nome,foto,turma,totalColetado,scansT8,tenant_id"
        const val CaptureColumns = "id,targetUserId,nome,turma,dataColada,tenant_id"
    }
}

/** Espelha `calcularIdade` + `idadePublica` do `/album/[turmaId]` do web. */
private fun albumAgeLabel(birthDate: String?, agePublic: Boolean?): String {
    if (agePublic == false) return "?? anos"
    val clean = birthDate?.trim().orEmpty()
    if (clean.isBlank()) return ""
    val parsed = runCatching { java.time.LocalDate.parse(clean.take(10)) }
        .getOrElse {
            runCatching {
                val parts = clean.split("/")
                if (parts.size != 3) return@runCatching null
                java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }.getOrNull()
        } ?: return ""
    val today = java.time.LocalDate.now()
    if (parsed.isAfter(today)) return ""
    val years = java.time.Period.between(parsed, today).years
    return if (years in 0..130) "$years anos" else ""
}

@Serializable
private data class AlbumAppConfigRow(
    val id: String = "",
    val capa: String? = null,
    val titulo: String? = null,
    val subtitulo: String? = null,
    val data: JsonObject? = null,
)

@Serializable
private data class AlbumUserRow(
    val uid: String = "",
    val nome: String? = null,
    val apelido: String? = null,
    val turma: String? = null,
    val foto: String? = null,
    val bio: String? = null,
    val instagram: String? = null,
    val instagramPublico: Boolean? = null,
    val cidadeOrigem: String? = null,
    val statusRelacionamento: String? = null,
    val relacionamentoPublico: Boolean? = null,
    val dataNascimento: String? = null,
    val idadePublica: Boolean? = null,
    val pets: String? = null,
    val esportes: List<String>? = null,
    @SerialName("profile_public") val profilePublic: Boolean? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class AlbumRankingRow(
    val id: String = "",
    val userId: String = "",
    val nome: String = "",
    val foto: String = "",
    val turma: String = "",
    val totalColetado: Int = 0,
    val scansT8: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class AlbumCaptureRow(
    val id: String = "",
    val targetUserId: String = "",
    val nome: String? = null,
    val turma: String? = null,
    val dataColada: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
)
