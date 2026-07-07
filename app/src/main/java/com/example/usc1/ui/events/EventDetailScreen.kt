package com.example.usc1.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
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
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando evento", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Evento",
                subtitle = "Erro ao carregar detalhes",
                icon = Icons.Outlined.Event,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Evento indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Event,
            )
        }
        state.event != null -> EventDetailLoadedContent(
            event = state.event,
            onCheckoutClick = onCheckoutClick,
            onTicketsClick = onTicketsClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventDetailLoadedContent(
    event: Event,
    onCheckoutClick: (Event) -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 120.dp,
    ) {
        PremiumHeader(
            title = event.title,
            subtitle = "${event.dateLabel} • ${event.location}",
            icon = Icons.Outlined.Event,
            onBackClick = onBackClick,
        )

        EventCover(
            event = event,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )

        PremiumCard(accent = eventStatusColor(event.status)) {
            EventStatusChip(status = event.status)
            androidx.compose.material3.Text(
                text = event.description,
                color = com.example.usc1.core.ui.PremiumZinc300,
                fontSize = 13.sp,
            )
            EventMetaLine(event = event)
        }

        PremiumCard {
            PremiumInfoRow("Lote", event.lotName)
            PremiumInfoRow("Preço", event.priceLabel)
            PremiumInfoRow("Vagas", "${event.availableSpots} disponíveis")
        }

        PremiumHeader(
            title = "Produtos",
            subtitle = "Fichas e itens vinculados ao evento",
            icon = Icons.Outlined.ShoppingBag,
        )
        if (event.products.isEmpty()) {
            PremiumEmptyState(
                title = "Sem produtos liberados",
                subtitle = "Produtos serão exibidos quando a venda abrir.",
                icon = Icons.Outlined.ShoppingBag,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                event.products.forEach { product ->
                    PremiumCard(accent = com.example.usc1.core.ui.PremiumPurple) {
                        PremiumInfoRow(product.name, "${product.priceLabel} • ${product.status}", accent = com.example.usc1.core.ui.PremiumPurple)
                    }
                }
            }
        }

        PremiumPrimaryButton(
            text = "Comprar / inscrever",
            onClick = { onCheckoutClick(event) },
            enabled = event.status == EventStatus.Open,
            icon = Icons.Outlined.ConfirmationNumber,
        )
        PremiumSecondaryButton(
            text = "Ver meus ingressos",
            onClick = onTicketsClick,
            icon = Icons.Outlined.ConfirmationNumber,
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
fun EventDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        EventDetailScreen(
            state = EventDetailUiState(event = MockEventsRepository.mockEvents.first()),
            onCheckoutClick = {},
            onTicketsClick = {},
            onBackClick = {},
        )
    }
}
