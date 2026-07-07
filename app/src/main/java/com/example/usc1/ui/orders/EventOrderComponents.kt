package com.example.usc1.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.data.repository.MockEventOrdersRepository
import com.example.usc1.domain.model.EventOrder
import com.example.usc1.domain.model.OrderStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun EventOrderCard(
    order: EventOrder,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, orderStatusColor(order.status).copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "PEDIDO #${order.id.take(8).uppercase()}",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = order.eventTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${order.quantity} ingresso(s) • ${order.lotName}",
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OrderStatusChip(status = order.status)
            }
            Text(
                text = "${order.paymentStatus.label} • ${order.approvalStatus}",
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "TOTAL",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = order.amountLabel,
                        color = orderStatusColor(order.status),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                PremiumSecondaryButton(
                    text = "Detalhes",
                    onClick = onDetailsClick,
                    icon = Icons.Outlined.ArrowForward,
                    modifier = Modifier.weight(0.9f),
                    accent = orderStatusColor(order.status),
                )
            }
        }
    }
}

@Composable
fun OrderStatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    PremiumChip(
        label = status.label,
        modifier = modifier,
        icon = Icons.Outlined.ReceiptLong,
        accent = orderStatusColor(status),
    )
}

fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.Pending -> PremiumAmber
    OrderStatus.Approved -> PremiumBrand
    OrderStatus.Cancelled,
    OrderStatus.Rejected,
    -> PremiumRed
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun EventOrderCardPreview() {
    UscTheme(darkTheme = true) {
        EventOrderCard(
            order = MockEventOrdersRepository.mockOrders.first(),
            onDetailsClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
