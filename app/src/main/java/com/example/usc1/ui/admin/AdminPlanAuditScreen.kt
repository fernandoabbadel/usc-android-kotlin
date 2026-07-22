package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc900

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminPlanAuditScreen(
    state: AdminPlanAuditUiState,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = "Auditoria",
            subtitle = "Conferência de fluxo dos planos",
            icon = Icons.Outlined.Security,
            accent = PremiumBrandAccent,
            onBackClick = onBackClick,
        )

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuditMetricCard("Solicitações pendentes", state.pendingRequests, Color(0xFFFACC15), Modifier.weight(1f))
            AuditMetricCard("Solicitações aprovadas", state.approvedRequests, PremiumBrandAccent, Modifier.weight(1f))
            AuditMetricCard("Solicitações rejeitadas", state.rejectedRequests, Color(0xFFF87171), Modifier.weight(1f))
            AuditMetricCard("Assinaturas ativas", state.activeSubscriptions, Color(0xFF60A5FA), Modifier.weight(1f))
            AuditMetricCard("Assinaturas pendentes", state.pendingSubscriptions, Color(0xFFFB923C), Modifier.weight(1f))
            PremiumCard(accent = PremiumBrandAccent, containerColor = PremiumZinc900, modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "Status geral",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = PremiumBrandAccent,
                        )
                        Text(
                            text = "OK",
                            color = PremiumBrandAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetricCard(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    PremiumCard(accent = color, containerColor = PremiumZinc900, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value.toString(),
                color = color,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Planos",
                color = PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
