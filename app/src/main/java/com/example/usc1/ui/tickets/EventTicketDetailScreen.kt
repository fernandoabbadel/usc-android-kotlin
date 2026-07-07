package com.example.usc1.ui.tickets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.data.repository.MockEventTicketsRepository
import com.example.usc1.domain.model.EventTicket
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventTicketDetailScreen(
    state: EventTicketDetailUiState,
    onTransferClick: () -> Unit,
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
            onTransferClick = onTransferClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun TicketDetailLoadedContent(
    ticket: EventTicket,
    onTransferClick: () -> Unit,
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
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TicketQrPlaceholder(payload = ticket.qrPayload)
        }
        TicketStatusChip(status = ticket.status)
        PremiumCard {
            PremiumInfoRow("Titular", ticket.holderName)
            PremiumInfoRow("Lote", ticket.lotName)
            PremiumInfoRow("Data", ticket.dateLabel)
            PremiumInfoRow("Token", ticket.token)
            PremiumInfoRow("Transferência", if (ticket.transferAvailable) "Disponível" else "Indisponível")
        }
        PremiumPrimaryButton(
            text = "Transferir ingresso",
            onClick = onTransferClick,
            enabled = ticket.transferAvailable,
            icon = Icons.Outlined.Send,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventTicketDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        EventTicketDetailScreen(
            state = EventTicketDetailUiState(ticket = MockEventTicketsRepository.mockTickets.first()),
            onTransferClick = {},
            onBackClick = {},
        )
    }
}
