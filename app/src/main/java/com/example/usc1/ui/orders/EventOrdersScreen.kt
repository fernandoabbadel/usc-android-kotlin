package com.example.usc1.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventOrdersScreen(
    state: EventOrdersUiState,
    onOrderClick: (EventOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando pedidos", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Pedidos de eventos",
                subtitle = "Erro ao carregar histórico",
                icon = Icons.Outlined.ReceiptLong,
            )
            PremiumEmptyState(
                title = "Pedidos indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ReceiptLong,
            )
        }
        else -> PremiumScreen(
            modifier = modifier,
            bottomPadding = 110.dp,
        ) {
            PremiumHeader(
                title = "Pedidos de eventos",
                subtitle = "Pendentes, aprovados e cancelados",
                icon = Icons.Outlined.ReceiptLong,
            )
            if (state.isEmpty) {
                PremiumEmptyState(
                    title = "Sem pedidos",
                    subtitle = "Nenhum pedido de evento encontrado.",
                    icon = Icons.Outlined.ReceiptLong,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    state.orders.forEach { order ->
                        EventOrderCard(
                            order = order,
                            onDetailsClick = { onOrderClick(order) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun EventOrdersScreenPreview() {
    UscTheme(darkTheme = true) {
        EventOrdersScreen(
            state = EventOrdersUiState(orders = MockEventOrdersRepository.mockOrders),
            onOrderClick = {},
        )
    }
}
