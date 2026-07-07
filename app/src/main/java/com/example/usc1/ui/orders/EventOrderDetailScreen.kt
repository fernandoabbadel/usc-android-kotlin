package com.example.usc1.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventOrderDetailScreen(
    state: EventOrderDetailUiState,
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
            state.order != null -> EventOrderDetailLoaded(
                order = state.order,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun EventOrderDetailLoaded(
    order: EventOrder,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Detalhe do pedido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OrderStatusChip(status = order.status)
        AppSectionHeader(
            title = order.eventTitle,
            subtitle = "Pedido ${order.id}",
        )
        EventOrderDetailRow("Quantidade", "${order.quantity}")
        EventOrderDetailRow("Lote", order.lotName)
        EventOrderDetailRow("Valor", order.amountLabel)
        EventOrderDetailRow("Pagamento", order.paymentStatus.label)
        EventOrderDetailRow("Aprovação", order.approvalStatus)
        EventOrderDetailRow("Criado em", order.createdAtLabel)
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@Composable
private fun EventOrderDetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventOrderDetailScreenPreview() {
    UscTheme {
        EventOrderDetailScreen(
            state = EventOrderDetailUiState(order = MockEventOrdersRepository.mockOrders.first()),
            onBackClick = {},
        )
    }
}
