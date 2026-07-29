package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500

enum class AdminBiSnapshotFocus(
    val label: String,
    val description: String,
    val icon: ImageVector,
) {
    Events("Eventos", "Criação, calendário e participação", Icons.Outlined.CalendarMonth),
    Store("Loja", "Pedidos, produtos e venda do tenant", Icons.Outlined.ShoppingBag),
    Training("Treinos", "Operação esportiva e frequência", Icons.Outlined.Groups),
    Finance("Financeiro", "Receita, vendas e pendências", Icons.Outlined.PointOfSale),
    Commercial("BI Comercial", "Vendas, loja, planos e conversão", Icons.Outlined.TrendingUp),
    Operational("BI Operacional", "Base, eventos, usuários e execução", Icons.Outlined.BarChart),
    Gate("BI Portaria", "Entrada, validação e controle", Icons.Outlined.Shield),
}

@Composable
fun AdminBiSnapshotScreen(
    state: AdminDashboardUiState,
    focus: AdminBiSnapshotFocus,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando ${focus.label}", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier, bottomPadding = 110.dp) {
            PremiumHeader(
                title = focus.label,
                subtitle = "Falha ao carregar dados reais",
                icon = focus.icon,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "BI indisponível",
                subtitle = state.errorMessage,
                icon = focus.icon,
            )
            PremiumSecondaryButton(
                text = "Tentar novamente",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
            )
        }
        else -> AdminBiSnapshotLoaded(
            state = state,
            focus = focus,
            onRefreshClick = onRefreshClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminBiSnapshotLoaded(
    state: AdminDashboardUiState,
    focus: AdminBiSnapshotFocus,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
) {
    val tenant = state.tenantSigla.ifBlank { state.tenantName.ifBlank { "USC" } }
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp) {
        PremiumHeader(
            title = focus.label,
            subtitle = "${focus.description} • $tenant",
            icon = focus.icon,
            onBackClick = onBackClick,
        )
        PremiumCard(accent = focus.accent) {
            PremiumChip(label = "Dados reais Supabase", icon = focus.icon, accent = focus.accent, filled = true)
            androidx.compose.material3.Text(
                text = focus.executiveSummary(state),
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        state.stats.chunked(2).forEach { rowStats ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowStats.forEach { stat ->
                    BiStatCard(stat = stat, modifier = Modifier.weight(1f))
                }
                if (rowStats.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
        PremiumCard(accent = PremiumBrand) {
            PremiumChip(label = "Novos cadastros", icon = Icons.Outlined.Groups, accent = PremiumBrand)
            if (state.recentUsers.isEmpty()) {
                androidx.compose.material3.Text(
                    text = "Nenhum cadastro recente retornado pelo Supabase.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                state.recentUsers.take(5).forEach { user ->
                    PremiumInfoRow(
                        label = user.name,
                        value = "${user.className} • ${user.role} • ${user.createdLabel}",
                        accent = PremiumBrand,
                    )
                }
            }
        }
        PremiumCard(accent = PremiumAmber) {
            PremiumChip(label = "Log do sistema", icon = Icons.Outlined.BarChart, accent = PremiumAmber)
            if (state.recentActivity.isEmpty()) {
                androidx.compose.material3.Text(
                    text = "Nenhuma atividade recente registrada.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                state.recentActivity.take(5).forEach { activity ->
                    PremiumInfoRow(
                        label = activity.timeLabel,
                        value = "${activity.userName} ${activity.action} em ${activity.resource}",
                        accent = PremiumAmber,
                    )
                }
            }
        }
        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
        )
    }
}

@Composable
private fun BiStatCard(
    stat: AdminStatUiModel,
    modifier: Modifier = Modifier,
) {
    PremiumCard(
        modifier = modifier,
        accent = stat.kind.accent,
    ) {
        PremiumChip(label = stat.trend, icon = stat.kind.icon, accent = stat.kind.accent)
        androidx.compose.material3.Text(
            text = stat.title.uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        androidx.compose.material3.Text(
            text = stat.value,
            color = Color.White,
            fontSize = 25.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private val AdminBiSnapshotFocus.accent: Color
    get() = when (this) {
        AdminBiSnapshotFocus.Events -> Color(0xFF3B82F6)
        AdminBiSnapshotFocus.Store,
        AdminBiSnapshotFocus.Finance,
        AdminBiSnapshotFocus.Commercial -> PremiumPurple
        AdminBiSnapshotFocus.Training,
        AdminBiSnapshotFocus.Operational -> PremiumBrand
        AdminBiSnapshotFocus.Gate -> PremiumAmber
    }

private val AdminStatKind.icon: ImageVector
    get() = when (this) {
        AdminStatKind.Users -> Icons.Outlined.Groups
        AdminStatKind.Events -> Icons.Outlined.CalendarMonth
        AdminStatKind.Sales -> Icons.Outlined.ShoppingBag
        AdminStatKind.Engagement -> Icons.Outlined.TrendingUp
    }

private val AdminStatKind.accent: Color
    get() = when (this) {
        AdminStatKind.Users -> PremiumBrand
        AdminStatKind.Events -> Color(0xFF3B82F6)
        AdminStatKind.Sales -> PremiumPurple
        AdminStatKind.Engagement -> PremiumAmber
    }

private fun AdminBiSnapshotFocus.executiveSummary(state: AdminDashboardUiState): String {
    val users = state.stats.firstOrNull { it.kind == AdminStatKind.Users }?.value ?: "--"
    val events = state.stats.firstOrNull { it.kind == AdminStatKind.Events }?.value ?: "--"
    val sales = state.stats.firstOrNull { it.kind == AdminStatKind.Sales }?.value ?: "--"
    return when (this) {
        AdminBiSnapshotFocus.Events -> "Eventos cadastrados: $events. Use esta visão para acompanhar volume de eventos, cadastros recentes e atividade operacional."
        AdminBiSnapshotFocus.Store -> "Vendas da loja: $sales. Esta visão cruza loja, pedidos e atividade recente do tenant."
        AdminBiSnapshotFocus.Training -> "Base ativa: $users atletas. Use com os logs recentes para avaliar operação esportiva e engajamento."
        AdminBiSnapshotFocus.Finance -> "Receita consolidada retornada pelo Supabase: $sales. Pendências aparecem nos módulos de loja, planos e mini-vendor."
        AdminBiSnapshotFocus.Commercial -> "Comercial resume vendas, eventos e cadastros: $sales em loja, $events eventos e $users atletas."
        AdminBiSnapshotFocus.Operational -> "Operacional resume execução do tenant: $users atletas, $events eventos e atividade recente do sistema."
        AdminBiSnapshotFocus.Gate -> "Portaria usa os mesmos dados vivos para apoiar validação, entrada e leitura de movimento recente."
    }
}
