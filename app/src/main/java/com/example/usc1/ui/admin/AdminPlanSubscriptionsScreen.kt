package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminPlanSubscription
import com.example.usc1.domain.model.AdminPlanSubscriptionStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminPlanSubscriptionsScreen(
    state: AdminPlanSubscriptionsUiState,
    onRefreshClick: () -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.rows.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = state.kind.title,
            subtitle = "Paginado 20 em 20",
            icon = Icons.Outlined.Groups,
            accent = PremiumBrandAccent,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Voltar",
                onClick = onBackClick,
                icon = Icons.Outlined.ArrowBack,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = if (state.isRefreshing) "Atualizando" else "Atualizar",
                onClick = onRefreshClick,
                enabled = !state.isRefreshing,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
            )
        }

        state.errorMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFCA5A5).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.28f)),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (state.pagedRows.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhuma assinatura encontrada.",
                subtitle = "A consulta do tenant ativo não retornou assinaturas para este plano.",
                icon = Icons.Outlined.Groups,
            )
        } else {
            state.pagedRows.forEach { row ->
                PlanSubscriptionRow(row)
            }
        }

        if (state.rows.size > AdminPlanSubscriptionsUiState.PageSize) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Página ${state.page} de ${state.totalPages}",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Anterior",
                    onClick = onPreviousPageClick,
                    enabled = state.canGoPrevious,
                    icon = Icons.Outlined.ChevronLeft,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Próxima",
                    onClick = onNextPageClick,
                    enabled = state.canGoNext,
                    icon = Icons.Outlined.ChevronRight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlanSubscriptionRow(row: AdminPlanSubscription) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = PremiumBrandAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PremiumZinc700),
                ) {
                    Text(
                        text = row.aluno.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = PremiumBrandAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = row.aluno.ifBlank { "Aluno" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.turma.ifBlank { "-" }.uppercase(),
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.dataInicio.isNotBlank()) {
                    Text(
                        text = row.dataInicio,
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = row.planoNome.ifBlank { row.planoId }.uppercase(),
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatter.format(row.valorPago),
                    color = PremiumBrandAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                PremiumChip(label = row.status.label, accent = planSubscriptionStatusColor(row.status))
            }
        }
    }
}

private fun planSubscriptionStatusColor(status: AdminPlanSubscriptionStatus): Color {
    return when (status) {
        AdminPlanSubscriptionStatus.Ativo -> PremiumBrandAccent
        AdminPlanSubscriptionStatus.Vencido -> Color(0xFFF87171)
        AdminPlanSubscriptionStatus.Pendente -> Color(0xFFFACC15)
    }
}
