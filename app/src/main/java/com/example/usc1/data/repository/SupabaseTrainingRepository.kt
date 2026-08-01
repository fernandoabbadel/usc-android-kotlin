package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.TrainingAgendaData
import com.example.usc1.domain.model.TrainingAttendanceRecord
import com.example.usc1.domain.model.TrainingAttendanceStatus
import com.example.usc1.domain.model.TrainingCatalog
import com.example.usc1.domain.model.TrainingDetailData
import com.example.usc1.domain.model.TrainingRecordStatus
import com.example.usc1.domain.model.TrainingRsvpRecord
import com.example.usc1.domain.model.TrainingRsvpStatus
import com.example.usc1.domain.model.TrainingSessionRecord
import com.example.usc1.domain.repository.TrainingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Porta de `web-reference/src/lib/treinosNativeService.ts` para as rotas
 * `/treinos` e `/treinos/[id]`.
 */
class SupabaseTrainingRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : TrainingRepository {

    override suspend fun getMonthAgenda(
        tenantId: String,
        startDate: String,
        endDate: String,
    ): TrainingAgendaData = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val start = startDate.trim().take(10)
        val end = endDate.trim().take(10)
        if (!SupabaseClientProvider.config.isConfigured ||
            cleanTenantId.isBlank() ||
            start.isBlank() ||
            end.isBlank()
        ) {
            return@withContext TrainingAgendaData()
        }

        val client = clientProvider()
        val rows = client.from(TrainingsTable)
            .select(columns = Columns.raw(TrainingColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    gte("dia", start)
                    lte("dia", end)
                }
                order(column = "dia", order = Order.ASCENDING)
                limit(count = TrainingCatalog.MaxMonthResults.toLong())
            }
            .decodeList<TrainingRow>()

        val modalityColors = runCatching { fetchModalityColors(client, cleanTenantId) }
            .getOrElse { emptyMap() }
        val presenceCounts = runCatching {
            fetchPresenceCounts(client, cleanTenantId, rows.map { it.id })
        }.getOrElse { emptyMap() }

        TrainingAgendaData(
            sessions = rows.map { row ->
                row.toRecord(
                    presentCount = presenceCounts[row.id] ?: 0,
                    calendarColor = modalityColors[TrainingCatalog.modalityKey(row.modalidade.orEmpty())]
                        ?: TrainingCatalog.DefaultModalityColor,
                )
            },
            modalityColors = modalityColors,
        )
    }

    override suspend fun getRsvpsForTrainings(
        tenantId: String,
        trainingIds: List<String>,
    ): Map<String, List<TrainingRsvpRecord>> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val ids = trainingIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || ids.isEmpty()) {
            return@withContext emptyMap()
        }

        clientProvider().from(TrainingRsvpsTable)
            .select(columns = Columns.raw(RsvpColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    isIn("treinoId", ids)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = TrainingCatalog.MaxParticipants.toLong())
            }
            .decodeList<TrainingRsvpRow>()
            .groupBy { it.treinoId.trim() }
            .mapValues { (_, rows) -> rows.mapNotNull { it.toRecord() } }
    }

    override suspend fun getTrainingDetail(
        tenantId: String,
        trainingId: String,
    ): TrainingDetailData? = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanTrainingId = trainingId.trim()
        if (!SupabaseClientProvider.config.isConfigured ||
            cleanTenantId.isBlank() ||
            cleanTrainingId.isBlank()
        ) {
            return@withContext null
        }

        val client = clientProvider()
        val session = client.from(TrainingsTable)
            .select(columns = Columns.raw(TrainingColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", cleanTrainingId)
                }
                limit(count = 1)
            }
            .decodeList<TrainingRow>()
            .firstOrNull()
            ?: return@withContext null

        val rsvps = client.from(TrainingRsvpsTable)
            .select(columns = Columns.raw(RsvpColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("treinoId", cleanTrainingId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = TrainingCatalog.MaxParticipants.toLong())
            }
            .decodeList<TrainingRsvpRow>()

        val attendance = client.from(TrainingAttendanceTable)
            .select(columns = Columns.raw(AttendanceColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("treinoId", cleanTrainingId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = TrainingCatalog.MaxParticipants.toLong())
            }
            .decodeList<TrainingAttendanceRow>()

        val modalityColors = runCatching { fetchModalityColors(client, cleanTenantId) }
            .getOrElse { emptyMap() }

        TrainingDetailData(
            session = session.toRecord(
                presentCount = attendance.count {
                    TrainingAttendanceStatus.fromRemote(it.status) == TrainingAttendanceStatus.Presente
                },
                calendarColor = modalityColors[TrainingCatalog.modalityKey(session.modalidade.orEmpty())]
                    ?: TrainingCatalog.DefaultModalityColor,
            ),
            rsvps = rsvps.mapNotNull { it.toRecord() },
            attendance = attendance.mapNotNull { it.toRecord() },
        )
    }

    override suspend fun setRsvp(
        tenantId: String,
        trainingId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String,
        userClass: String,
        status: TrainingRsvpStatus,
    ) = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanTrainingId = trainingId.trim()
        val cleanUserId = userId.trim()
        require(cleanTenantId.isNotBlank() && cleanTrainingId.isNotBlank() && cleanUserId.isNotBlank()) {
            "Dados inválidos para confirmar presença no treino."
        }

        val client = clientProvider()
        if (status == TrainingRsvpStatus.NotGoing) {
            client.from(TrainingRsvpsTable).delete {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("treinoId", cleanTrainingId)
                    eq("userId", cleanUserId)
                }
            }
        } else {
            val existingId = client.from(TrainingRsvpsTable)
                .select(columns = Columns.raw("id")) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("treinoId", cleanTrainingId)
                        eq("userId", cleanUserId)
                    }
                    limit(count = 1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?.stringValue("id")
                .orEmpty()

            val payload = buildJsonObject {
                if (existingId.isNotBlank()) put("id", JsonPrimitive(existingId))
                put("tenant_id", JsonPrimitive(cleanTenantId))
                put("treinoId", JsonPrimitive(cleanTrainingId))
                put("userId", JsonPrimitive(cleanUserId))
                put("userName", JsonPrimitive(userName.trim().take(120).ifBlank { "Atleta" }))
                put("userAvatar", JsonPrimitive(userAvatarUrl.trim().take(2_000)))
                put("userTurma", JsonPrimitive(userClass.trim().take(30).ifBlank { "Geral" }))
                put("status", JsonPrimitive(TrainingRsvpStatus.Going.remoteValue))
                put("timestamp", JsonPrimitive(Instant.now().toString()))
            }
            client.from(TrainingRsvpsTable).upsert(payload) {
                onConflict = "treinoId,userId"
            }
        }

        refreshConfirmedCount(client, cleanTenantId, cleanTrainingId)
    }

    /** `refreshTreinoConfirmedCount`: recontagem dos `going` gravada no próprio treino. */
    private suspend fun refreshConfirmedCount(
        client: SupabaseClient,
        tenantId: String,
        trainingId: String,
    ) {
        // `count: "exact", head: true` do web: contagem sem trazer as linhas.
        val goingCount = client.from(TrainingRsvpsTable)
            .select(columns = Columns.raw("id")) {
                head = true
                count(Count.EXACT)
                filter {
                    eq("tenant_id", tenantId)
                    eq("treinoId", trainingId)
                    eq("status", TrainingRsvpStatus.Going.remoteValue)
                }
            }
            .countOrNull()
            ?.coerceAtLeast(0L)
            ?: return

        client.from(TrainingsTable).update(
            buildJsonObject {
                put("confirmedCount", JsonPrimitive(goingCount.toInt()))
                put("updatedAt", JsonPrimitive(Instant.now().toString()))
            },
        ) {
            filter {
                eq("tenant_id", tenantId)
                eq("id", trainingId)
            }
        }
    }

    /** `fetchTreinoSettings`: `settings` com id escopado por tenant. */
    private suspend fun fetchModalityColors(
        client: SupabaseClient,
        tenantId: String,
    ): Map<String, String> {
        val row = client.from(TrainingCatalog.SettingsTable)
            .select(columns = Columns.raw("id,modalidades,data")) {
                filter { eq("id", buildTenantScopedRowId(tenantId, TrainingCatalog.SettingsDocId)) }
                limit(count = 1)
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: return emptyMap()

        val allowed = (row["modalidades"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.let(TrainingCatalog::modalityKey) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

        val colors = (row["data"] as? JsonObject)?.get("modalidadeColors") as? JsonObject
            ?: return emptyMap()

        return colors.entries.mapNotNull { (rawKey, rawValue) ->
            val key = TrainingCatalog.modalityKey(rawKey)
            if (key.isBlank()) return@mapNotNull null
            if (allowed.isNotEmpty() && key !in allowed) return@mapNotNull null
            val color = (rawValue as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val normalized = if (HexColorRegex.matches(color)) color else TrainingCatalog.DefaultModalityColor
            key to normalized
        }.toMap()
    }

    /** `fetchTreinoPresenceCounts`: só as linhas `presente` da chamada. */
    private suspend fun fetchPresenceCounts(
        client: SupabaseClient,
        tenantId: String,
        trainingIds: List<String>,
    ): Map<String, Int> {
        val ids = trainingIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return client.from(TrainingAttendanceTable)
            .select(columns = Columns.raw("treinoId,status")) {
                filter {
                    eq("tenant_id", tenantId)
                    isIn("treinoId", ids)
                    eq("status", TrainingAttendanceStatus.Presente.remoteValue)
                }
                limit(count = MaxPresenceScan.toLong())
            }
            .decodeList<JsonObject>()
            .mapNotNull { it.stringValue("treinoId")?.trim()?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        return "tenant:${tenantId.trim()}::${baseId.trim()}"
    }

    private fun TrainingRow.toRecord(
        presentCount: Int,
        calendarColor: String,
    ): TrainingSessionRecord {
        return TrainingSessionRecord(
            id = id.trim(),
            modality = modalidade.orEmpty().trim().take(80).ifBlank { "Treino" },
            weekdayLabel = diaSemana.orEmpty().trim().take(40),
            date = dia.orEmpty().trim().take(10),
            time = horario.orEmpty().trim().take(20),
            location = local.orEmpty().trim().take(140),
            coachName = treinador.orEmpty().trim().take(120),
            coachId = treinadorId.orEmpty().trim().take(120),
            coachAvatarUrl = resolveRemoteImageUrl(treinadorAvatar),
            description = descricao.orEmpty().trim().take(700),
            imageUrl = resolveRemoteImageUrl(imagem),
            dayOrder = ordemDia.coerceAtLeast(0),
            status = TrainingRecordStatus.fromRemote(status),
            confirmedCount = confirmedCount.coerceAtLeast(0),
            presentCount = presentCount,
            calendarColor = calendarColor,
        )
    }

    private fun TrainingRsvpRow.toRecord(): TrainingRsvpRecord? {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) return null
        return TrainingRsvpRecord(
            userId = cleanUserId,
            userName = userName.orEmpty().trim().take(120).ifBlank { "Aluno" },
            userAvatarUrl = resolveRemoteImageUrl(userAvatar),
            userClass = userTurma.orEmpty().trim().take(30).ifBlank { "Geral" },
            status = TrainingRsvpStatus.fromRemote(status),
        )
    }

    private fun TrainingAttendanceRow.toRecord(): TrainingAttendanceRecord? {
        val cleanUserId = userId.trim().ifBlank { id.trim() }
        if (cleanUserId.isBlank()) return null
        return TrainingAttendanceRecord(
            id = id.trim().ifBlank { cleanUserId },
            userId = cleanUserId,
            name = nome.orEmpty().trim().take(120).ifBlank { "Aluno" },
            avatarUrl = resolveRemoteImageUrl(avatar),
            userClass = turma.orEmpty().trim().take(30).ifBlank { "Geral" },
            status = TrainingAttendanceStatus.fromRemote(status),
        )
    }

    private companion object {
        const val TrainingsTable = "treinos"
        const val TrainingRsvpsTable = "treinos_rsvps"
        const val TrainingAttendanceTable = "treinos_chamada"
        const val MaxPresenceScan = 5_000
        const val TrainingColumns =
            "id,modalidade,diaSemana,dia,horario,local,treinador,treinadorId,treinadorAvatar," +
                "descricao,imagem,ordemDia,status,confirmedCount"
        const val RsvpColumns = "id,treinoId,userId,userName,userAvatar,userTurma,status,timestamp"
        const val AttendanceColumns = "id,treinoId,userId,nome,avatar,turma,status,origem,timestamp"
        val HexColorRegex = Regex("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")
    }
}

private fun JsonObject.stringValue(key: String): String? {
    val element: JsonElement = this[key] ?: return null
    return (element as? JsonPrimitive)?.contentOrNull
}

@Serializable
private data class TrainingRow(
    val id: String = "",
    val modalidade: String? = null,
    val diaSemana: String? = null,
    val dia: String? = null,
    val horario: String? = null,
    val local: String? = null,
    val treinador: String? = null,
    val treinadorId: String? = null,
    val treinadorAvatar: String? = null,
    val descricao: String? = null,
    val imagem: String? = null,
    val ordemDia: Int = 0,
    val status: String? = null,
    val confirmedCount: Int = 0,
)

@Serializable
private data class TrainingRsvpRow(
    val id: String = "",
    val treinoId: String = "",
    val userId: String = "",
    val userName: String? = null,
    val userAvatar: String? = null,
    val userTurma: String? = null,
    val status: String? = null,
    val timestamp: String? = null,
)

@Serializable
private data class TrainingAttendanceRow(
    val id: String = "",
    val treinoId: String = "",
    val userId: String = "",
    val nome: String? = null,
    val avatar: String? = null,
    val turma: String? = null,
    val status: String? = null,
    val origem: String? = null,
    val timestamp: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)
