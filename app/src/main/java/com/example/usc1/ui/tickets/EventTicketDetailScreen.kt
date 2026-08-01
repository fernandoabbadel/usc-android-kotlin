package com.example.usc1.ui.tickets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.data.repository.MockEventTicketsRepository
import com.example.usc1.domain.model.EventTicket
import com.example.usc1.domain.model.TicketStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventTicketDetailScreen(
    state: EventTicketDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando ingresso", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Ingresso",
                subtitle = "Erro ao carregar",
                icon = Icons.Outlined.ConfirmationNumber,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Ingresso indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ConfirmationNumber,
            )
        }
        state.ticket != null -> TicketDetailLoadedContent(
            ticket = state.ticket,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun TicketDetailLoadedContent(
    ticket: EventTicket,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = ticket.eventTitle,
            subtitle = "Aproxime o QR do leitor",
            icon = Icons.Outlined.ConfirmationNumber,
            onBackClick = onBackClick,
        )
        if (ticket.isQrDisabled) {
            // Igual ao cartão público do web: ingresso transferido perde o QR antigo.
            PremiumCard(accent = PremiumRed) {
                Text(
                    text = "QR Code desativado",
                    color = PremiumRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (ticket.status == TicketStatus.Transferred) {
                        "Este ingresso foi transferido e o QR Code antigo não pode mais ser usado."
                    } else {
                        "Este ingresso foi cancelado e o QR Code não pode mais ser usado."
                    },
                    color = PremiumZinc300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TicketQrPlaceholder(payload = ticket.qrPayload)
            }
        }
        TicketStatusChip(status = ticket.status)
        PremiumCard {
            PremiumInfoRow("Titular", ticket.holderName)
            PremiumInfoRow("Turma", ticket.holderTurma.ifBlank { "Sem turma" })
            PremiumInfoRow("Lote", ticket.lotName)
            PremiumInfoRow("Data", ticket.dateLabel)
            PremiumInfoRow("Token", ticket.token)
            ticket.transferredToUserName.takeIf(String::isNotBlank)?.let { name ->
                PremiumInfoRow("Transferido para", name)
            }
            ticket.transferredFromUserName.takeIf(String::isNotBlank)?.let { name ->
                PremiumInfoRow("Transferido de", name)
            }
        }
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventTicketDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        EventTicketDetailScreen(
            state = EventTicketDetailUiState(ticket = MockEventTicketsRepository.mockTickets.first()),
            onBackClick = {},
        )
    }
}
