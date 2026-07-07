package com.example.usc1.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
fun EventsScreen(
    state: EventsUiState,
    onEventClick: (Event) -> Unit,
    onStatusFilterClick: (EventStatus?) -> Unit,
    onTicketsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onRetryClick: () -> Unit,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetryClick) {
                        Text("Tentar novamente")
                    }
                }
            }
            else -> EventsLoadedContent(
                state = state,
                onEventClick = onEventClick,
                onStatusFilterClick = onStatusFilterClick,
                onTicketsClick = onTicketsClick,
                onOrdersClick = onOrdersClick,
            )
        }
    }
}

@Composable
private fun EventsLoadedContent(
    state: EventsUiState,
    onEventClick: (Event) -> Unit,
    onStatusFilterClick: (EventStatus?) -> Unit,
    onTicketsClick: () -> Unit,
    onOrdersClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Eventos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Ingressos, lotes, fichas e pedidos da atlética.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onTicketsClick, modifier = Modifier.weight(1f)) {
                Text("Meus ingressos")
            }
            Button(onClick = onOrdersClick, modifier = Modifier.weight(1f)) {
                Text("Pedidos")
            }
        }

        EventStatusFilters(
            selectedStatus = state.selectedStatus,
            onStatusFilterClick = onStatusFilterClick,
        )

        AppSectionHeader(
            title = "Lista de eventos",
            subtitle = "Dados mockados até a integração Supabase.",
        )

        if (state.isEmpty) {
            EmptyEventsMessage()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.events.forEach { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventStatusFilters(
    selectedStatus: EventStatus?,
    onStatusFilterClick: (EventStatus?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Filtrar por status",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = { onStatusFilterClick(null) },
                label = { Text("Todos") },
                enabled = selectedStatus != null,
            )
            EventStatus.entries.forEach { status ->
                AssistChip(
                    onClick = { onStatusFilterClick(status) },
                    label = { Text(status.label) },
                    enabled = selectedStatus != status,
                )
            }
        }
    }
}

@Composable
private fun EmptyEventsMessage() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = "Nenhum evento encontrado para este filtro.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventsScreenPreview() {
    UscTheme {
        EventsScreen(
            state = EventsUiState(events = MockEventsRepository.mockEvents),
            onEventClick = {},
            onStatusFilterClick = {},
            onTicketsClick = {},
            onOrdersClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventsScreenLoadingPreview() {
    UscTheme {
        EventsScreen(
            state = EventsUiState.loading(),
            onEventClick = {},
            onStatusFilterClick = {},
            onTicketsClick = {},
            onOrdersClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventsScreenEmptyPreview() {
    UscTheme {
        EventsScreen(
            state = EventsUiState.empty(),
            onEventClick = {},
            onStatusFilterClick = {},
            onTicketsClick = {},
            onOrdersClick = {},
            onRetryClick = {},
        )
    }
}
