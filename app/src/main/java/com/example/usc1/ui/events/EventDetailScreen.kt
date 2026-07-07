package com.example.usc1.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.usc1.data.repository.MockEventsRepository
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    onCheckoutClick: (Event) -> Unit,
    onTicketsClick: () -> Unit,
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
            state.event != null -> EventDetailLoadedContent(
                event = state.event,
                onCheckoutClick = onCheckoutClick,
                onTicketsClick = onTicketsClick,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun EventDetailLoadedContent(
    event: Event,
    onCheckoutClick: (Event) -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EventCover(
            event = event,
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EventStatusChip(status = event.status)
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EventDetailRow("Data", "${event.dateLabel} às ${event.timeLabel}")
        EventDetailRow("Local", event.location)
        EventDetailRow("Lote", event.lotName)
        EventDetailRow("Preço", event.priceLabel)
        EventDetailRow("Vagas", "${event.availableSpots} disponíveis")

        AppSectionHeader(
            title = "Produtos e fichas",
            subtitle = if (event.products.isEmpty()) "Produtos serão exibidos quando liberados." else "Itens mockados vinculados ao evento.",
        )
        event.products.forEach { product ->
            EventDetailRow(product.name, "${product.priceLabel} • ${product.status}")
        }

        Button(
            onClick = { onCheckoutClick(event) },
            modifier = Modifier.fillMaxWidth(),
            enabled = event.status == EventStatus.Open,
        ) {
            Text("Comprar/inscrever")
        }
        OutlinedButton(
            onClick = onTicketsClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ver meus ingressos")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Voltar")
        }
    }
}

@Composable
private fun EventDetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventDetailScreenPreview() {
    UscTheme {
        EventDetailScreen(
            state = EventDetailUiState(event = MockEventsRepository.mockEvents.first()),
            onCheckoutClick = {},
            onTicketsClick = {},
            onBackClick = {},
        )
    }
}
