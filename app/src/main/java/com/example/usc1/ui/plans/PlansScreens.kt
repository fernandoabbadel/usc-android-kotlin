@file:OptIn(ExperimentalLayoutApi::class)

package com.example.usc1.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumImageCard
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.ui.theme.UscTheme

@Composable
fun PlansScreen(
    state: PlanUiState,
    onPlanClick: (UscPlan) -> Unit,
    onStatusClick: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Planos",
            subtitle = "Benefícios, adesões e status do sócio",
            icon = Icons.Outlined.CreditCard,
        )

        ActivePlanCard(
            status = state.activePlan,
            onStatusClick = onStatusClick,
            onOrdersClick = onOrdersClick,
        )

        Text(
            text = "PLANOS DISPONÍVEIS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 2.dp),
        )
        state.plans.forEach { plan ->
            PlanCard(
                plan = plan,
                onClick = { onPlanClick(plan) },
            )
        }
    }
}

@Composable
fun PlanDetailScreen(
    plan: UscPlan,
    onSubscribeClick: (UscPlan) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = planAccent(plan)
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = plan.name,
            subtitle = plan.subtitle,
            icon = Icons.Outlined.Star,
            accent = accent,
            onBackClick = onBackClick,
        )

        PremiumImageCard(
            imageRes = plan.imageRes,
            height = 294.dp,
            accent = accent,
            imageAlpha = 0.70f,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlanStatusChip(status = plan.status)
                Text(
                    text = plan.name.uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = plan.priceLabel,
                    color = accent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        PremiumCard(accent = accent) {
            Text(
                text = plan.description,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                plan.benefits.forEach { benefit ->
                    PlanBenefitChip(benefit = benefit, accent = accent)
                }
            }
            PremiumInfoRow(label = "Identidade", value = plan.accentName, accent = accent)
            PremiumInfoRow(label = "Status", value = plan.status.label, accent = accent)
        }

        PremiumPrimaryButton(
            text = if (plan.status == PlanStatus.Locked) "Acesso por convite" else "Solicitar plano",
            onClick = { onSubscribeClick(plan) },
            enabled = plan.status != PlanStatus.Locked,
            icon = Icons.Outlined.CreditCard,
            accent = accent,
        )
    }
}

@Composable
fun UserPlanStatusScreen(
    state: PlanUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Plano Ativo",
            subtitle = "Carteirinha, renovação e benefícios",
            icon = Icons.Outlined.AccountBalanceWallet,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            PremiumChip(label = state.activePlan.statusLabel, icon = Icons.Outlined.CheckCircle, accent = PremiumBrand, filled = true)
            Text(
                text = state.activePlan.planName,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = state.activePlan.memberSince,
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumInfoRow(label = "Renovação", value = state.activePlan.renewalLabel)
            PremiumInfoRow(label = "Status", value = state.activePlan.statusLabel)
        }

        val activePlan = state.plans.firstOrNull { it.status == PlanStatus.Active }
        if (activePlan != null) {
            PremiumCard {
                Text(
                    text = "BENEFÍCIOS ATIVOS",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activePlan.benefits.forEach { benefit ->
                        PlanBenefitChip(benefit = benefit)
                    }
                }
            }
        }
    }
}

@Composable
fun PlanOrdersScreen(
    state: PlanUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Pedidos Planos",
            subtitle = "Histórico de adesões mockadas",
            icon = Icons.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )

        state.orders.forEach { order ->
            PlanOrderCard(order = order)
        }
    }
}

@Composable
private fun ActivePlanCard(
    status: UserPlanStatus,
    onStatusClick: () -> Unit,
    onOrdersClick: () -> Unit,
) {
    PremiumCard(accent = PremiumBrand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                PremiumChip(label = status.statusLabel, icon = Icons.Outlined.CheckCircle, accent = PremiumBrand, filled = true)
                Text(
                    text = status.planName,
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${status.memberSince} • ${status.renewalLabel}",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Status",
                onClick = onStatusClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountBalanceWallet,
            )
            PremiumSecondaryButton(
                text = "Pedidos",
                onClick = onOrdersClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.ReceiptLong,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun PlansScreenPreview() {
    UscTheme(darkTheme = true) {
        PlansScreen(
            state = PlanUiState(),
            onPlanClick = {},
            onStatusClick = {},
            onOrdersClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun PlanDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        PlanDetailScreen(
            plan = PlansMockData.plans.first(),
            onSubscribeClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun UserPlanStatusScreenPreview() {
    UscTheme(darkTheme = true) {
        UserPlanStatusScreen(
            state = PlanUiState(),
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun PlanOrdersScreenPreview() {
    UscTheme(darkTheme = true) {
        PlanOrdersScreen(
            state = PlanUiState(),
            onBackClick = {},
        )
    }
}
