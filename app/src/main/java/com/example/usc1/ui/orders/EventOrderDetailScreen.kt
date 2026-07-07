package com.example.usc1.ui.orders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventOrderDetailScreen(
    state: EventOrderDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando pedido", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Pedido",
                subtitle = "Erro ao carregar detalhe",
                icon = Icons.Outlined.ReceiptLong,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Pedido indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ReceiptLong,
            )
        }
        state.order != null -> EventOrderDetailLoaded(
            order = state.order,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventOrderDetailLoaded(
    order: EventOrder,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Detalhe do pedido",
            subtitle = "Pedido #${order.id.take(8).uppercase()}",
            icon = Icons.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )
        EventOrderCard(order = order, onDetailsClick = {})
        PremiumCard(accent = orderStatusColor(order.status)) {
            OrderStatusChip(status = order.status)
            PremiumInfoRow("Quantidade", "${order.quantity}")
            PremiumInfoRow("Lote", order.lotName)
            PremiumInfoRow("Valor", order.amountLabel)
            PremiumInfoRow("Pagamento", order.paymentStatus.label)
            PremiumInfoRow("Aprovação", order.approvalStatus)
            PremiumInfoRow("Criado em", order.createdAtLabel)
        }
        PremiumCard(accent = com.example.usc1.core.ui.PremiumAmber) {
            PremiumInfoRow("Chave PIX", "mock-pix-usc")
            PremiumInfoRow("Banco", "Banco da Atlética")
            PremiumInfoRow("Titular", "AAAKN USC")
        }
        PremiumPrimaryButton(
            text = "Copiar PIX",
            onClick = {},
            icon = Icons.Outlined.ContentCopy,
            accent = com.example.usc1.core.ui.PremiumAmber,
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
fun EventOrderDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        EventOrderDetailScreen(
            state = EventOrderDetailUiState(order = MockEventOrdersRepository.mockOrders.first()),
            onBackClick = {},
        )
    }
}
