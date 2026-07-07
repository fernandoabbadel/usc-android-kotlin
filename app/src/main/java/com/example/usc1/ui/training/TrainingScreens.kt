package com.example.usc1.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.ui.theme.UscTheme

@Composable
fun TrainingScreen(
    state: TrainingUiState,
    onSessionClick: (TrainingSession) -> Unit,
    onCheckInClick: () -> Unit,
    onFrequencyClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Treinos",
            subtitle = "Check-in, frequência e feed da comunidade",
            icon = Icons.Outlined.FitnessCenter,
        )

        TrainingChallengeCard(
            state = state,
            onCheckInClick = onCheckInClick,
            onFrequencyClick = onFrequencyClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Histórico",
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.History,
            )
            PremiumSecondaryButton(
                text = "Check-in",
                onClick = onCheckInClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.QrCodeScanner,
                accent = PremiumAmber,
            )
        }

        Text(
            text = "TREINOS ABERTOS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 2.dp),
        )
        state.sessions.forEach { session ->
            TrainingCard(
                session = session,
                onClick = { onSessionClick(session) },
            )
        }
    }
}

@Composable
fun TrainingCheckInScreen(
    state: TrainingUiState,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Check-in",
            subtitle = "Validação visual do treino",
            icon = Icons.Outlined.QrCodeScanner,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            PremiumChip(label = state.checkIn.status.label, icon = Icons.Outlined.CheckCircle, accent = PremiumBrand, filled = true)
            Text(
                text = state.checkIn.sessionTitle,
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = "${state.checkIn.userName} • ${state.checkIn.createdAtLabel}",
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PremiumQrCode(
                    payload = state.checkIn.qrPayload,
                    cells = 13,
                    cellSize = 7.dp,
                    label = "CHECK-IN USC",
                )
            }
            PremiumPrimaryButton(
                text = "Confirmar presença",
                onClick = onConfirmClick,
                icon = Icons.Outlined.CheckCircle,
            )
        }
    }
}

@Composable
fun TrainingCheckInDetailScreen(
    checkIn: TrainingCheckIn,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Detalhe do Check-in",
            subtitle = checkIn.createdAtLabel,
            icon = Icons.Outlined.CheckCircle,
            accent = trainingStatusColor(checkIn.status),
            onBackClick = onBackClick,
        )
        PremiumCard(accent = trainingStatusColor(checkIn.status)) {
            TrainingStatusChip(status = checkIn.status)
            PremiumInfoRow(label = "Treino", value = checkIn.sessionTitle, accent = trainingStatusColor(checkIn.status))
            PremiumInfoRow(label = "Aluno", value = checkIn.userName, accent = trainingStatusColor(checkIn.status))
            PremiumInfoRow(label = "Código", value = checkIn.id, accent = trainingStatusColor(checkIn.status))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PremiumQrCode(payload = checkIn.qrPayload, label = "VALIDADO")
            }
        }
    }
}

@Composable
fun TrainingFrequencyScreen(
    state: TrainingUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Frequência",
            subtitle = state.frequency.monthLabel,
            icon = Icons.Outlined.Star,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "${state.frequency.attended}/${state.frequency.total}",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = state.frequency.streakLabel,
                color = PremiumBrand,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            FrequencyBar(
                attended = state.frequency.attended,
                total = state.frequency.total,
            )
            PremiumInfoRow(label = "Mês", value = state.frequency.monthLabel)
            PremiumInfoRow(label = "Presenças", value = "${state.frequency.attended} confirmadas")
        }

        state.history.forEach { checkIn ->
            TrainingHistoryRow(checkIn = checkIn)
        }
    }
}

@Composable
fun TrainingHistoryScreen(
    state: TrainingUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Histórico",
            subtitle = "Check-ins e validações recentes",
            icon = Icons.Outlined.History,
            onBackClick = onBackClick,
        )
        state.history.forEach { checkIn ->
            TrainingHistoryRow(checkIn = checkIn)
        }
    }
}

@Composable
private fun TrainingChallengeCard(
    state: TrainingUiState,
    onCheckInClick: () -> Unit,
    onFrequencyClick: () -> Unit,
) {
    PremiumCard(accent = PremiumBrand) {
        PremiumChip(label = state.activeChallengeSubtitle, icon = Icons.Outlined.Star, accent = PremiumBrand, filled = true)
        Text(
            text = state.activeChallengeTitle.uppercase(),
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
        Text(
            text = state.activeChallengeDescription,
            color = PremiumZinc400,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Frequência",
                onClick = onFrequencyClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Star,
            )
            PremiumPrimaryButton(
                text = "Check-in",
                onClick = onCheckInClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.QrCodeScanner,
            )
        }
    }
}

@Composable
private fun FrequencyBar(
    attended: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val progress = attended.toFloat() / total.coerceAtLeast(1).toFloat()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
    ) {
        val radius = CornerRadius(18f, 18f)
        drawRoundRect(
            color = Color(0xFF27272A),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = PremiumBrand,
            size = size.copy(width = size.width * progress.coerceIn(0f, 1f)),
            cornerRadius = radius,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TrainingScreenPreview() {
    UscTheme(darkTheme = true) {
        TrainingScreen(
            state = TrainingUiState(),
            onSessionClick = {},
            onCheckInClick = {},
            onFrequencyClick = {},
            onHistoryClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TrainingCheckInScreenPreview() {
    UscTheme(darkTheme = true) {
        TrainingCheckInScreen(
            state = TrainingUiState(),
            onConfirmClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TrainingFrequencyScreenPreview() {
    UscTheme(darkTheme = true) {
        TrainingFrequencyScreen(
            state = TrainingUiState(),
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun TrainingCheckInDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        TrainingCheckInDetailScreen(
            checkIn = TrainingMockData.checkIn,
            onBackClick = {},
        )
    }
}
