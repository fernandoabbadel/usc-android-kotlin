package com.example.usc1.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.AppSectionHeader
import com.example.usc1.core.ui.InfoChip
import com.example.usc1.ui.theme.UscTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQuickActionClick: (QuickActionUiModel) -> Unit,
    onModuleClick: (HomeModuleUiModel) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            state.isLoading -> HomeLoadingContent()
            state.errorMessage != null -> HomeErrorContent(
                message = state.errorMessage,
                onRetryClick = onRetryClick,
            )
            else -> HomeLoadedContent(
                state = state,
                onQuickActionClick = onQuickActionClick,
                onModuleClick = onModuleClick,
            )
        }
    }
}

@Composable
private fun HomeLoadedContent(
    state: HomeUiState,
    onQuickActionClick: (QuickActionUiModel) -> Unit,
    onModuleClick: (HomeModuleUiModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Olá, ${state.userName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = state.tenantName)
                InfoChip(label = state.accountStatus)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardSummaryCard(
                title = "Plano",
                value = state.planName,
                supportingText = "Carteirinha ${state.membershipCode}",
                modifier = Modifier.weight(1f),
            )
            DashboardSummaryCard(
                title = "Pedidos",
                value = "${state.orderSummary.pendingOrders} pendentes",
                supportingText = "${state.orderSummary.activeTickets} ingressos ativos",
                modifier = Modifier.weight(1f),
            )
        }

        AppSectionHeader(
            title = "Acesso rápido",
            subtitle = "Principais áreas para o dia a dia da atlética.",
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.quickActions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowActions.forEach { action ->
                        QuickActionCard(
                            action = action,
                            onClick = { onQuickActionClick(action) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        AppSectionHeader(title = "Próximos eventos")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.upcomingEvents.forEach { event ->
                HomeEventCard(event = event)
            }
        }

        AppSectionHeader(
            title = "Pedidos e ingressos",
            subtitle = state.orderSummary.lastOrderLabel,
        )

        AppSectionHeader(title = "Módulos principais")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.mainModules.forEach { module ->
                HomeModuleCard(
                    module = module,
                    onClick = { onModuleClick(module) },
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Carregando sua atlética...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeErrorContent(
    message: String,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onRetryClick) {
            Text("Tentar novamente")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    UscTheme {
        HomeScreen(
            state = HomeUiState(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenLoadingPreview() {
    UscTheme {
        HomeScreen(
            state = HomeUiState.loading(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenErrorPreview() {
    UscTheme {
        HomeScreen(
            state = HomeUiState.error(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}
