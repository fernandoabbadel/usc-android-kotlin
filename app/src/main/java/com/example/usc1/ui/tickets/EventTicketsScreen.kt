package com.example.usc1.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.data.repository.MockEventTicketsRepository
import com.example.usc1.domain.model.EventTicket
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventTicketsScreen(
    state: EventTicketsUiState,
    onTicketClick: (EventTicket) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando ingressos", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Meus ingressos",
                subtitle = "Erro ao carregar QR Codes",
                icon = Icons.Outlined.ConfirmationNumber,
            )
            PremiumEmptyState(
                title = "Ingressos indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ConfirmationNumber,
            )
        }
        else -> PremiumScreen(
            modifier = modifier,
            bottomPadding = 110.dp,
        ) {
            PremiumHeader(
                title = "Meus ingressos",
                subtitle = "QR visual e status de entrada",
                icon = Icons.Outlined.ConfirmationNumber,
            )
            if (state.isEmpty) {
                PremiumEmptyState(
                    title = "Sem ingressos",
                    subtitle = "Você ainda não tem ingressos ativos.",
                    icon = Icons.Outlined.ConfirmationNumber,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    state.tickets.forEach { ticket ->
                        TicketCard(
                            ticket = ticket,
                            onDetailsClick = { onTicketClick(ticket) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventTicketsScreenPreview() {
    UscTheme(darkTheme = true) {
        EventTicketsScreen(
            state = EventTicketsUiState(tickets = MockEventTicketsRepository.mockTickets),
            onTicketClick = {},
        )
    }
}
