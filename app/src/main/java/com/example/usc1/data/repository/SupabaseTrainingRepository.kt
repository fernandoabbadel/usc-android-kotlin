package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.TrainingRepository
import com.example.usc1.ui.training.TrainingCheckIn
import com.example.usc1.ui.training.TrainingFrequency
import com.example.usc1.ui.training.TrainingSession
import com.example.usc1.ui.training.TrainingStatus
import com.example.usc1.ui.training.TrainingUiState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseTrainingRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : TrainingRepository {
    override suspend fun getTrainingHub(
        tenantId: String,
        userId: String,
        userName: String,
    ): TrainingUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext TrainingUiState(
                activeChallengeSubtitle = "Sessão necessária",
                activeChallengeDescription = "Entre com sua conta para carregar os treinos reais da atlética.",
            )
        }

        val client = clientProvider()
        val today = LocalDate.now(TrainingZone)
        val startDate = today.minusDays(21).toString()

        val treinoRows = client.from(TrainingsTable)
            .select(columns = Columns.raw(TrainingColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    gte("dia", startDate)
                }
                order(column = "dia", order = Order.ASCENDING)
                limit(count = MaxTrainings.toLong())
            }
            .decodeList<TrainingRow>()

        val trainingIds = treinoRows.mapNotNull { it.id.trim().takeIf(String::isNotBlank) }
        val rsvps = if (trainingIds.isEmpty()) {
            emptyList()
        } else {
            client.from(TrainingRsvpsTable)
                .select(columns = Columns.raw(RsvpColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                        isIn("treinoId", trainingIds)
                    }
                    limit(count = MaxPresenceRows.toLong())
                }
                .decodeList<TrainingRsvpRow>()
        }
        val chamada = if (trainingIds.isEmpty()) {
            emptyList()
        } else {
            client.from(TrainingChamadaTable)
                .select(columns = Columns.raw(ChamadaColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                        isIn("treinoId", trainingIds)
                    }
                    order(column = "timestamp", order = Order.DESCENDING)
                    limit(count = MaxPresenceRows.toLong())
                }
                .decodeList<TrainingChamadaRow>()
        }

        val rsvpByTraining = rsvps.associateBy { it.treinoId }
        val chamadaByTraining = chamada.associateBy { it.treinoId }
        val sessionRows = treinoRows.filterNot { it.status.orEmpty().equals("cancelado", ignoreCase = true) }
        val sessions = sessionRows.map { row ->
            mapSession(
                row = row,
                rsvp = rsvpByTraining[row.id],
                chamada = chamadaByTraining[row.id],
                today = today,
            )
        }

        val trainingById = treinoRows.associateBy { it.id }
        val history = chamada.map { row ->
            val treino = trainingById[row.treinoId]
            TrainingCheckIn(
                id = row.id.trim().ifBlank { row.treinoId.take(10).uppercase() },
                sessionTitle = treino?.modalidade.orEmpty().trim().ifBlank { "Treino" },
                userName = row.nome.orEmpty().trim().ifBlank { userName.ifBlank { "Atleta" } },
                status = row.status.toPresenceStatus(),
                qrPayload = buildTrainingPresenceQrPayload(
                    treinoId = row.treinoId,
                    tenantId = cleanTenantId,
                    userId = cleanUserId,
                    userName = userName,
                    userClass = row.turma,
                    userAvatar = row.avatar,
                ),
                createdAtLabel = formatInstant(row.timestamp.orEmpty()),
            )
        }

        val selectedSession = sessions.firstOrNull { it.status != TrainingStatus.Closed } ?: sessions.firstOrNull()
        val monthStart = today.withDayOfMonth(1)
        val monthlyTrainingCount = treinoRows.count { row ->
            row.localDate()?.let { !it.isBefore(monthStart) && !it.isAfter(today.withDayOfMonth(today.lengthOfMonth())) } == true
        }
        val monthlyPresenceCount = chamada.count { row ->
            val instant = parseInstant(row.timestamp.orEmpty())
            val date = instant?.atZone(TrainingZone)?.toLocalDate()
            row.status.toPresenceStatus() == TrainingStatus.Confirmed &&
                date != null &&
                !date.isBefore(monthStart)
        }

        TrainingUiState(
            activeChallengeTitle = "Desafio Cardume",
            activeChallengeSubtitle = "Validado por check-in",
            activeChallengeDescription = if (sessions.isEmpty()) {
                "Nenhum treino cadastrado para este período."
            } else {
                "Some presenças reais, mantenha sequência e suba no ranking da atlética."
            },
            sessions = sessions,
            checkIn = selectedSession?.let { session ->
                TrainingCheckIn(
                    id = "CHK-${session.id.take(8).uppercase()}",
                    sessionTitle = session.title,
                    userName = userName.ifBlank { "Atleta" },
                    status = session.status,
                    qrPayload = buildTrainingPresenceQrPayload(
                        treinoId = session.id,
                        tenantId = cleanTenantId,
                        userId = cleanUserId,
                        userName = userName,
                        userClass = "",
                        userAvatar = "",
                    ),
                    createdAtLabel = session.dateLabel,
                )
            } ?: TrainingUiState().checkIn,
            frequency = TrainingFrequency(
                monthLabel = formatMonth(today),
                attended = monthlyPresenceCount,
                total = monthlyTrainingCount.coerceAtLeast(monthlyPresenceCount),
                streakLabel = when (monthlyPresenceCount) {
                    0 -> "Sem presenças registradas"
                    1 -> "1 presença confirmada"
                    else -> "$monthlyPresenceCount presenças confirmadas"
                },
            ),
            history = history,
        )
    }

    private fun mapSession(
        row: TrainingRow,
        rsvp: TrainingRsvpRow?,
        chamada: TrainingChamadaRow?,
        today: LocalDate,
    ): TrainingSession {
        val date = row.localDate()
        val status = when {
            chamada?.status.toPresenceStatus() == TrainingStatus.Confirmed -> TrainingStatus.Confirmed
            rsvp?.status.toPresenceStatus() == TrainingStatus.Confirmed -> TrainingStatus.Confirmed
            row.status.orEmpty().lowercase() in setOf("encerrado", "closed", "finalizado") -> TrainingStatus.Closed
            date != null && date.isBefore(today) -> TrainingStatus.Closed
            else -> TrainingStatus.Open
        }
        val confirmedCount = row.confirmados.size
        return TrainingSession(
            id = row.id.trim(),
            title = row.modalidade.orEmpty().trim().ifBlank { "Treino" },
            modality = row.modalidade.orEmpty().trim().ifBlank { "Treino" },
            coachName = row.treinador.orEmpty().trim().ifBlank { "Atlética" },
            dateLabel = formatTrainingDate(row.dia.orEmpty(), today),
            timeLabel = row.horario.orEmpty().trim().ifBlank { "Livre" },
            location = row.local.orEmpty().trim().ifBlank { "Local a definir" },
            status = status,
            presenceLabel = when {
                confirmedCount > 0 -> "+$confirmedCount confirmados"
                status == TrainingStatus.Confirmed -> "Presença confirmada"
                status == TrainingStatus.Closed -> "Histórico fechado"
                else -> "Check-in disponível"
            },
            imageUrl = row.imagem?.trim()?.takeIf(String::isNotBlank),
        )
    }

    private fun TrainingRow.localDate(): LocalDate? {
        return runCatching { LocalDate.parse(dia.orEmpty().take(10)) }.getOrNull()
    }

    private fun String?.toPresenceStatus(): TrainingStatus {
        return when (orEmpty().trim().lowercase()) {
            "presente",
            "confirmado",
            "confirmed",
            "approved",
            "aprovado",
            "vou",
            "going" -> TrainingStatus.Confirmed
            "encerrado",
            "closed",
            "ausente",
            "cancelado" -> TrainingStatus.Closed
            else -> TrainingStatus.Open
        }
    }

    private fun formatTrainingDate(value: String, today: LocalDate): String {
        val date = runCatching { LocalDate.parse(value.take(10)) }.getOrNull() ?: return value.take(10)
        return when (date) {
            today -> "Hoje"
            today.plusDays(1) -> "Amanhã"
            else -> TrainingDayFormatter.format(date).uppercase()
        }
    }

    private fun formatMonth(value: LocalDate): String {
        val month = value.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }
        return "$month ${value.year}"
    }

    private fun formatInstant(value: String): String {
        return parseInstant(value)?.let(TrainingInstantFormatter::format) ?: value.take(16)
    }

    private fun parseInstant(value: String): Instant? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        return runCatching { OffsetDateTime.parse(clean).toInstant() }
            .getOrElse { runCatching { Instant.parse(clean) }.getOrNull() }
    }

    private fun buildTrainingPresenceQrPayload(
        treinoId: String,
        tenantId: String,
        userId: String,
        userName: String,
        userClass: String?,
        userAvatar: String?,
    ): String {
        return buildJsonObject {
            put("t", "treino-presenca")
            put("v", 1)
            put("tid", treinoId)
            put("ten", tenantId)
            put("uid", userId)
            put("n", userName.ifBlank { "Atleta" })
            put("tu", userClass.orEmpty().ifBlank { "Geral" })
            put("av", userAvatar.orEmpty())
            put("ts", System.currentTimeMillis())
        }.toString()
    }

    private companion object {
        const val TrainingsTable = "treinos"
        const val TrainingRsvpsTable = "treinos_rsvps"
        const val TrainingChamadaTable = "treinos_chamada"
        const val MaxTrainings = 160
        const val MaxPresenceRows = 240
        const val TrainingColumns =
            "id,modalidade,diaSemana,dia,horario,local,treinador,treinadorId,treinadorAvatar,descricao,imagem,ordemDia,status,confirmados,createdAt,updatedAt"
        const val RsvpColumns =
            "id,treinoId,userId,userName,userAvatar,userTurma,status,timestamp"
        const val ChamadaColumns =
            "id,treinoId,userId,nome,avatar,turma,status,origem,pagamento,timestamp,updatedAt"
    }
}

private val TrainingZone: ZoneId = ZoneId.of("America/Sao_Paulo")

private val TrainingDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.forLanguageTag("pt-BR"))

private val TrainingInstantFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM • HH:mm", Locale.forLanguageTag("pt-BR")).withZone(TrainingZone)

@Serializable
private data class TrainingRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
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
    val confirmados: List<String> = emptyList(),
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
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
private data class TrainingChamadaRow(
    val id: String = "",
    val treinoId: String = "",
    val userId: String = "",
    val nome: String? = null,
    val avatar: String? = null,
    val turma: String? = null,
    val status: String? = null,
    val origem: String? = null,
    val pagamento: String? = null,
    val timestamp: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)
