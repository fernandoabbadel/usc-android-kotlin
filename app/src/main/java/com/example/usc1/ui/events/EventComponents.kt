package com.example.usc1.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.data.repository.MockEventsRepository
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            EventCover(
                event = event,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${event.dateLabel} • ${event.timeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EventStatusChip(status = event.status)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = event.priceLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = event.lotName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EventCover(
    event: Event,
    modifier: Modifier = Modifier,
) {
    val colors = coverColors(event.status)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(colors = colors),
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.Event,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp),
            tint = Color.White.copy(alpha = 0.84f),
        )
        Text(
            text = event.coverColorName,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
fun EventStatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        EventStatus.Open -> MaterialTheme.colorScheme.primary
        EventStatus.Closed -> MaterialTheme.colorScheme.onSurfaceVariant
        EventStatus.SoldOut -> MaterialTheme.colorScheme.error
        EventStatus.ComingSoon -> MaterialTheme.colorScheme.secondary
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

private fun coverColors(status: EventStatus): List<Color> = when (status) {
    EventStatus.Open -> listOf(Color(0xFF0B6B57), Color(0xFF083D35))
    EventStatus.Closed -> listOf(Color(0xFF5E6C66), Color(0xFF25302C))
    EventStatus.SoldOut -> listOf(Color(0xFFB3261E), Color(0xFF4A1512))
    EventStatus.ComingSoon -> listOf(Color(0xFFFFC857), Color(0xFF8C5C00))
}

@Preview(showBackground = true)
@Composable
fun EventCardPreview() {
    UscTheme {
        EventCard(
            event = MockEventsRepository.mockEvents.first(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
