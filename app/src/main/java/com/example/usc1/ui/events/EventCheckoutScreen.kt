package com.example.usc1.ui.events

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.data.repository.MockEventsRepository
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventCheckoutScreen(
    state: EventCheckoutUiState,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 100.dp,
    ) {
        PremiumHeader(
            title = "Pedido do evento",
            subtitle = "Checkout visual mockado",
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )
        EventCard(event = state.event, onClick = {})
        PremiumCard {
            EventStatusChip(status = state.event.status)
            PremiumInfoRow("Lote", state.event.lotName)
            PremiumInfoRow("Quantidade", state.selectedQuantity.toString())
            PremiumInfoRow("Total", state.totalLabel)
            PremiumInfoRow("Pagamento", "PIX em função segura futura")
        }
        PremiumPrimaryButton(
            text = "Criar pedido mockado",
            onClick = onConfirmClick,
            icon = Icons.Outlined.Payment,
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
private fun EventCheckoutScreenPreview() {
    UscTheme(darkTheme = true) {
        EventCheckoutScreen(
            state = EventCheckoutUiState(event = MockEventsRepository.mockEvents.first()),
            onConfirmClick = {},
            onBackClick = {},
        )
    }
}
