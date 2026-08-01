package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.data.repository.SupabaseAdminEventSalesRepository
import com.example.usc1.domain.model.AdminEventSalesDashboard
import com.example.usc1.domain.model.AdminEventSalesEvent
import com.example.usc1.domain.model.AdminEventSalesOrder
import com.example.usc1.domain.model.AdminEventSalesOrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminEventSalesModeViewModel(
    private val repository: SupabaseAdminEventSalesRepository = SupabaseAdminEventSalesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminEventSalesModeUiState(isLoading = true))
    val uiState: StateFlow<AdminEventSalesModeUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        if (_uiState.value.dashboard.hasOperation && !forceRefresh) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                _uiState.value = AdminEventSalesModeUiState(
                    dashboard = repository.getDashboard(),
                )
            } catch (error: Throwable) {
                _uiState.value = AdminEventSalesModeUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o Modo Vendas.",
                )
            }
        }
    }
}

data class AdminEventSalesModeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dashboard: AdminEventSalesDashboard = AdminEventSalesDashboard(),
)

@Composable
fun AdminEventSalesModeScreen(
    state: AdminEventSalesModeUiState,
    onRefreshClick: () -> Unit,
    onOpenPublicSalesModeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando Modo Vendas", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier, bottomPadding = 110.dp) {
            PremiumHeader(
                title = "Eventos",
                subtitle = "Modo Vendas indisponível",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Erro no Modo Vendas",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Storefront,
                accent = PremiumAmber,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRefreshClick)
        }
        else -> AdminEventSalesModeLoaded(
            dashboard = state.dashboard,
            onRefreshClick = onRefreshClick,
            onOpenPublicSalesModeClick = onOpenPublicSalesModeClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminEventSalesModeLoaded(
    dashboard: AdminEventSalesDashboard,
    onRefreshClick: () -> Unit,
    onOpenPublicSalesModeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("Todos") }
    val statusFilters = listOf("Todos") + AdminEventSalesOrderStatus.entries.map { it.label }
    val visibleOrders = dashboard.orders.filter { order ->
        val query = searchQuery.trim()
        val statusMatches = selectedStatus == "Todos" || order.status.label == selectedStatus
        val searchMatches = query.isBlank() ||
            order.eventTitle.contains(query, ignoreCase = true) ||
            order.productName.contains(query, ignoreCase = true) ||
            order.userName.contains(query, ignoreCase = true) ||
            order.category.contains(query, ignoreCase = true)
        statusMatches && searchMatches
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Eventos",
            subtitle = "BI, pedidos e operação do Modo Vendas",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumAmber) {
            PremiumChip(
                label = "MODO VENDAS",
                icon = Icons.Outlined.Storefront,
                accent = PremiumAmber,
                filled = true,
            )
            Text(
                text = "OPERAÇÃO DE PRODUTOS",
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = "Pedidos de produtos do evento são lidos de `orders.data.eventParty`. Lotes e ingressos ficam fora deste painel.",
                color = PremiumZinc300,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryButton(
                    text = "Atualizar",
                    onClick = onRefreshClick,
                    icon = Icons.Outlined.Tune,
                    modifier = Modifier.weight(1f),
                    accent = PremiumAmber,
                )
                PremiumSecondaryButton(
                    text = "Abrir operação",
                    onClick = onOpenPublicSalesModeClick,
                    icon = Icons.Outlined.Storefront,
                    modifier = Modifier.weight(1f),
                    accent = PremiumBrand,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminEventSalesMetric("Pedidos", dashboard.orders.size.toString(), Icons.AutoMirrored.Outlined.ReceiptLong, Modifier.weight(1f))
            AdminEventSalesMetric("Itens", dashboard.totalItems.toString(), Icons.Outlined.Inventory2, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminEventSalesMetric("Aprovados", dashboard.approvedOrders.toString(), Icons.Outlined.CheckCircle, Modifier.weight(1f), PremiumBrand)
            AdminEventSalesMetric("Pendentes", dashboard.pendingOrders.toString(), Icons.Outlined.Payments, Modifier.weight(1f), PremiumAmber)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminEventSalesMetric("Receita total", dashboard.totalRevenueLabel, Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f), PremiumBrand)
            AdminEventSalesMetric("Em análise", dashboard.pendingRevenueLabel, Icons.Outlined.Payments, Modifier.weight(1f), PremiumAmber)
        }

        if (!dashboard.hasOperation) {
            PremiumEmptyState(
                title = "Nenhuma operação ativa",
                subtitle = "Ative o Modo Vendas em um evento para ver produtos, pedidos e métricas aqui.",
                icon = Icons.Outlined.Storefront,
                accent = PremiumAmber,
            )
            return@PremiumScreen
        }

        PremiumHeader(
            title = "Eventos com cardápio",
            subtitle = "Resumo de produtos por evento",
            icon = Icons.Outlined.EventAvailable,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            dashboard.events.forEach { event ->
                AdminEventSalesEventRow(event)
            }
        }

        PremiumHeader(
            title = "Pedidos do Modo Vendas",
            subtitle = "Comprovantes, aprovação e retirada",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        )
        PremiumTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar por evento, comprador, produto ou categoria",
            leadingIcon = Icons.Outlined.Search,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            statusFilters.forEach { status ->
                Box(modifier = Modifier.clickable { selectedStatus = status }) {
                    PremiumChip(
                        label = status,
                        accent = if (selectedStatus == status) PremiumAmber else PremiumZinc500,
                        filled = selectedStatus == status,
                    )
                }
            }
        }

        if (visibleOrders.isEmpty()) {
            PremiumEmptyState(
                title = "Sem pedidos neste filtro",
                subtitle = "Os pedidos de produtos do evento aparecem aqui quando `eventParty.eventId` estiver preenchido.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                accent = PremiumAmber,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleOrders.forEach { order -> AdminEventSalesOrderCard(order) }
            }
        }
    }
}

@Composable
private fun AdminEventSalesMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    PremiumCard(modifier = modifier, accent = accent) {
        Icon(icon, contentDescription = null, tint = accent)
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdminEventSalesEventRow(event: AdminEventSalesEvent) {
    PremiumCard(accent = PremiumAmber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                PremiumChip(label = event.menuTitle, icon = Icons.Outlined.Storefront, accent = PremiumAmber, filled = true)
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.category,
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PremiumChip(label = event.statusLabel, accent = PremiumBrand)
                Text(
                    text = "${event.productCount} produtos",
                    color = PremiumAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${event.stockCount} em estoque",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun AdminEventSalesOrderCard(order: AdminEventSalesOrder) {
    val accent = when (order.status) {
        AdminEventSalesOrderStatus.Approved,
        AdminEventSalesOrderStatus.Delivered,
        -> PremiumBrand
        AdminEventSalesOrderStatus.Rejected,
        AdminEventSalesOrderStatus.Cancelled,
        -> Color(0xFFEF4444)
        AdminEventSalesOrderStatus.Pending -> PremiumAmber
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PremiumZinc900)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.32f)), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "PEDIDO #${order.id.take(8).uppercase()}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = order.productName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${order.eventTitle} • ${order.category}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumChip(label = order.status.label, accent = accent, filled = order.status == AdminEventSalesOrderStatus.Pending)
                Text(
                    text = order.totalLabel,
                    color = accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PremiumInfoRow("Comprador", order.userName, modifier = Modifier.weight(1f), accent = accent)
            PremiumInfoRow("Qtd.", order.quantity.toString(), modifier = Modifier.weight(1f), accent = accent)
        }
        PremiumInfoRow("Comprovante para", order.receiverLabel, accent = accent)
        PremiumInfoRow("Aprovação", order.approvalLabel, accent = accent)
        PremiumInfoRow("Retirada/QR", order.voucherStatusLabel, accent = accent)
        PremiumInfoRow("Origem", order.sourceLabel, accent = accent)
        PremiumInfoRow("Criado em", order.createdAtLabel.ifBlank { "-" }, accent = accent)
    }
}
