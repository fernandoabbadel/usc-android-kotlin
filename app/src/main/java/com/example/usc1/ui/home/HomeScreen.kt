package com.example.usc1.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R
import com.example.usc1.navigation.AppRoute
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
        color = HomeBlack,
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
    val membershipAction = state.actionOrFallback(
        kind = QuickActionKind.MembershipCard,
        title = "Carteirinha",
        subtitle = "Status e QR",
        route = AppRoute.MembershipCard,
    )
    val eventsAction = state.actionOrFallback(
        kind = QuickActionKind.Events,
        title = "Eventos",
        subtitle = "Ingressos e festas",
        route = AppRoute.Events,
    )
    val storeAction = state.actionOrFallback(
        kind = QuickActionKind.Store,
        title = "Modo vendas",
        subtitle = "Menu do evento",
        route = AppRoute.Store,
    )
    val profileAction = state.actionOrFallback(
        kind = QuickActionKind.Profile,
        title = "Perfil",
        subtitle = "Dados e histórico",
        route = AppRoute.Profile,
    )
    val menuModule = HomeModuleUiModel(
        title = "Menu",
        description = "Configurações e atalhos do app.",
        route = AppRoute.Settings,
        kind = QuickActionKind.Profile,
    )
    val scannerModule = HomeModuleUiModel(
        title = "Scanner",
        description = "Leitura e validação de QR Codes.",
        route = "scanner",
        kind = QuickActionKind.Events,
    )
    val firstEvent = state.upcomingEvents.firstOrNull()
    val leagueModule = state.mainModules.firstOrNull { it.kind == QuickActionKind.Leagues }
    val trainingAction = state.actionOrFallback(
        kind = QuickActionKind.Training,
        title = "Treinos",
        subtitle = "Agenda e presença",
        route = AppRoute.Training,
    )
    val plansModule = state.mainModules.firstOrNull { it.route == AppRoute.Plans }
        ?: HomeModuleUiModel(
            title = "Planos",
            description = "Plano ativo, adesões e benefícios.",
            route = AppRoute.Plans,
            kind = QuickActionKind.Profile,
        )
    val partnersModule = state.mainModules.firstOrNull { it.route == AppRoute.Partners }
        ?: HomeModuleUiModel(
            title = "Parceiros",
            description = "Empresas, cupons e descontos.",
            route = AppRoute.Partners,
            kind = QuickActionKind.Community,
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HomeBlack,
                        Color.Black,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            HomeBrand.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        center = androidx.compose.ui.geometry.Offset(160f, 90f),
                        radius = 460f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 126.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DashboardHeader(
                firstName = state.userName.firstName(),
                tenantName = state.tenantName,
                onAvatarClick = { onQuickActionClick(profileAction) },
            )

            SalesModeCard(
                eventTitle = firstEvent?.title ?: "Evento ativo",
                onClick = { onQuickActionClick(storeAction) },
            )

            PremiumDashboardCard(
                title = "Loja",
                eyebrow = "Drop oficial",
                body = "Produtos, carrinho e pedidos da atlética.",
                icon = Icons.Outlined.Storefront,
                accent = HomeBrandAccent,
                backgroundImageRes = R.drawable.logo_usc_wide,
                onClick = { onQuickActionClick(storeAction) },
            )

            MembershipHomeCard(
                membershipCode = state.membershipCode,
                planName = state.planName,
                tenantName = state.tenantName,
                onClick = { onQuickActionClick(membershipAction) },
            )

            PremiumDashboardCard(
                title = plansModule.title,
                eyebrow = state.planName,
                body = plansModule.description,
                icon = Icons.Outlined.CreditCard,
                accent = HomeGold,
                backgroundImageRes = R.drawable.carteirinha_bg,
                onClick = { onModuleClick(plansModule) },
            )

            PremiumDashboardCard(
                title = "BoardRound",
                eyebrow = "Em breve",
                body = "Ranking, jogos e rivalidade da base.",
                icon = Icons.Outlined.Star,
                accent = HomeBrand,
                backgroundImageRes = R.drawable.logo_platform_web,
                onClick = {
                    onModuleClick(
                        HomeModuleUiModel(
                            title = "BoardRound",
                            description = "Ranking, estatísticas e quizzes.",
                            route = "boardround",
                            kind = QuickActionKind.Community,
                        ),
                    )
                },
            )

            PremiumDashboardCard(
                title = trainingAction.title,
                eyebrow = "Área do atleta",
                body = trainingAction.subtitle,
                icon = Icons.Outlined.FitnessCenter,
                accent = HomeAmber,
                onClick = { onQuickActionClick(trainingAction) },
            )

            PremiumDashboardCard(
                title = partnersModule.title,
                eyebrow = "Benefícios ativos",
                body = partnersModule.description,
                icon = Icons.Outlined.Groups,
                accent = HomeBrand,
                backgroundImageRes = R.drawable.logo_aaakn,
                onClick = { onModuleClick(partnersModule) },
            )

            RadarAlbumCard(
                foundCount = 42,
                totalCount = 96,
                onClick = { onModuleClick(scannerModule) },
            )

            DashboardSectionTitle(
                title = "Eventos",
                icon = Icons.Outlined.Event,
            )

            PremiumDashboardCard(
                title = firstEvent?.title ?: "Intermed USC",
                eyebrow = firstEvent?.status ?: "Vendas abertas",
                body = firstEvent?.let { "${it.dateLabel} • ${it.location}" }
                    ?: "Ginásio principal • Sábado, 18:00",
                icon = Icons.Outlined.Event,
                accent = HomeBrandAccent,
                backgroundImageRes = R.drawable.battle_forest,
                onClick = { onQuickActionClick(eventsAction) },
            )

            if (leagueModule != null) {
                DashboardSectionTitle(
                    title = "Central USC",
                    icon = Icons.Outlined.Groups,
                    accent = HomeGold,
                )
                PremiumDashboardCard(
                    title = leagueModule.title,
                    eyebrow = "Ligas acadêmicas",
                    body = leagueModule.description,
                    icon = iconFor(leagueModule.kind),
                    accent = HomeGold,
                    onClick = { onModuleClick(leagueModule) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        FloatingBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHomeClick = {},
            onEventsClick = { onQuickActionClick(eventsAction) },
            onScannerClick = { onModuleClick(scannerModule) },
            onWalletClick = { onQuickActionClick(membershipAction) },
            onMenuClick = { onModuleClick(menuModule) },
        )
    }
}

@Composable
private fun HomeLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = HomeBrand)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Carregando sua atlética...",
            color = HomeZinc400,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
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
            .background(HomeBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = Color(0xFFFCA5A5),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeBrand,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = "Tentar novamente",
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun HomeUiState.actionOrFallback(
    kind: QuickActionKind,
    title: String,
    subtitle: String,
    route: String,
): QuickActionUiModel =
    quickActions.firstOrNull { it.kind == kind }
        ?: QuickActionUiModel(
            title = title,
            subtitle = subtitle,
            route = route,
            kind = kind,
        )

private fun String.firstName(): String =
    trim()
        .split(" ")
        .firstOrNull { it.isNotBlank() }
        ?.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        ?: "Fernando"

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun HomeScreenPreview() {
    UscTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun HomeScreenLoadingPreview() {
    UscTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState.loading(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun HomeScreenErrorPreview() {
    UscTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState.error(),
            onQuickActionClick = {},
            onModuleClick = {},
            onRetryClick = {},
        )
    }
}
