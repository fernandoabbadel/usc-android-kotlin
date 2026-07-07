package com.example.usc1.ui.tickets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TicketQrPlaceholder(
                payload = ticket.qrPayload,
                compact = true,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TicketStatusChip(status = ticket.status)
                Text(
                    text = ticket.eventTitle,
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${ticket.dateLabel} • ${ticket.lotName}",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = ticket.token,
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                PremiumSecondaryButton(
                    text = "Detalhes",
                    onClick = onDetailsClick,
                    icon = Icons.Outlined.ArrowForward,
                )
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
    PremiumQrCode(
        payload = payload,
        modifier = modifier,
        cells = if (compact) 9 else 13,
        cellSize = if (compact) 4.dp else 9.dp,
        label = if (compact) null else "QR mockado",
    )
}

@Composable
fun TicketStatusChip(
    status: TicketStatus,
    modifier: Modifier = Modifier,
) {
    PremiumChip(
        label = status.label,
        modifier = modifier,
        icon = Icons.Outlined.ConfirmationNumber,
        accent = when (status) {
            TicketStatus.Active -> PremiumBrand
            TicketStatus.Pending -> PremiumAmber
            TicketStatus.Used -> PremiumZinc500
            TicketStatus.Transferred -> PremiumBrand
            TicketStatus.Cancelled -> PremiumRed
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TicketCardPreview() {
    UscTheme(darkTheme = true) {
        TicketCard(
            ticket = MockEventTicketsRepository.mockTickets.first(),
            onDetailsClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
