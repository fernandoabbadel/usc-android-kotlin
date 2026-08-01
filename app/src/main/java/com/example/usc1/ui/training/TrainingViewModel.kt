package com.example.usc1.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseTrainingRepository
import com.example.usc1.domain.model.TrainingAgendaData
import com.example.usc1.domain.model.TrainingCatalog
import com.example.usc1.domain.model.TrainingRsvpStatus
import com.example.usc1.domain.model.TrainingSessionRecord
import com.example.usc1.domain.model.classRanking
import com.example.usc1.domain.model.participants
import com.example.usc1.domain.repository.TrainingRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Agenda mensal de `/treinos`. */
class TrainingAgendaViewModel(
    private val repository: TrainingRepository = SupabaseTrainingRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingAgendaUiState())
    val uiState: StateFlow<TrainingAgendaUiState> = _uiState.asStateFlow()

    private var monthSessions: List<TrainingSessionRecord> = emptyList()
    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = TrainingAgendaUiState(requiresSession = true)
            return
        }

        val state = _uiState.value
        val key = "$tenantId::$userId::${state.year}-${state.month}"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        loadMonth(tenantId, userId, state.year, state.month, state.selectedDay)
    }

    fun selectDay(session: UserSession, day: Int) {
        val current = _uiState.value
        if (day == current.selectedDay) return
        _uiState.update { it.copy(selectedDay = day, message = null) }
        refreshSelectedDay(session)
    }

    fun previousMonth(session: UserSession) = shiftMonth(session, -1)

    fun nextMonth(session: UserSession) = shiftMonth(session, 1)

    private fun shiftMonth(session: UserSession, delta: Int) {
        val current = _uiState.value
        val target = LocalDate.of(current.year, current.month, 1).plusMonths(delta.toLong())
        _uiState.update {
            it.copy(
                year = target.year,
                month = target.monthValue,
                selectedDay = 1,
                sessions = emptyList(),
                message = null,
            )
        }
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank()) return
        lastLoadKey = "$tenantId::$userId::${target.year}-${target.monthValue}"
        loadMonth(tenantId, userId, target.year, target.monthValue, 1)
    }

    fun setRsvp(session: UserSession, trainingId: String, status: TrainingRsvpStatus) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank()) {
            _uiState.update { it.copy(message = "Faça login para confirmar presença.") }
            return
        }
        if (_uiState.value.pendingSessionId.isNotBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(pendingSessionId = trainingId, message = null) }
            runCatching {
                repository.setRsvp(
                    tenantId = tenantId,
                    trainingId = trainingId,
                    userId = userId,
                    userName = user?.name.orEmpty(),
                    userAvatarUrl = user?.avatarUrl.orEmpty(),
                    userClass = user?.classCode.orEmpty(),
                    status = status,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        pendingSessionId = "",
                        message = if (status == TrainingRsvpStatus.Going) {
                            "Bora treinar!"
                        } else {
                            "Inscrição cancelada."
                        },
                    )
                }
                loadMonth(
                    tenantId = tenantId,
                    userId = userId,
                    year = _uiState.value.year,
                    month = _uiState.value.month,
                    selectedDay = _uiState.value.selectedDay,
                    keepMessage = true,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingSessionId = "",
                        message = error.message.orEmpty().ifBlank { "Erro ao atualizar." },
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadMonth(
        tenantId: String,
        userId: String,
        year: Int,
        month: Int,
        selectedDay: Int,
        keepMessage: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    requiresSession = false,
                    message = if (keepMessage) it.message else null,
                )
            }

            val firstDay = LocalDate.of(year, month, 1)
            runCatching {
                repository.getMonthAgenda(
                    tenantId = tenantId,
                    startDate = firstDay.toString(),
                    endDate = firstDay.withDayOfMonth(firstDay.lengthOfMonth()).toString(),
                )
            }.onSuccess { agenda ->
                monthSessions = agenda.sessions
                val safeDay = selectedDay.coerceIn(1, firstDay.lengthOfMonth())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        year = year,
                        month = month,
                        selectedDay = safeDay,
                        days = buildTrainingCalendar(year, month, agenda.sessions),
                    )
                }
                loadDaySessions(tenantId, userId, agenda)
            }.onFailure { error ->
                monthSessions = emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        days = buildTrainingCalendar(year, month, emptyList()),
                        sessions = emptyList(),
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar a agenda de treinos."
                        },
                    )
                }
            }
        }
    }

    private fun refreshSelectedDay(session: UserSession) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            loadDaySessions(tenantId, userId, TrainingAgendaData(sessions = monthSessions))
        }
    }

    private suspend fun loadDaySessions(
        tenantId: String,
        userId: String,
        agenda: TrainingAgendaData,
    ) {
        val daySessions = agenda.sessions.sessionsOfDay(_uiState.value.selectedDateIso)
        if (daySessions.isEmpty()) {
            _uiState.update { it.copy(sessions = emptyList()) }
            return
        }

        val rsvpsByTraining = runCatching {
            repository.getRsvpsForTrainings(tenantId, daySessions.map { it.id })
        }.getOrElse { emptyMap() }

        _uiState.update { state ->
            state.copy(
                sessions = daySessions.map { training ->
                    val rows = rsvpsByTraining[training.id].orEmpty()
                    val going = rows.filter { it.status == TrainingRsvpStatus.Going }
                    TrainingAgendaItem(
                        session = training,
                        confirmedCount = if (rows.isEmpty()) training.confirmedCount else going.size,
                        presentCount = training.presentCount,
                        classRanking = going
                            .filter { it.userClass.isNotBlank() }
                            .groupingBy { it.userClass.uppercase() }
                            .eachCount()
                            .entries
                            .sortedByDescending { it.value }
                            .take(3)
                            .map {
                                com.example.usc1.domain.model.TrainingClassRanking(it.key, it.value)
                            },
                        avatars = going.mapNotNull { it.userAvatarUrl }.take(4),
                        userStatus = rows.firstOrNull { it.userId == userId }?.status,
                    )
                },
            )
        }
    }
}

/** Detalhe de `/treinos/[id]`. */
class TrainingDetailViewModel(
    private val repository: TrainingRepository = SupabaseTrainingRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingDetailUiState(isLoading = true))
    val uiState: StateFlow<TrainingDetailUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, trainingId: String, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val cleanTrainingId = trainingId.trim()
        if (tenantId.isBlank() || cleanTrainingId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = TrainingDetailUiState(
                isLoading = false,
                notFound = true,
                errorMessage = "Treino não encontrado.",
            )
            return
        }

        val key = "$tenantId::$userId::$cleanTrainingId"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, message = null) }
            runCatching { repository.getTrainingDetail(tenantId, cleanTrainingId) }
                .onSuccess { detail ->
                    if (detail == null) {
                        _uiState.value = TrainingDetailUiState(
                            isLoading = false,
                            notFound = true,
                            errorMessage = "Treino não encontrado.",
                        )
                        return@onSuccess
                    }

                    val role = user?.role
                    val isPast = isTrainingPast(detail.session.date)
                    if (isPast && (role == null || !TrainingCatalog.canOpenPastTraining(role))) {
                        _uiState.value = TrainingDetailUiState(
                            isLoading = false,
                            blockedByPastRule = true,
                            session = detail.session,
                        )
                        return@onSuccess
                    }

                    val participants = detail.participants()
                    _uiState.value = TrainingDetailUiState(
                        isLoading = false,
                        session = detail.session,
                        participants = participants,
                        classRanking = participants.classRanking(),
                        confirmedCount = detail.rsvps.count { it.status == TrainingRsvpStatus.Going },
                        presentCount = detail.attendance.count {
                            it.status == com.example.usc1.domain.model.TrainingAttendanceStatus.Presente
                        },
                        userStatus = detail.rsvps.firstOrNull { it.userId == userId }?.status,
                        presenceQrPayload = if (userId.isBlank()) {
                            ""
                        } else {
                            buildPresenceQrPayload(
                                trainingId = detail.session.id,
                                tenantId = tenantId,
                                userId = userId,
                                userName = user?.name.orEmpty(),
                                userClass = user?.classCode.orEmpty(),
                                userAvatar = user?.avatarUrl.orEmpty(),
                            )
                        },
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.orEmpty().ifBlank {
                                "Erro ao carregar treino."
                            },
                        )
                    }
                }
        }
    }

    fun setRsvp(session: UserSession, status: TrainingRsvpStatus) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val trainingId = _uiState.value.session?.id.orEmpty()
        if (tenantId.isBlank() || userId.isBlank() || trainingId.isBlank()) {
            _uiState.update { it.copy(message = "Faça login para confirmar.") }
            return
        }
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            runCatching {
                repository.setRsvp(
                    tenantId = tenantId,
                    trainingId = trainingId,
                    userId = userId,
                    userName = user?.name.orEmpty(),
                    userAvatarUrl = user?.avatarUrl.orEmpty(),
                    userClass = user?.classCode.orEmpty(),
                    status = status,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSubmitting = false) }
                lastLoadKey = ""
                load(session, trainingId, forceRefresh = true)
                _uiState.update {
                    it.copy(
                        message = if (status == TrainingRsvpStatus.Going) {
                            "Presença confirmada."
                        } else {
                            "Inscrição removida."
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        message = error.message.orEmpty().ifBlank { "Erro ao atualizar." },
                    )
                }
            }
        }
    }

    fun setPresenceQrVisible(visible: Boolean) {
        if (visible && _uiState.value.presenceQrPayload.isBlank()) {
            _uiState.update { it.copy(message = "Faça login para abrir seu QR de presença.") }
            return
        }
        _uiState.update { it.copy(showPresenceQr = visible) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun isTrainingPast(isoDate: String): Boolean {
        val date = runCatching { LocalDate.parse(isoDate.trim().take(10)) }.getOrNull() ?: return false
        return date.isBefore(LocalDate.now())
    }
}

/** `buildTreinoPresenceQrPayload` de `web-reference/src/lib/qrPayloads.ts`. */
internal fun buildPresenceQrPayload(
    trainingId: String,
    tenantId: String,
    userId: String,
    userName: String,
    userClass: String,
    userAvatar: String,
): String = buildJsonObject {
    put("t", "treino-presenca")
    put("v", 1)
    put("tid", trainingId)
    put("ten", tenantId)
    put("uid", userId)
    put("n", userName.ifBlank { "Atleta" })
    put("tu", userClass.ifBlank { "Geral" })
    put("av", userAvatar)
    put("ts", System.currentTimeMillis())
}.toString()
