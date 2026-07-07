package com.example.usc1.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventOrdersScreen(
    state: EventOrdersUiState,
    onOrderClick: (EventOrder) -> Unit,
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Pedidos de eventos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                AppSectionHeader(
                    title = "Histórico de pedidos",
                    subtitle = "Pendentes, aprovados e cancelados com pagamento mockado.",
                )
                if (state.isEmpty) {
                    Text(
                        text = "Nenhum pedido de evento encontrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
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

@Preview(showBackground = true)
@Composable
fun EventOrdersScreenPreview() {
    UscTheme {
        EventOrdersScreen(
            state = EventOrdersUiState(orders = MockEventOrdersRepository.mockOrders),
            onOrderClick = {},
        )
    }
}
