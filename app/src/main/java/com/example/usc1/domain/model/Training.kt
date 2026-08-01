package com.example.usc1.domain.model

import com.example.usc1.core.roles.UserRole

/**
 * Espelha `TreinoRecord`, `TreinoRsvpRecord` e `TreinoChamadaRecord` de
 * `web-reference/src/lib/treinosNativeService.ts`, usados por `/treinos` e `/treinos/[id]`.
 */
object TrainingCatalog {
    const val SettingsTable = "settings"
    const val SettingsDocId = "treinos"
    const val DefaultModalityColor = "#10B981"

    /** `MAX_MONTH_RESULTS` do serviço do web. */
    const val MaxMonthResults = 220

    /** `maxResults: 220` usado pela página de detalhe. */
    const val MaxParticipants = 220

    /**
     * `FERIADOS` fixos da página `/treinos` do web (calendário UNITAU 2026).
     * O web mantém a lista no próprio arquivo da rota, sem tabela no Supabase.
     */
    val Holidays: Set<String> = setOf(
        "2026-02-16", "2026-02-17", "2026-02-18",
        "2026-04-03", "2026-04-21",
        "2026-05-01", "2026-06-04",
        "2026-06-13", "2026-07-09",
        "2026-09-07", "2026-10-12",
        "2026-10-28", "2026-11-02", "2026-11-15", "2026-11-20",
        "2026-12-25",
    )

    /** `TREINO_HISTORICO_ALLOWED_ROLES` de `/treinos/[id]`. */
    val PastTrainingRoles: Set<UserRole> = setOf(
        UserRole.Master,
        UserRole.AdminGeral,
        UserRole.AdminGestor,
        UserRole.AdminTreino,
        UserRole.Treinador,
    )

    fun modalityKey(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()

    fun isHoliday(isoDate: String): Boolean = Holidays.contains(isoDate.trim().take(10))

    fun canOpenPastTraining(role: UserRole): Boolean = role in PastTrainingRoles
}

enum class TrainingRecordStatus(val remoteValue: String) {
    Ativo("ativo"),
    Cancelado("cancelado");

    companion object {
        fun fromRemote(value: String?): TrainingRecordStatus =
            if (value?.trim()?.lowercase() == Cancelado.remoteValue) Cancelado else Ativo
    }
}

enum class TrainingRsvpStatus(val remoteValue: String) {
    Going("going"),
    NotGoing("not_going");

    companion object {
        fun fromRemote(value: String?): TrainingRsvpStatus =
            if (value?.trim()?.lowercase() == NotGoing.remoteValue) NotGoing else Going
    }
}

enum class TrainingAttendanceStatus(val remoteValue: String) {
    Presente("presente"),
    Falta("falta"),
    Justificado("justificado"),
    Inscrito("inscrito");

    companion object {
        fun fromRemote(value: String?): TrainingAttendanceStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Presente
        }
    }
}

data class TrainingSessionRecord(
    val id: String,
    val modality: String,
    val weekdayLabel: String = "",
    /** `dia` no formato `yyyy-MM-dd`. */
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val coachName: String = "",
    val coachId: String = "",
    val coachAvatarUrl: String? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val dayOrder: Int = 0,
    val status: TrainingRecordStatus = TrainingRecordStatus.Ativo,
    val confirmedCount: Int = 0,
    val presentCount: Int = 0,
    val calendarColor: String = TrainingCatalog.DefaultModalityColor,
) {
    val isCancelled: Boolean get() = status == TrainingRecordStatus.Cancelado
}

data class TrainingRsvpRecord(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val userClass: String = "Geral",
    val status: TrainingRsvpStatus = TrainingRsvpStatus.Going,
)

data class TrainingAttendanceRecord(
    val id: String,
    val userId: String,
    val name: String,
    val avatarUrl: String? = null,
    val userClass: String = "Geral",
    val status: TrainingAttendanceStatus = TrainingAttendanceStatus.Presente,
)

/** Resultado de `fetchTreinosByDateRange` + `fetchTreinoSettings` + `fetchTreinoPresenceCounts`. */
data class TrainingAgendaData(
    val sessions: List<TrainingSessionRecord> = emptyList(),
    val modalityColors: Map<String, String> = emptyMap(),
)

/** Resultado de `fetchTreinoById` + `fetchTreinoRsvps` + `fetchTreinoChamada`. */
data class TrainingDetailData(
    val session: TrainingSessionRecord,
    val rsvps: List<TrainingRsvpRecord> = emptyList(),
    val attendance: List<TrainingAttendanceRecord> = emptyList(),
)

enum class TrainingParticipantStatus {
    Confirmado,
    Presente,
    Falta,
}

data class TrainingParticipant(
    val userId: String,
    val name: String,
    val userClass: String,
    val avatarUrl: String?,
    val status: TrainingParticipantStatus,
)

data class TrainingClassRanking(
    val className: String,
    val count: Int,
)

/**
 * `listaFinal` de `/treinos/[id]`: parte de quem confirmou (`going`) e sobrescreve
 * com a chamada oficial do admin.
 */
fun TrainingDetailData.participants(): List<TrainingParticipant> {
    val merged = LinkedHashMap<String, TrainingParticipant>()
    rsvps.filter { it.status == TrainingRsvpStatus.Going }.forEach { rsvp ->
        merged[rsvp.userId] = TrainingParticipant(
            userId = rsvp.userId,
            name = rsvp.userName,
            userClass = rsvp.userClass,
            avatarUrl = rsvp.userAvatarUrl,
            status = TrainingParticipantStatus.Confirmado,
        )
    }
    attendance.forEach { entry ->
        val existing = merged[entry.userId] ?: return@forEach
        merged[entry.userId] = existing.copy(
            status = if (entry.status == TrainingAttendanceStatus.Falta) {
                TrainingParticipantStatus.Falta
            } else {
                TrainingParticipantStatus.Presente
            },
        )
    }
    return merged.values.sortedBy { it.name.lowercase() }
}

/** `rankingTurmas`: top 3 turmas entre quem não faltou. */
fun List<TrainingParticipant>.classRanking(limit: Int = 3): List<TrainingClassRanking> =
    filter { it.status != TrainingParticipantStatus.Falta && it.userClass.isNotBlank() }
        .groupingBy { it.userClass.uppercase() }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { TrainingClassRanking(className = it.key, count = it.value) }
