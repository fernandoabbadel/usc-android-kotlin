package com.example.usc1.ui.training

import com.example.usc1.R

enum class TrainingStatus(val label: String) {
    Open("Check-in aberto"),
    Confirmed("Confirmado"),
    Closed("Encerrado"),
}

data class TrainingSession(
    val id: String,
    val title: String,
    val modality: String,
    val coachName: String,
    val dateLabel: String,
    val timeLabel: String,
    val location: String,
    val status: TrainingStatus,
    val presenceLabel: String,
    val imageRes: Int,
)

data class TrainingCheckIn(
    val id: String,
    val sessionTitle: String,
    val userName: String,
    val status: TrainingStatus,
    val qrPayload: String,
    val createdAtLabel: String,
)

data class TrainingFrequency(
    val monthLabel: String,
    val attended: Int,
    val total: Int,
    val streakLabel: String,
)

data class TrainingUiState(
    val activeChallengeTitle: String = "Desafio Cardume",
    val activeChallengeSubtitle: String = "Validado por check-in",
    val activeChallengeDescription: String = "Some presenças, mantenha sequência e suba no ranking da atlética.",
    val sessions: List<TrainingSession> = TrainingMockData.sessions,
    val checkIn: TrainingCheckIn = TrainingMockData.checkIn,
    val frequency: TrainingFrequency = TrainingMockData.frequency,
    val history: List<TrainingCheckIn> = TrainingMockData.history,
)

object TrainingMockData {
    val sessions = listOf(
        TrainingSession(
            id = "futsal-01",
            title = "Treino Futsal Masculino",
            modality = "Futsal",
            coachName = "Capitão Lucas",
            dateLabel = "Hoje",
            timeLabel = "20:00",
            location = "Ginásio USC",
            status = TrainingStatus.Open,
            presenceLabel = "+38 confirmados",
            imageRes = R.drawable.battle_forest,
        ),
        TrainingSession(
            id = "volei-02",
            title = "Vôlei Interturmas",
            modality = "Vôlei",
            coachName = "Comissão Atlética",
            dateLabel = "Qua, 08 JUL",
            timeLabel = "18:30",
            location = "Quadra 2",
            status = TrainingStatus.Confirmed,
            presenceLabel = "+21 confirmados",
            imageRes = R.drawable.logo_usc_wide,
        ),
        TrainingSession(
            id = "gym-03",
            title = "Check-in Gym",
            modality = "Academia",
            coachName = "USC Gym",
            dateLabel = "Sex, 10 JUL",
            timeLabel = "Livre",
            location = "Academia parceira",
            status = TrainingStatus.Closed,
            presenceLabel = "Histórico fechado",
            imageRes = R.drawable.carteirinha_bg,
        ),
    )

    val checkIn = TrainingCheckIn(
        id = "CHK-5021",
        sessionTitle = sessions.first().title,
        userName = "Fernando USC",
        status = TrainingStatus.Open,
        qrPayload = "USC-GYM-CHK-5021",
        createdAtLabel = "Hoje • 19:52",
    )

    val frequency = TrainingFrequency(
        monthLabel = "Julho 2026",
        attended = 9,
        total = 12,
        streakLabel = "5 treinos seguidos",
    )

    val history = listOf(
        checkIn.copy(status = TrainingStatus.Confirmed, createdAtLabel = "Ontem • 20:03"),
        checkIn.copy(id = "CHK-4980", sessionTitle = "Vôlei Interturmas", status = TrainingStatus.Confirmed, createdAtLabel = "03 JUL • 18:44"),
        checkIn.copy(id = "CHK-4912", sessionTitle = "Check-in Gym", status = TrainingStatus.Closed, createdAtLabel = "01 JUL • 07:20"),
    )

    fun sessionById(id: String): TrainingSession =
        sessions.firstOrNull { it.id == id } ?: sessions.first()
}
