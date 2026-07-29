package com.example.usc1.ui.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.R
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen

@Composable
fun MiniVendorScreen(
    state: MiniVendorUiState,
    onProductsClick: () -> Unit,
    onPendingOrdersClick: () -> Unit,
    onApprovedOrdersClick: () -> Unit,
    onFinanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando Mini Vendor", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Mini-vendor", subtitle = "Produtos, pedidos e financeiro", icon = Icons.Outlined.Storefront)
        state.errorMessage?.let { message ->
            PremiumEmptyState(
                title = "Mini Vendor indisponível",
                subtitle = message,
                icon = Icons.Outlined.Storefront,
            )
            return@PremiumScreen
        }
        if (!state.hasProfile) {
            PremiumEmptyState(
                title = "Mini Vendor não cadastrado",
                subtitle = state.statusLabel,
                icon = Icons.Outlined.Storefront,
            )
            return@PremiumScreen
        }
        NativeModuleHeroCard(
            title = state.storeName,
            subtitle = "Modo vendedor",
            body = state.description.ifBlank { state.statusLabel },
            imageRes = R.drawable.logo_usc_wide,
            imageUrl = state.coverUrl ?: state.logoUrl,
            accent = PremiumBrand,
            status = state.statusLabel,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard("Receita", state.totalRevenueLabel, icon = Icons.Outlined.AccountBalanceWallet, modifier = Modifier.weight(1f))
            NativeStatCard("Pendente", state.pendingAmountLabel, icon = Icons.Outlined.Payment, accent = PremiumAmber, modifier = Modifier.weight(1f))
        }
        NativeActionCard(NativeAction("Produtos", "Catálogo, estoque e status.", Icons.Outlined.Storefront), onProductsClick)
        NativeActionCard(NativeAction("Pedidos pendentes", "Aprovar e validar retiradas.", Icons.AutoMirrored.Outlined.ReceiptLong, PremiumAmber), onPendingOrdersClick)
        NativeActionCard(NativeAction("Pedidos aprovados", "Histórico liberado para retirada.", Icons.AutoMirrored.Outlined.ReceiptLong), onApprovedOrdersClick)
        NativeActionCard(NativeAction("Financeiro", "Receita aprovada e saldo futuro.", Icons.Outlined.AccountBalanceWallet), onFinanceClick)
    }
}

@Composable
fun MiniVendorProductsScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando produtos", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Produtos", subtitle = state.storeName, icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Produtos indisponíveis", state.errorMessage, Icons.Outlined.Storefront)
            !state.hasProfile -> PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.Outlined.Storefront)
            state.products.isEmpty() -> PremiumEmptyState("Nenhum produto", "Cadastre produtos no Mini Vendor para aparecerem aqui.", Icons.Outlined.Storefront)
            else -> state.products.forEach { product -> MiniVendorProductCard(product = product) }
        }
    }
}

@Composable
fun MiniVendorPendingOrdersScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando pedidos", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Pendentes", subtitle = state.storeName, icon = Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Pedidos indisponíveis", state.errorMessage, Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber)
            !state.hasProfile -> PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber)
            state.pendingOrders.isEmpty() -> PremiumEmptyState("Sem pendências", "Nenhum pedido aguardando aprovação agora.", Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber)
            else -> state.pendingOrders.forEach { order -> MiniVendorOrderCard(order = order) }
        }
    }
}

@Composable
fun MiniVendorApprovedOrdersScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando aprovados", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Aprovados", subtitle = state.storeName, icon = Icons.AutoMirrored.Outlined.ReceiptLong, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Pedidos indisponíveis", state.errorMessage, Icons.AutoMirrored.Outlined.ReceiptLong)
            !state.hasProfile -> PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.AutoMirrored.Outlined.ReceiptLong)
            state.approvedOrders.isEmpty() -> PremiumEmptyState("Sem aprovados", "Nenhum pedido aprovado encontrado.", Icons.AutoMirrored.Outlined.ReceiptLong)
            else -> state.approvedOrders.forEach { order -> MiniVendorOrderCard(order = order) }
        }
    }
}

@Composable
fun MiniVendorFinanceScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando financeiro", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Financeiro", subtitle = "Gestão simplificada", icon = Icons.Outlined.AccountBalanceWallet, onBackClick = onBackClick)
        if (state.errorMessage != null) {
            PremiumEmptyState("Financeiro indisponível", state.errorMessage, Icons.Outlined.AccountBalanceWallet)
            return@PremiumScreen
        }
        if (!state.hasProfile) {
            PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.Outlined.AccountBalanceWallet)
            return@PremiumScreen
        }
        NativeStatCard("Receita aprovada", state.totalRevenueLabel, icon = Icons.Outlined.AccountBalanceWallet)
        NativeStatCard("Aguardando baixa", state.pendingAmountLabel, icon = Icons.Outlined.Payment, accent = PremiumAmber)
        NativeSectionTitle(title = "Últimos aprovados")
        if (state.approvedOrders.isEmpty()) {
            PremiumEmptyState("Sem movimentação", "Os pedidos aprovados aparecerão aqui.", Icons.Outlined.AccountBalanceWallet)
        } else {
            state.approvedOrders.forEach { order -> MiniVendorOrderCard(order = order) }
        }
    }
}

@Composable
fun SalesModeScreen(
    state: MiniVendorUiState,
    onEventMenuClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando modo vendas", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Modo Vendas", subtitle = "Menu do evento e retiradas", icon = Icons.Outlined.QrCodeScanner)
        if (state.errorMessage != null) {
            PremiumEmptyState("Modo vendas indisponível", state.errorMessage, Icons.Outlined.QrCodeScanner)
            return@PremiumScreen
        }
        if (!state.hasProfile) {
            PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.Outlined.QrCodeScanner)
            return@PremiumScreen
        }
        NativeModuleHeroCard(
            title = "MENU DO EVENTO",
            subtitle = "Vendas ativas",
            body = "Atalhos de produtos, pedidos e scanner visual para retirada.",
            imageRes = R.drawable.battle_forest,
            imageUrl = state.coverUrl ?: state.logoUrl,
            accent = PremiumBrand,
            status = state.storeName,
        )
        NativeActionCard(NativeAction("Menu do evento", "Produtos e fichas disponíveis.", Icons.Outlined.Storefront), onEventMenuClick)
        NativeActionCard(NativeAction("Scanner de retirada", "Validar retirada por QR.", Icons.Outlined.QrCodeScanner, PremiumAmber), onScannerClick)
    }
}

@Composable
fun SalesModeEventMenuScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando menu", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Menu do Evento", subtitle = "Produtos do vendedor", icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Menu indisponível", state.errorMessage, Icons.Outlined.Storefront)
            !state.hasProfile -> PremiumEmptyState("Mini Vendor não cadastrado", state.statusLabel, Icons.Outlined.Storefront)
            state.products.isEmpty() -> PremiumEmptyState("Nenhum produto", "Produtos ativos do vendedor aparecerão neste menu.", Icons.Outlined.Storefront)
            else -> state.products.forEach { product -> MiniVendorProductCard(product = product) }
        }
    }
}
