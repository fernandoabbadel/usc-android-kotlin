package com.example.usc1.domain.repository

import com.example.usc1.domain.model.TrainingAgendaData
import com.example.usc1.domain.model.TrainingDetailData
import com.example.usc1.domain.model.TrainingRsvpRecord
import com.example.usc1.domain.model.TrainingRsvpStatus

interface TrainingRepository {
    /** `fetchTreinosByDateRange` + `fetchTreinoSettings` + `fetchTreinoPresenceCounts` do web. */
    suspend fun getMonthAgenda(
        tenantId: String,
        startDate: String,
        endDate: String,
    ): TrainingAgendaData

    /**
     * `fetchTreinoRsvps` dos treinos visíveis. O web faz uma chamada por card;
     * aqui os treinos do dia selecionado são lidos numa consulta só.
     */
    suspend fun getRsvpsForTrainings(
        tenantId: String,
        trainingIds: List<String>,
    ): Map<String, List<TrainingRsvpRecord>>

    /** `fetchTreinoById` + `fetchTreinoRsvps` + `fetchTreinoChamada` do web. */
    suspend fun getTrainingDetail(
        tenantId: String,
        trainingId: String,
    ): TrainingDetailData?

    /** `setTreinoRsvp` do web: grava em `treinos_rsvps` e recalcula `confirmedCount`. */
    suspend fun setRsvp(
        tenantId: String,
        trainingId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String,
        userClass: String,
        status: TrainingRsvpStatus,
    )
}
