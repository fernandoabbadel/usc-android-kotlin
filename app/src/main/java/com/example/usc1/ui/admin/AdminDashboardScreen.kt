package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.core.ui.PremiumPurple

@Composable
fun AdminDashboardScreen(
    state: AdminDashboardUiState,
    onModulesClick: () -> Unit,
    onPoliciesClick: () -> Unit,
    onUsersClick: () -> Unit,
    onStoreClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "CARREGANDO BASE...", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Visao Geral",
                subtitle = "Falha ao carregar dashboard admin",
                icon = Icons.Outlined.Home,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Dashboard indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Home,
            )
            PremiumPrimaryButton(
                text = "Tentar novamente",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
            )
        }
        else -> AdminDashboardLoadedContent(
            state = state,
            onModulesClick = onModulesClick,
            onPoliciesClick = onPoliciesClick,
            onUsersClick = onUsersClick,
            onStoreClick = onStoreClick,
            onRefreshClick = onRefreshClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminDashboardLoadedContent(
    state: AdminDashboardUiState,
    onModulesClick: () -> Unit,
    onPoliciesClick: () -> Unit,
    onUsersClick: () -> Unit,
    onStoreClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Visao Geral",
            subtitle = "Metricas e atividade em tempo real • ${state.tenantSigla.ifBlank { state.tenantName.ifBlank { "USC" } }}",
            icon = Icons.Outlined.Home,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Modulos do App",
                onClick = onModulesClick,
                icon = Icons.Outlined.Home,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Políticas",
                onClick = onPoliciesClick,
                icon = Icons.Outlined.Shield,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Usuários",
                onClick = onUsersClick,
                icon = Icons.Outlined.Groups,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Loja",
                onClick = onStoreClick,
                icon = Icons.Outlined.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
        }

        state.stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { stat ->
                    AdminStatCard(
                        stat = stat,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }

        PremiumCard {
            SectionTitle(
                title = "Novos cadastros",
                icon = Icons.Outlined.Shield,
                accent = PremiumBrand,
            )
            if (state.recentUsers.isEmpty()) {
                PremiumEmptyState(
                    title = "Sem cadastros recentes",
                    subtitle = "Nenhum usuário retornado pelo Supabase para este tenant.",
                    icon = Icons.Outlined.Groups,
                )
            } else {
                state.recentUsers.forEach { user ->
                    AdminRecentUserRow(user = user)
                }
            }
        }

        PremiumCard {
            SectionTitle(
                title = "Log do Sistema",
                icon = Icons.Outlined.History,
                accent = PremiumAmber,
            )
            if (state.recentActivity.isEmpty()) {
                Text(
                    text = "Nenhuma atividade recente.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                state.recentActivity.forEach { activity ->
                    PremiumInfoRow(
                        label = activity.timeLabel,
                        value = "${activity.userName} ${activity.action} em ${activity.resource}",
                        accent = PremiumBrand,
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
            icon = Icons.Outlined.ArrowBack,
        )
    }
}

@Composable
private fun AdminStatCard(
    stat: AdminStatUiModel,
    modifier: Modifier = Modifier,
) {
    val accent = statAccent(stat.kind)
    PremiumCard(
        modifier = modifier,
        accent = accent,
        containerColor = PremiumZinc900,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black,
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Icon(
                    imageVector = statIcon(stat.kind),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = accent,
                )
            }
            PremiumChip(label = stat.trend, accent = accent)
        }
        Text(
            text = stat.title.uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stat.value,
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdminRecentUserRow(user: AdminRecentUserUiModel) {
    PremiumCard(
        accent = PremiumZinc800,
        containerColor = Color.Black.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = PremiumBrand.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.36f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = PremiumBrand,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${user.className} - ${user.role}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PremiumChip(label = user.createdLabel, accent = PremiumZinc400)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = accent,
        )
        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun statIcon(kind: AdminStatKind): ImageVector = when (kind) {
    AdminStatKind.Users -> Icons.Outlined.Groups
    AdminStatKind.Events -> Icons.Outlined.CalendarMonth
    AdminStatKind.Sales -> Icons.Outlined.ShoppingBag
    AdminStatKind.Engagement -> Icons.Outlined.TrendingUp
}

private fun statAccent(kind: AdminStatKind): Color = when (kind) {
    AdminStatKind.Users -> PremiumBrand
    AdminStatKind.Events -> Color(0xFF3B82F6)
    AdminStatKind.Sales -> PremiumPurple
    AdminStatKind.Engagement -> PremiumAmber
}
