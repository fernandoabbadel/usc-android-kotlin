package com.example.usc1.ui.generalorders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun OrdersHubScreen(
    state: GeneralOrdersUiState,
    onTypeClick: (GeneralOrderType) -> Unit,
    onOrderClick: (GeneralOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Pedidos", subtitle = "Eventos, loja e planos", icon = Icons.Outlined.ReceiptLong)
        NativeActionCard(NativeAction("Pedidos Eventos", "Ingressos, status e QR.", Icons.Outlined.Event), { onTypeClick(GeneralOrderType.Events) })
        NativeActionCard(NativeAction("Pedidos Loja", "Produtos, retirada e pagamento.", Icons.Outlined.Storefront), { onTypeClick(GeneralOrderType.Store) })
        NativeActionCard(NativeAction("Pedidos Planos", "Adesões e renovações.", Icons.Outlined.CreditCard), { onTypeClick(GeneralOrderType.Plans) })
        state.orders.forEach { order -> GeneralOrderCard(order = order, onClick = { onOrderClick(order) }) }
    }
}

@Composable
fun OrdersByTypeScreen(
    state: GeneralOrdersUiState,
    type: GeneralOrderType?,
    onStatusClick: (GeneralOrderStatus?) -> Unit,
    onOrderClick: (GeneralOrder) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orders = state.orders.filter { type == null || it.type == type }
        .filter { state.selectedStatus == null || it.status == state.selectedStatus }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = type?.label ?: "Todos", subtitle = "Pedidos por tipo", icon = Icons.Outlined.ReceiptLong, onBackClick = onBackClick)
        OrdersStatusTabs(selectedStatus = state.selectedStatus, onStatusClick = onStatusClick)
        orders.forEach { order -> GeneralOrderCard(order = order, onClick = { onOrderClick(order) }) }
    }
}

@Composable
fun GeneralOrderDetailScreen(order: GeneralOrder, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = order.id, subtitle = order.type.label, icon = Icons.Outlined.ReceiptLong, accent = orderStatusColor(order.status), onBackClick = onBackClick)
        PremiumCard(accent = orderStatusColor(order.status)) {
            PremiumChip(label = order.status.label, accent = orderStatusColor(order.status))
            Text(text = order.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = order.description, color = PremiumZinc400, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            PremiumInfoRow("Criado em", order.createdAtLabel, accent = orderStatusColor(order.status))
            PremiumInfoRow("Total", order.amountLabel, accent = orderStatusColor(order.status))
        }
    }
}

@Composable
fun OrdersStatusTabs(
    selectedStatus: GeneralOrderStatus?,
    onStatusClick: (GeneralOrderStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OrderStatusPill("Todos", selectedStatus == null, PremiumBrand) { onStatusClick(null) }
        GeneralOrderStatus.values().forEach { status ->
            OrderStatusPill(status.label, selectedStatus == status, orderStatusColor(status)) { onStatusClick(status) }
        }
    }
}

@Composable
fun GeneralOrderCard(order: GeneralOrder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, orderStatusColor(order.status).copy(alpha = 0.26f)),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PremiumChip(label = order.type.label, accent = orderStatusColor(order.status))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = order.id, color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = order.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(text = "${order.createdAtLabel} • ${order.amountLabel}", color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = orderStatusColor(order.status))
        }
    }
}

@Composable
private fun OrderStatusPill(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) accent else PremiumZinc800),
    ) {
        Text(text = label.uppercase(), color = if (selected) Color.Black else PremiumZinc400, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

fun orderStatusColor(status: GeneralOrderStatus): Color = when (status) {
    GeneralOrderStatus.Pending -> PremiumAmber
    GeneralOrderStatus.Approved -> PremiumBrand
    GeneralOrderStatus.Cancelled -> PremiumRed
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun OrdersHubScreenPreview() {
    UscTheme(darkTheme = true) {
        OrdersHubScreen(GeneralOrdersUiState(), {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun GeneralOrderDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        GeneralOrderDetailScreen(GeneralOrdersMockData.orders.first(), {})
    }
}
