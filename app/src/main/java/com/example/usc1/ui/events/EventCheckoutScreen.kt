package com.example.usc1.ui.events

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton

@Composable
fun EventCheckoutUnavailableScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 100.dp,
    ) {
        PremiumHeader(
            title = "Pedido do evento",
            subtitle = "Fluxo real pendente",
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Checkout não liberado",
            subtitle = "Pedidos e pagamentos de evento exigem clonagem do fluxo web com segurança antes de gravar no Supabase.",
            icon = Icons.Outlined.Payment,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
        )
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
            subtitle = "Este fluxo ainda precisa ser clonado do web app com Supabase real antes de entrar em produção.",
            icon = Icons.Outlined.Payment,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
        )
    }
}
