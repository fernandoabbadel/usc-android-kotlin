package com.example.usc1.ui.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.R
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.ui.theme.UscTheme

@Composable
fun MiniVendorScreen(
    state: MiniVendorUiState,
    onProductsClick: () -> Unit,
    onPendingOrdersClick: () -> Unit,
    onApprovedOrdersClick: () -> Unit,
    onFinanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Mini-vendor", subtitle = "Produtos, pedidos e financeiro", icon = Icons.Outlined.Storefront)
        NativeModuleHeroCard(
            title = state.storeName,
            subtitle = "Modo vendedor",
            body = state.statusLabel,
            imageRes = R.drawable.logo_usc_wide,
            accent = PremiumBrand,
            status = "Mockado",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard("Receita", state.totalRevenueLabel, icon = Icons.Outlined.AccountBalanceWallet, modifier = Modifier.weight(1f))
            NativeStatCard("Pendente", state.pendingAmountLabel, icon = Icons.Outlined.Payment, accent = PremiumAmber, modifier = Modifier.weight(1f))
        }
        NativeActionCard(NativeAction("Produtos", "Catálogo, estoque e status.", Icons.Outlined.Storefront), onProductsClick)
        NativeActionCard(NativeAction("Pedidos pendentes", "Aprovar e validar retiradas.", Icons.Outlined.ReceiptLong, PremiumAmber), onPendingOrdersClick)
        NativeActionCard(NativeAction("Pedidos aprovados", "Histórico liberado para retirada.", Icons.Outlined.ReceiptLong), onApprovedOrdersClick)
        NativeActionCard(NativeAction("Financeiro", "Receita simplificada e saldo futuro.", Icons.Outlined.AccountBalanceWallet), onFinanceClick)
    }
}

@Composable
fun MiniVendorProductsScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Produtos", subtitle = state.storeName, icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        state.products.forEach { product -> MiniVendorProductCard(product = product) }
    }
}

@Composable
fun MiniVendorPendingOrdersScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Pendentes", subtitle = state.storeName, icon = Icons.Outlined.ReceiptLong, accent = PremiumAmber, onBackClick = onBackClick)
        state.pendingOrders.forEach { order -> MiniVendorOrderCard(order = order) }
    }
}

@Composable
fun MiniVendorApprovedOrdersScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Aprovados", subtitle = state.storeName, icon = Icons.Outlined.ReceiptLong, onBackClick = onBackClick)
        state.approvedOrders.forEach { order -> MiniVendorOrderCard(order = order) }
    }
}

@Composable
fun MiniVendorFinanceScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Financeiro", subtitle = "Gestão simplificada", icon = Icons.Outlined.AccountBalanceWallet, onBackClick = onBackClick)
        NativeStatCard("Receita aprovada", state.totalRevenueLabel, icon = Icons.Outlined.AccountBalanceWallet)
        NativeStatCard("Aguardando baixa", state.pendingAmountLabel, icon = Icons.Outlined.Payment, accent = PremiumAmber)
        NativeSectionTitle(title = "Últimos aprovados")
        state.approvedOrders.forEach { order -> MiniVendorOrderCard(order = order) }
    }
}

@Composable
fun SalesModeScreen(
    state: MiniVendorUiState,
    onEventMenuClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Modo Vendas", subtitle = "Menu do evento e retiradas", icon = Icons.Outlined.QrCodeScanner)
        NativeModuleHeroCard(
            title = "MENU DO EVENTO",
            subtitle = "Vendas ativas",
            body = "Atalhos de produtos, pedidos e scanner visual para retirada.",
            imageRes = R.drawable.battle_forest,
            accent = PremiumBrand,
            status = state.storeName,
        )
        NativeActionCard(NativeAction("Menu do evento", "Produtos e fichas disponíveis.", Icons.Outlined.Storefront), onEventMenuClick)
        NativeActionCard(NativeAction("Scanner de retirada", "Validar pedido mockado por QR.", Icons.Outlined.QrCodeScanner, PremiumAmber), onScannerClick)
    }
}

@Composable
fun SalesModeEventMenuScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Menu do Evento", subtitle = "Produtos do vendedor", icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        state.products.forEach { product -> MiniVendorProductCard(product = product) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun MiniVendorScreenPreview() {
    UscTheme(darkTheme = true) {
        MiniVendorScreen(MiniVendorUiState(), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun SalesModeScreenPreview() {
    UscTheme(darkTheme = true) {
        SalesModeScreen(MiniVendorUiState(), {}, {})
    }
}
