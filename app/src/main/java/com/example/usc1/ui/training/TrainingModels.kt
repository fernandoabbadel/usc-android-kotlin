package com.example.usc1.ui.training

import com.example.usc1.domain.model.TrainingCatalog
import com.example.usc1.domain.model.TrainingClassRanking
import com.example.usc1.domain.model.TrainingParticipant
import com.example.usc1.domain.model.TrainingRsvpStatus
import com.example.usc1.domain.model.TrainingSessionRecord
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** Célula do calendário mensal de `/treinos`. */
data class TrainingCalendarDay(
    val day: Int?,
    val dateIso: String = "",
    val isHoliday: Boolean = false,
    val dotColors: List<String> = emptyList(),
)

/** Card de treino da agenda, com os contadores e o social proof do web. */
data class TrainingAgendaItem(
    val session: TrainingSessionRecord,
    val confirmedCount: Int = 0,
    val presentCount: Int = 0,
    val classRanking: List<TrainingClassRanking> = emptyList(),
    val avatars: List<String> = emptyList(),
    val userStatus: TrainingRsvpStatus? = null,
) {
    val isConfirmed: Boolean get() = userStatus == TrainingRsvpStatus.Going
}

data class TrainingAgendaUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val requiresSession: Boolean = false,
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val selectedDay: Int = LocalDate.now().dayOfMonth,
    val days: List<TrainingCalendarDay> = emptyList(),
    val sessions: List<TrainingAgendaItem> = emptyList(),
    val pendingSessionId: String = "",
    val message: String? = null,
) {
    val monthLabel: String
        get() = "${monthName(month)} $year"

    val selectedDayLabel: String
        get() = "$selectedDay DE ${monthName(month).uppercase(PtBr)}"

    val selectedDateIso: String
        get() = "%04d-%02d-%02d".format(year, month, selectedDay)

    val isSelectedDayHoliday: Boolean
        get() = TrainingCatalog.isHoliday(selectedDateIso)
}

data class TrainingDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notFound: Boolean = false,
    /** Treino já encerrado e usuário sem papel de gestão/treinador. */
    val blockedByPastRule: Boolean = false,
    val session: TrainingSessionRecord? = null,
    val participants: List<TrainingParticipant> = emptyList(),
    val classRanking: List<TrainingClassRanking> = emptyList(),
    val confirmedCount: Int = 0,
    val presentCount: Int = 0,
    val userStatus: TrainingRsvpStatus? = null,
    val isSubmitting: Boolean = false,
    val presenceQrPayload: String = "",
    val showPresenceQr: Boolean = false,
    val message: String? = null,
) {
    val isConfirmed: Boolean get() = userStatus == TrainingRsvpStatus.Going
}

/** `getDaysInMonth` + preenchimento dos dias vazios antes do primeiro dia da semana. */
fun buildTrainingCalendar(
    year: Int,
    month: Int,
    sessions: List<TrainingSessionRecord>,
): List<TrainingCalendarDay> {
    val firstDay = LocalDate.of(year, month, 1)
    // `Date.getDay()` do JS: domingo = 0.
    val leading = firstDay.dayOfWeek.value % 7
    val days = ArrayList<TrainingCalendarDay>(leading + firstDay.lengthOfMonth())
    repeat(leading) { days += TrainingCalendarDay(day = null) }

    val byDate = sessions
        .filterNot { it.isCancelled }
        .groupBy { it.date }

    for (day in 1..firstDay.lengthOfMonth()) {
        val iso = "%04d-%02d-%02d".format(year, month, day)
        days += TrainingCalendarDay(
            day = day,
            dateIso = iso,
            isHoliday = TrainingCatalog.isHoliday(iso),
            dotColors = byDate[iso].orEmpty().take(3).map { it.calendarColor },
        )
    }
    return days
}

/** `treinosSelecionados`: treinos ativos do dia escolhido. */
fun List<TrainingSessionRecord>.sessionsOfDay(dateIso: String): List<TrainingSessionRecord> =
    filter { it.date == dateIso && !it.isCancelled }

internal fun monthName(month: Int): String =
    LocalDate.of(2000, month, 1).month.getDisplayName(TextStyle.FULL, PtBr)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(PtBr) else it.toString() }

/** `treino.dia.split("-").reverse()` do web. */
fun formatTrainingDate(isoDate: String): String {
    val parts = isoDate.trim().take(10).split("-")
    if (parts.size != 3) return isoDate
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

fun formatTrainingShortDate(isoDate: String): String {
    val parts = isoDate.trim().take(10).split("-")
    if (parts.size != 3) return isoDate
    return "${parts[2]}/${parts[1]}"
}
