package com.example.usc1.domain.model

import com.example.usc1.core.roles.UserRole
import com.example.usc1.ui.training.buildTrainingCalendar
import com.example.usc1.ui.training.sessionsOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras copiadas de `/treinos` e `/treinos/[id]` do web. */
class TrainingM5RulesTest {

    @Test
    fun `chamada oficial sobrescreve o status de quem confirmou`() {
        val detail = TrainingDetailData(
            session = session(),
            rsvps = listOf(
                rsvp("u1", "Ana", "T1"),
                rsvp("u2", "Bruno", "T2"),
                rsvp("u3", "Carla", "T1"),
            ),
            attendance = listOf(
                attendance("u1", TrainingAttendanceStatus.Presente),
                attendance("u2", TrainingAttendanceStatus.Falta),
            ),
        )

        val participants = detail.participants()

        assertEquals(listOf("Ana", "Bruno", "Carla"), participants.map { it.name })
        assertEquals(TrainingParticipantStatus.Presente, participants[0].status)
        assertEquals(TrainingParticipantStatus.Falta, participants[1].status)
        assertEquals(TrainingParticipantStatus.Confirmado, participants[2].status)
    }

    @Test
    fun `chamada manual sem rsvp nao entra na lista da pagina publica`() {
        val detail = TrainingDetailData(
            session = session(),
            rsvps = listOf(rsvp("u1", "Ana", "T1")),
            attendance = listOf(attendance("u9", TrainingAttendanceStatus.Presente)),
        )

        assertEquals(listOf("u1"), detail.participants().map { it.userId })
    }

    @Test
    fun `quem nao vai fica de fora da lista final`() {
        val detail = TrainingDetailData(
            session = session(),
            rsvps = listOf(
                rsvp("u1", "Ana", "T1"),
                rsvp("u2", "Bruno", "T2", TrainingRsvpStatus.NotGoing),
            ),
        )

        assertEquals(listOf("u1"), detail.participants().map { it.userId })
    }

    @Test
    fun `ranking de turmas ignora faltas e fica no top 3`() {
        val participants = listOf(
            participant("u1", "T1", TrainingParticipantStatus.Presente),
            participant("u2", "T1", TrainingParticipantStatus.Confirmado),
            participant("u3", "T2", TrainingParticipantStatus.Presente),
            participant("u4", "T3", TrainingParticipantStatus.Confirmado),
            participant("u5", "T4", TrainingParticipantStatus.Falta),
        )

        val ranking = participants.classRanking()

        assertEquals(3, ranking.size)
        assertEquals(TrainingClassRanking("T1", 2), ranking.first())
        assertFalse(ranking.any { it.className == "T4" })
    }

    @Test
    fun `calendario preenche os dias antes do primeiro dia da semana`() {
        // 01/07/2026 cai numa quarta-feira: 3 celulas vazias antes do dia 1.
        val days = buildTrainingCalendar(2026, 7, emptyList())

        assertEquals(3 + 31, days.size)
        assertTrue(days.take(3).all { it.day == null })
        assertEquals(1, days[3].day)
        assertEquals("2026-07-01", days[3].dateIso)
    }

    @Test
    fun `feriado da lista do web aparece marcado no calendario`() {
        val days = buildTrainingCalendar(2026, 7, emptyList())
        val holiday = days.first { it.dateIso == "2026-07-09" }
        val regularDay = days.first { it.dateIso == "2026-07-10" }

        assertTrue(holiday.isHoliday)
        assertFalse(regularDay.isHoliday)
    }

    @Test
    fun `calendario usa a cor da modalidade e ignora treino cancelado`() {
        val sessions = listOf(
            session(id = "a", date = "2026-07-08", color = "#FACC15"),
            session(id = "b", date = "2026-07-08", color = "#3B82F6"),
            session(
                id = "c",
                date = "2026-07-08",
                color = "#EF4444",
                status = TrainingRecordStatus.Cancelado,
            ),
        )

        val day = buildTrainingCalendar(2026, 7, sessions).first { it.dateIso == "2026-07-08" }

        assertEquals(listOf("#FACC15", "#3B82F6"), day.dotColors)
    }

    @Test
    fun `lista do dia ignora cancelados e datas diferentes`() {
        val sessions = listOf(
            session(id = "a", date = "2026-07-08"),
            session(id = "b", date = "2026-07-09"),
            session(id = "c", date = "2026-07-08", status = TrainingRecordStatus.Cancelado),
        )

        assertEquals(listOf("a"), sessions.sessionsOfDay("2026-07-08").map { it.id })
    }

    @Test
    fun `treino encerrado so abre para comissao tecnica`() {
        assertTrue(TrainingCatalog.canOpenPastTraining(UserRole.Treinador))
        assertTrue(TrainingCatalog.canOpenPastTraining(UserRole.AdminTreino))
        assertTrue(TrainingCatalog.canOpenPastTraining(UserRole.Master))
        assertFalse(TrainingCatalog.canOpenPastTraining(UserRole.User))
        assertFalse(TrainingCatalog.canOpenPastTraining(UserRole.Visitante))
    }

    @Test
    fun `chave da modalidade normaliza espacos e caixa`() {
        assertEquals("volei de praia", TrainingCatalog.modalityKey("  Volei   DE Praia "))
    }

    @Test
    fun `status remoto desconhecido cai no padrao do web`() {
        assertEquals(TrainingRecordStatus.Ativo, TrainingRecordStatus.fromRemote("qualquer"))
        assertEquals(TrainingRecordStatus.Cancelado, TrainingRecordStatus.fromRemote("cancelado"))
        assertEquals(TrainingRsvpStatus.Going, TrainingRsvpStatus.fromRemote(null))
        assertEquals(TrainingRsvpStatus.NotGoing, TrainingRsvpStatus.fromRemote("not_going"))
        assertEquals(TrainingAttendanceStatus.Presente, TrainingAttendanceStatus.fromRemote(""))
        assertNull(TrainingAttendanceStatus.entries.firstOrNull { it.remoteValue == "ausente" })
    }

    private fun session(
        id: String = "t1",
        date: String = "2026-07-08",
        color: String = TrainingCatalog.DefaultModalityColor,
        status: TrainingRecordStatus = TrainingRecordStatus.Ativo,
    ) = TrainingSessionRecord(
        id = id,
        modality = "Futsal",
        date = date,
        status = status,
        calendarColor = color,
    )

    private fun rsvp(
        userId: String,
        name: String,
        userClass: String,
        status: TrainingRsvpStatus = TrainingRsvpStatus.Going,
    ) = TrainingRsvpRecord(
        userId = userId,
        userName = name,
        userClass = userClass,
        status = status,
    )

    private fun attendance(
        userId: String,
        status: TrainingAttendanceStatus,
    ) = TrainingAttendanceRecord(
        id = "chamada:$userId",
        userId = userId,
        name = "Aluno",
        status = status,
    )

    private fun participant(
        userId: String,
        userClass: String,
        status: TrainingParticipantStatus,
    ) = TrainingParticipant(
        userId = userId,
        name = userId,
        userClass = userClass,
        avatarUrl = null,
        status = status,
    )
}
