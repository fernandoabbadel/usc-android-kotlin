package com.example.usc1.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.TrainingParticipant
import com.example.usc1.domain.model.TrainingParticipantStatus
import com.example.usc1.domain.model.TrainingRsvpStatus

private val Weekdays = listOf("D", "S", "T", "Q", "Q", "S", "S")

/** Calendário mensal com os pontos coloridos por modalidade, como em `/treinos`. */
@Composable
fun TrainingCalendar(
    state: TrainingAgendaUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrainingMonthArrow(label = "‹", onClick = onPreviousMonth)
                Text(
                    text = state.monthLabel.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                TrainingMonthArrow(label = "›", onClick = onNextMonth)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Weekdays.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            state.days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { item ->
                        TrainingCalendarCell(
                            item = item,
                            isSelected = item.day != null && item.day == state.selectedDay,
                            onClick = { item.day?.let(onDayClick) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(7 - week.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingMonthArrow(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, color = PremiumZinc400, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TrainingCalendarCell(
    item: TrainingCalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item.day == null) {
        Box(modifier = modifier.height(42.dp))
        return
    }

    val background = when {
        isSelected -> PremiumBrand
        item.isHoliday -> PremiumRed.copy(alpha = 0.12f)
        else -> PremiumZinc900.copy(alpha = 0.6f)
    }
    val border = when {
        isSelected -> PremiumBrand
        item.isHoliday -> PremiumRed.copy(alpha = 0.32f)
        else -> PremiumZinc800
    }
    val textColor = when {
        isSelected -> Color.Black
        item.isHoliday -> PremiumRed
        else -> PremiumZinc400
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = item.day.toString(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                item.dotColors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.Black else parseHexColor(hex)),
                    )
                }
            }
        }
    }
}

/** Card de treino do dia, com contadores, ranking de turmas e botões de RSVP. */
@Composable
fun TrainingAgendaCard(
    item: TrainingAgendaItem,
    isPending: Boolean,
    onClick: () -> Unit,
    onRsvp: (TrainingRsvpStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseHexColor(item.session.calendarColor)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color.Black),
            ) {
                if (!item.session.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.session.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.72f),
                                    PremiumZinc900,
                                ),
                            ),
                        ),
                )
                PremiumChip(
                    label = item.session.modality,
                    icon = Icons.Outlined.FitnessCenter,
                    accent = accent,
                    filled = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TrainingCounterPill(
                        icon = Icons.Outlined.Group,
                        label = "${item.confirmedCount} confirmados",
                        tint = Color.White,
                    )
                    TrainingCounterPill(
                        icon = Icons.Outlined.CheckCircle,
                        label = "${item.presentCount} presentes",
                        tint = PremiumBrand,
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.session.modality.uppercase(),
                        color = Color.White,
                        fontSize = 26.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrainingMetaItem(Icons.Outlined.Schedule, item.session.time.ifBlank { "Livre" }, accent)
                        TrainingMetaItem(
                            Icons.Outlined.LocationOn,
                            item.session.location.ifBlank { "Local a definir" },
                            accent,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (item.classRanking.isEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PremiumZinc500,
                        )
                        Text(
                            text = "SEJA A PRIMEIRA TURMA!",
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.classRanking.forEach { ranking ->
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, PremiumZinc800),
                            ) {
                                Text(
                                    text = "${ranking.className} +${ranking.count}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }

                if (item.avatars.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.avatars.forEach { avatar ->
                            AsyncImage(
                                model = avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PremiumZinc800),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrainingRsvpButton(
                        text = "Não vou",
                        selected = false,
                        accent = PremiumRed,
                        enabled = !isPending,
                        onClick = { onRsvp(TrainingRsvpStatus.NotGoing) },
                        modifier = Modifier.weight(1f),
                    )
                    TrainingRsvpButton(
                        text = if (item.isConfirmed) "Confirmado" else "Eu vou!",
                        selected = item.isConfirmed,
                        accent = PremiumBrand,
                        enabled = !isPending,
                        onClick = { onRsvp(TrainingRsvpStatus.Going) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun TrainingRsvpButton(
    text: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent else Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 1f else 0.32f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text.uppercase(),
                color = if (selected) Color.Black else accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun TrainingCounterPill(
    icon: ImageVector,
    label: String,
    tint: Color,
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = tint)
            Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun TrainingMetaItem(
    icon: ImageVector,
    label: String,
    accent: Color = PremiumBrand,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
            Text(
                text = label,
                color = PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Linha da lista de presença de `/treinos/[id]`. */
@Composable
fun TrainingParticipantRow(
    participant: TrainingParticipant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (participant.status) {
        TrainingParticipantStatus.Presente -> PremiumBrand
        TrainingParticipantStatus.Falta -> PremiumRed
        TrainingParticipantStatus.Confirmado -> PremiumZinc700
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PremiumZinc800)
                    .border(1.dp, PremiumZinc700, CircleShape),
            ) {
                if (!participant.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = participant.name,
                    color = if (participant.status == TrainingParticipantStatus.Falta) {
                        PremiumZinc500
                    } else {
                        Color.White
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = participant.userClass.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = when (participant.status) {
                    TrainingParticipantStatus.Presente -> "PRESENTE"
                    TrainingParticipantStatus.Falta -> "FALTOU"
                    TrainingParticipantStatus.Confirmado -> "INSCRITO"
                },
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun TrainingClassRankingRow(
    ranking: List<com.example.usc1.domain.model.TrainingClassRanking>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (ranking.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ranking.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Column(
                    modifier = Modifier
                        .width(88.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "DOMINANDO",
                        color = PremiumZinc500,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = entry.className,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "+${entry.count}",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

/** `calendarColor`/`modalidadeColors` vêm como hex do Supabase. */
internal fun parseHexColor(value: String): Color {
    val clean = value.trim().removePrefix("#")
    val normalized = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString("")
        6 -> clean
        else -> return PremiumBrand
    }
    val parsed = normalized.toLongOrNull(16) ?: return PremiumBrand
    return Color(0xFF000000L or parsed)
}
