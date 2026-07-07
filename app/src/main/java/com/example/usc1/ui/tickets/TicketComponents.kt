package com.example.usc1.ui.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.data.repository.MockEventTicketsRepository
import com.example.usc1.domain.model.EventTicket
import com.example.usc1.domain.model.TicketStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun TicketCard(
    ticket: EventTicket,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TicketQrPlaceholder(
                payload = ticket.qrPayload,
                compact = true,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = ticket.eventTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ticket.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TicketStatusChip(status = ticket.status)
                Text(
                    text = ticket.token,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onDetailsClick) {
                    Text("Detalhes")
                }
            }
        }
    }
}

@Composable
fun TicketQrPlaceholder(
    payload: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val cell = if (compact) 5.dp else 9.dp
    val padding = if (compact) 6.dp else 12.dp

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = if (compact) 0.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(11) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(11) { column ->
                            val filled = row in 0..2 && column in 0..2 ||
                                row in 0..2 && column in 8..10 ||
                                row in 8..10 && column in 0..2 ||
                                (payload.length + row * 7 + column * 11) % 5 != 0
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .background(
                                        color = if (filled) Color.Black else Color.White,
                                        shape = RoundedCornerShape(1.dp),
                                    ),
                            )
                        }
                    }
                }
            }
            if (!compact) {
                Text(
                    text = "QR mockado",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
fun TicketStatusChip(
    status: TicketStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        TicketStatus.Active -> MaterialTheme.colorScheme.primary
        TicketStatus.Pending -> MaterialTheme.colorScheme.secondary
        TicketStatus.Used -> MaterialTheme.colorScheme.onSurfaceVariant
        TicketStatus.Transferred -> MaterialTheme.colorScheme.primary
        TicketStatus.Cancelled -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TicketCardPreview() {
    UscTheme {
        TicketCard(
            ticket = MockEventTicketsRepository.mockTickets.first(),
            onDetailsClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
