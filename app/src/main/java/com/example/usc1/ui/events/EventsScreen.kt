package com.example.usc1.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
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
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando agenda", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "AgendaEventos",
                subtitle = "Falha ao carregar eventos",
                icon = Icons.Outlined.CalendarMonth,
            )
            PremiumEmptyState(
                title = "Erro em eventos",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.CalendarMonth,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        else -> EventsLoadedContent(
            state = state,
            onEventClick = onEventClick,
            onStatusFilterClick = onStatusFilterClick,
            onTicketsClick = onTicketsClick,
            onOrdersClick = onOrdersClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventsLoadedContent(
    state: EventsUiState,
    onEventClick: (Event) -> Unit,
    onStatusFilterClick: (EventStatus?) -> Unit,
    onTicketsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 120.dp,
    ) {
        PremiumHeader(
            title = "AgendaEventos",
            subtitle = "Próximos eventos",
            icon = Icons.Outlined.CalendarMonth,
        )

        PremiumTextField(
            value = "",
            onValueChange = {},
            label = "Buscar evento por nome, local ou tipo",
            leadingIcon = Icons.Outlined.Search,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.clickable { onStatusFilterClick(null) }) {
                PremiumChip(
                    label = "Todos",
                    accent = if (state.selectedStatus == null) com.example.usc1.core.ui.PremiumBrand else com.example.usc1.core.ui.PremiumZinc500,
                    filled = state.selectedStatus == null,
                )
            }
            EventStatus.entries.forEach { status ->
                Box(modifier = Modifier.clickable { onStatusFilterClick(status) }) {
                    PremiumChip(
                        label = status.label,
                        accent = if (state.selectedStatus == status) eventStatusColor(status) else com.example.usc1.core.ui.PremiumZinc500,
                        filled = state.selectedStatus == status,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Meus ingressos",
                onClick = onTicketsClick,
                icon = Icons.Outlined.ConfirmationNumber,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Pedidos",
                onClick = onOrdersClick,
                icon = Icons.Outlined.History,
                modifier = Modifier.weight(1f),
            )
        }

        PremiumChip(label = "PÚBLICO • INTERNO • VENDAS", icon = Icons.Outlined.CalendarMonth)

        if (state.isEmpty) {
            PremiumEmptyState(
                title = "Nada por aqui",
                subtitle = "Nenhum evento ativo encontrado para este filtro.",
                icon = Icons.Outlined.CalendarMonth,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventsScreenPreview() {
    UscTheme(darkTheme = true) {
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventsScreenLoadingPreview() {
    UscTheme(darkTheme = true) {
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

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventsScreenEmptyPreview() {
    UscTheme(darkTheme = true) {
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
