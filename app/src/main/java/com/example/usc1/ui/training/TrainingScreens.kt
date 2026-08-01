package com.example.usc1.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.TrainingRsvpStatus

/** `/treinos` — agenda mensal com calendário e cards do dia. */
@Composable
fun TrainingAgendaScreen(
    state: TrainingAgendaUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit,
    onSessionClick: (String) -> Unit,
    onRsvp: (String, TrainingRsvpStatus) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Agenda de Treinos",
            subtitle = "Confirme presença e acompanhe a chamada",
            icon = Icons.Outlined.FitnessCenter,
            onBackClick = onBackClick,
        )

        if (state.requiresSession) {
            PremiumEmptyState(
                title = "Sessão necessária",
                subtitle = "Entre com sua conta para carregar os treinos da atlética.",
                icon = Icons.Outlined.FitnessCenter,
            )
            return@PremiumScreen
        }

        TrainingCalendar(
            state = state,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDayClick = onDayClick,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = PremiumBrand,
                )
                Text(
                    text = state.selectedDayLabel,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
            if (state.isSelectedDayHoliday) {
                PremiumChip(label = "Feriado", accent = PremiumRed)
            }
        }

        state.message?.let { message ->
            PremiumChip(label = message, accent = PremiumBrand)
        }

        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando grade")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Treinos indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.FitnessCenter,
            )
            state.sessions.isEmpty() -> PremiumEmptyState(
                title = "Sem treino",
                subtitle = "Nenhum treino cadastrado para este dia.",
                icon = Icons.Outlined.FitnessCenter,
            )
            else -> state.sessions.forEach { item ->
                TrainingAgendaCard(
                    item = item,
                    isPending = state.pendingSessionId == item.session.id,
                    onClick = { onSessionClick(item.session.id) },
                    onRsvp = { status -> onRsvp(item.session.id, status) },
                )
            }
        }
    }
}

/** `/treinos/[id]` — detalhe com lista de presença e QR do atleta. */
@Composable
fun TrainingDetailScreen(
    state: TrainingDetailUiState,
    onBackClick: () -> Unit,
    onRsvp: (TrainingRsvpStatus) -> Unit,
    onTogglePresenceQr: (Boolean) -> Unit,
    onOpenMaps: (String) -> Unit,
    onCoachClick: (String) -> Unit,
    onParticipantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando treino", modifier = modifier)
        return
    }

    val session = state.session
    if (session == null || state.notFound) {
        PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = "Treino",
                subtitle = "Agenda da atlética",
                icon = Icons.Outlined.FitnessCenter,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Treino não encontrado",
                subtitle = state.errorMessage ?: "Consulte a agenda atual da atlética.",
                icon = Icons.Outlined.FitnessCenter,
            )
        }
        return
    }

    if (state.blockedByPastRule) {
        PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = session.modality,
                subtitle = formatTrainingDate(session.date),
                icon = Icons.Outlined.FitnessCenter,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Treino encerrado",
                subtitle = "Consulte a agenda atual. O histórico fica disponível para a comissão técnica.",
                icon = Icons.Outlined.CalendarMonth,
            )
        }
        return
    }

    val accent = parseHexColor(session.calendarColor)

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = session.modality,
            subtitle = "${session.weekdayLabel.ifBlank { "Treino" }} • ${formatTrainingShortDate(session.date)}",
            icon = Icons.Outlined.FitnessCenter,
            accent = accent,
            onBackClick = onBackClick,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
        ) {
            if (!session.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = session.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PremiumChip(
                    label = "${state.confirmedCount} atletas",
                    icon = Icons.Outlined.Group,
                    accent = accent,
                    filled = true,
                )
                PremiumChip(
                    label = "${state.presentCount} presentes",
                    icon = Icons.Outlined.CheckCircle,
                    accent = PremiumBrand,
                )
            }
        }

        TrainingClassRankingRow(ranking = state.classRanking, accent = accent)

        state.message?.let { message ->
            PremiumChip(label = message, accent = accent)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TrainingRsvpButton(
                text = "Não vou",
                selected = false,
                accent = PremiumRed,
                enabled = !state.isSubmitting,
                onClick = { onRsvp(TrainingRsvpStatus.NotGoing) },
                modifier = Modifier.weight(1f),
            )
            TrainingRsvpButton(
                text = if (state.isConfirmed) "Confirmado" else "Confirmar presença",
                selected = state.isConfirmed,
                accent = PremiumBrand,
                enabled = !state.isSubmitting,
                onClick = { onRsvp(TrainingRsvpStatus.Going) },
                modifier = Modifier.weight(2f),
            )
        }

        PremiumSecondaryButton(
            text = if (state.showPresenceQr) "Fechar QR de presença" else "Abrir QR de presença",
            onClick = { onTogglePresenceQr(!state.showPresenceQr) },
            icon = Icons.Outlined.QrCode2,
        )

        if (state.showPresenceQr && state.presenceQrPayload.isNotBlank()) {
            PremiumCard(accent = PremiumBrand) {
                Text(
                    text = "QR DO TREINO",
                    color = PremiumBrand,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = session.modality.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PremiumQrCode(
                        payload = state.presenceQrPayload,
                        cells = 13,
                        cellSize = 7.dp,
                        label = "PRESENÇA USC",
                    )
                }
                Text(
                    text = "${formatTrainingDate(session.date)} • ${session.time.ifBlank { "-" }}",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        PremiumCard(accent = accent) {
            Text(
                text = "O TREINO",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = session.description.ifBlank { "Sem descrição informada." },
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingMetaItem(Icons.Outlined.Schedule, session.time.ifBlank { "Livre" }, accent)
                TrainingMetaItem(Icons.Outlined.CalendarMonth, formatTrainingDate(session.date), accent)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = PremiumZinc900,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LOCALIZAÇÃO",
                        color = PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = session.location.ifBlank { "Local a definir" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (session.location.isNotBlank()) {
            PremiumSecondaryButton(
                text = "Abrir no mapa",
                onClick = { onOpenMaps(session.location) },
                icon = Icons.Outlined.Map,
                accent = accent,
            )
        }

        PremiumCard(accent = accent) {
            Text(
                text = "RESPONSÁVEL",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PremiumZinc800)
                        .border(1.dp, PremiumZinc700, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!session.coachAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = session.coachAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = PremiumZinc500,
                        )
                    }
                }
                Text(
                    text = session.coachName.ifBlank { "Equipe responsável" },
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (session.coachId.isNotBlank()) {
                PremiumSecondaryButton(
                    text = "Ver perfil",
                    onClick = { onCoachClick(session.coachId) },
                    accent = accent,
                )
            }
        }

        Text(
            text = "LISTA DE PRESENÇA",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        if (state.participants.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum confirmado ainda",
                subtitle = "Seja o primeiro a confirmar presença neste treino.",
                icon = Icons.Outlined.Group,
            )
        } else {
            state.participants.forEach { participant ->
                TrainingParticipantRow(
                    participant = participant,
                    onClick = { onParticipantClick(participant.userId) },
                )
            }
        }
    }
}
