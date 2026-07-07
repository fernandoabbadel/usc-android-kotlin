package com.example.usc1.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.AppSectionHeader
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            state.ticket != null -> TicketDetailLoadedContent(
                ticket = state.ticket,
                onTransferClick = onTransferClick,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun TicketDetailLoadedContent(
    ticket: EventTicket,
    onTransferClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = ticket.eventTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        TicketQrPlaceholder(payload = ticket.qrPayload)
        TicketStatusChip(status = ticket.status)
        AppSectionHeader(
            title = "Dados do ingresso",
            subtitle = "Token mockado: ${ticket.token}",
        )
        TicketDetailRow("Titular", ticket.holderName)
        TicketDetailRow("Lote", ticket.lotName)
        TicketDetailRow("Data", ticket.dateLabel)
        TicketDetailRow("Transferência", if (ticket.transferAvailable) "Disponível como placeholder" else "Indisponível")
        Button(
            onClick = onTransferClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = ticket.transferAvailable,
        ) {
            Text("Transferir ingresso")
        }
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@Composable
private fun TicketDetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventTicketDetailScreenPreview() {
    UscTheme {
        EventTicketDetailScreen(
            state = EventTicketDetailUiState(ticket = MockEventTicketsRepository.mockTickets.first()),
            onTransferClick = {},
            onBackClick = {},
        )
    }
}
