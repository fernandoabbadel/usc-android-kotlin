package com.example.usc1.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.AppSectionHeader
import com.example.usc1.data.repository.MockEventsRepository
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventCheckoutScreen(
    state: EventCheckoutUiState,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Pedido do evento",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AppSectionHeader(
                title = state.event.title,
                subtitle = "Checkout visual mockado. Pagamento real ficará em função/API segura.",
            )
            EventStatusChip(status = state.event.status)
            Text("Lote: ${state.event.lotName}", color = MaterialTheme.colorScheme.onSurface)
            Text("Quantidade: ${state.selectedQuantity}", color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Total: ${state.totalLabel}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onConfirmClick, modifier = Modifier.fillMaxWidth()) {
                Text("Criar pedido mockado")
            }
            OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventCheckoutScreenPreview() {
    UscTheme {
        EventCheckoutScreen(
            state = EventCheckoutUiState(event = MockEventsRepository.mockEvents.first()),
            onConfirmClick = {},
            onBackClick = {},
        )
    }
}
