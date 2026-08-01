package com.example.usc1.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBlueBlack
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.domain.model.Event

@Composable
fun EventsScreen(
    state: EventsUiState,
    onEventClick: (Event) -> Unit,
    onFilterClick: (EventFeedFilter) -> Unit,
    onTicketsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando agenda", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Agenda Eventos",
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
            onFilterClick = onFilterClick,
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
    onFilterClick: (EventFeedFilter) -> Unit,
    onTicketsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleEvents = state.filteredEvents.filter { event ->
        val query = searchQuery.trim()
        query.isBlank() ||
            event.title.contains(query, ignoreCase = true) ||
            event.location.contains(query, ignoreCase = true) ||
            event.ownerName.contains(query, ignoreCase = true) ||
            event.coverColorName.contains(query, ignoreCase = true)
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 120.dp,
    ) {
        PremiumHeader(
            title = "Agenda Eventos",
            subtitle = "Próximos eventos",
            icon = Icons.Outlined.CalendarMonth,
        )

        PremiumTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar evento por nome, local ou tipo",
            leadingIcon = Icons.Outlined.Search,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EventFeedFilter.entries.forEach { filter ->
                Box(modifier = Modifier.clickable { onFilterClick(filter) }) {
                    PremiumChip(
                        label = filter.label,
                        accent = if (state.selectedFilter == filter) PremiumBrand else PremiumZinc500,
                        filled = state.selectedFilter == filter,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PremiumChip(label = "Legenda", accent = PremiumZinc500)
            PremiumChip(label = "P Público", accent = Color(0xFF2563EB), filled = true)
            PremiumChip(label = "I Interno", accent = PremiumRed, filled = true)
            PremiumChip(label = "Vendas", accent = PremiumBrand)
            PremiumChip(label = "Comissões", accent = PremiumPurple)
        }

        Text(
            text = "PÚBLICO • INTERNO • VENDAS",
            color = PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )

        if (visibleEvents.isEmpty()) {
            PremiumEmptyState(
                title = "Nada por aqui",
                subtitle = "Nenhum evento ativo encontrado para este filtro.",
                icon = Icons.Outlined.CalendarMonth,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                visibleEvents.forEach { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                    )
                }
            }
        }
    }
}
