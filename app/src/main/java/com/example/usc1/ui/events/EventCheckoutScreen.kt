package com.example.usc1.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300

@Composable
fun EventCheckoutScreen(
    state: EventDetailUiState,
    onOrdersClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando checkout", modifier = modifier)
        state.errorMessage != null -> EventFlowUnavailableScreen(
            title = "Pedido do evento",
            subtitle = state.errorMessage,
            onBackClick = onBackClick,
            modifier = modifier,
        )
        state.event != null -> {
            val event = state.event
            PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
                PremiumHeader(
                    title = "Pedido do evento",
                    subtitle = event.title,
                    icon = Icons.Outlined.ShoppingCart,
                    onBackClick = onBackClick,
                )
                EventCover(
                    event = event,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
                PremiumCard(accent = eventStatusColor(event.status)) {
                    EventStatusChip(status = event.status)
                    androidx.compose.material3.Text(
                        text = event.description,
                        color = PremiumZinc300,
                        fontSize = 13.sp,
                    )
                    PremiumInfoRow("Lote", event.lotName, accent = eventStatusColor(event.status))
                    PremiumInfoRow("Preço", event.priceLabel, accent = eventStatusColor(event.status))
                    PremiumInfoRow("Vagas", "${event.availableSpots} disponíveis", accent = eventStatusColor(event.status))
                    PremiumInfoRow("Organizador", event.ownerName.ifBlank { event.ownerType.label }, accent = eventStatusColor(event.status))
                }
                if (event.products.isNotEmpty()) {
                    PremiumHeader(
                        title = "Produtos do evento",
                        subtitle = "Itens liberados junto ao ingresso",
                        icon = Icons.Outlined.Payment,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        event.products.forEach { product ->
                            PremiumCard(accent = PremiumPurple) {
                                PremiumInfoRow(product.name, "${product.priceLabel} • ${product.status}", accent = PremiumPurple)
                            }
                        }
                    }
                }
                PremiumCard(accent = PremiumAmber) {
                    PremiumInfoRow("Integração segura", "A leitura do evento e dos pedidos já usa Supabase real.", accent = PremiumAmber)
                    PremiumInfoRow("Próximo passo", "Gravação de pagamento deve seguir o fluxo transacional do web app.", accent = PremiumAmber)
                }
                PremiumPrimaryButton(
                    text = "Ver pedidos do evento",
                    onClick = onOrdersClick,
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
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                )
            }
        }
    }
}

@Composable
fun EventFlowUnavailableScreen(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 100.dp,
    ) {
        PremiumHeader(
            title = title,
            subtitle = "Fluxo real pendente",
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = subtitle,
            subtitle = "Esta etapa ainda precisa seguir exatamente o fluxo seguro do web app antes de gravar no Supabase.",
            icon = Icons.Outlined.Payment,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
        )
    }
}
