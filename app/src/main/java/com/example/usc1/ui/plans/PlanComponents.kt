package com.example.usc1.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumGold
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun PlanCard(
    plan: UscPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(286.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(PremiumZinc900)
            .border(1.dp, planAccent(plan).copy(alpha = 0.32f), RoundedCornerShape(30.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = plan.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.48f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumChip(label = plan.subtitle, accent = planAccent(plan), filled = plan.status == PlanStatus.Active)
                    PlanStatusChip(status = plan.status)
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = Color.Black,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = plan.name,
                    color = Color.White,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = plan.description,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PLANO",
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = plan.priceLabel,
                            color = planAccent(plan),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        text = plan.accentName.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
fun PlanBenefitChip(
    benefit: PlanBenefit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    PremiumChip(
        label = benefit.title,
        modifier = modifier,
        icon = if (benefit.highlighted) Icons.Outlined.Star else Icons.Outlined.CheckCircle,
        accent = if (benefit.highlighted) PremiumGold else accent,
        filled = benefit.highlighted,
    )
}

@Composable
fun PlanStatusChip(
    status: PlanStatus,
    modifier: Modifier = Modifier,
) {
    val icon = when (status) {
        PlanStatus.Active -> Icons.Outlined.CheckCircle
        PlanStatus.Available -> Icons.Outlined.CreditCard
        PlanStatus.Locked -> Icons.Outlined.Lock
    }
    PremiumChip(
        label = status.label,
        icon = icon,
        accent = planStatusColor(status),
        modifier = modifier,
    )
}

@Composable
fun PlanOrderCard(
    order: PlanOrder,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, planOrderColor(order.status).copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = planOrderColor(order.status).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, planOrderColor(order.status).copy(alpha = 0.34f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = planOrderColor(order.status),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = order.id,
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.planName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.createdAtLabel,
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = order.amountLabel,
                    color = planOrderColor(order.status),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.status.label.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

fun planAccent(plan: UscPlan): Color = when (plan.id) {
    "atleta" -> PremiumGold
    "lenda" -> PremiumPurple
    "bicho-solto" -> PremiumRed
    else -> PremiumBrand
}

fun planStatusColor(status: PlanStatus): Color = when (status) {
    PlanStatus.Active -> PremiumBrand
    PlanStatus.Available -> PremiumGold
    PlanStatus.Locked -> PremiumRed
}

fun planOrderColor(status: PlanOrderStatus): Color = when (status) {
    PlanOrderStatus.Pending -> PremiumAmber
    PlanOrderStatus.Approved -> PremiumBrand
    PlanOrderStatus.Cancelled -> PremiumRed
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun PlanCardPreview() {
    UscTheme(darkTheme = true) {
        PlanCard(
            plan = PlansMockData.plans.first(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
