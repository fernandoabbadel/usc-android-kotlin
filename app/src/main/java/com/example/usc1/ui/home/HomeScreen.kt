package com.example.usc1.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.navigation.AppRoute

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeTenantTheme(tenantContext = state.tenantContext) {
        var isDrawerOpen by rememberSaveable { mutableStateOf(false) }
        BackHandler(enabled = isDrawerOpen) { isDrawerOpen = false }

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
                    onNavigate = onNavigate,
                    onOpenDrawer = { isDrawerOpen = true },
                )
            }

            HomeSideDrawer(
                visible = isDrawerOpen,
                state = state,
                onDismiss = { isDrawerOpen = false },
                onNavigate = onNavigate,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
private fun HomeLoadedContent(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val dashboard = state.dashboard
    val goldPartners = dashboard.partners.filter { it.tier == "ouro" }
    val silverPartners = dashboard.partners.filter { it.tier == "prata" }
    val standardPartners = dashboard.partners.filterNot { it.tier == "ouro" || it.tier == "prata" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HomeBlack, Color.Black),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 24.dp,
                end = 20.dp,
                bottom = 164.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            item(key = "header") {
                DashboardHeader(
                    firstName = state.userName.firstName(),
                    avatarUrl = state.userAvatarUrl,
                    onAvatarClick = { onNavigate(AppRoute.Profile) },
                )
            }

            if (dashboard.isModuleVisible("parceiros") && (goldPartners.isNotEmpty() || silverPartners.isNotEmpty())) {
                item(key = "premium-partners") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DashboardSectionHeader(
                            title = "Parceiros Premium",
                            icon = Icons.Outlined.Handshake,
                            accent = HomeGold,
                            onViewAll = { onNavigate(AppRoute.Partners) },
                        )
                        if (goldPartners.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(goldPartners, key = { it.id }) { partner ->
                                    DashboardPartnerCard(
                                        partner = partner,
                                        modifier = Modifier.fillParentMaxWidth(),
                                        premium = true,
                                        onClick = { onNavigate(AppRoute.partnerDetail(partner.id)) },
                                    )
                                }
                            }
                        }
                        if (silverPartners.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                color = HomeZinc900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, HomeZinc800),
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Text(
                                        text = "PARCEIROS PRATA",
                                        color = HomeZinc400,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(silverPartners, key = { it.id }) { partner ->
                                            DashboardPartnerCard(
                                                partner = partner,
                                                onClick = { onNavigate(AppRoute.partnerDetail(partner.id)) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            dashboard.activeSalesEvent?.let { salesEvent ->
                item(key = "sales-mode") {
                    SalesModeCard(
                        eventTitle = salesEvent.title,
                        menuTitle = salesEvent.menuTitle,
                        imageUrl = salesEvent.imageUrl,
                        onClick = { onNavigate(AppRoute.eventDetail(salesEvent.id)) },
                    )
                }
            }

            if (dashboard.isModuleVisible("carteirinha")) {
                item(key = "membership") {
                    MembershipHomeCard(
                        membershipCode = state.membershipCode,
                        planName = state.planName,
                        className = state.className,
                        memberStatus = state.accountStatus,
                        backgroundImageRes = turmaDashboardImage(state.className),
                        onClick = { onNavigate(AppRoute.MembershipCard) },
                    )
                }
            }

            if (dashboard.isModuleVisible("sharkround") || dashboard.isModuleVisible("treinos")) {
                item(key = "boardround-training") {
                    DashboardFeatureGrid(
                        trainingImageUrls = dashboard.trainingImageUrls,
                        showBoardround = dashboard.isModuleVisible("sharkround"),
                        showTraining = dashboard.isModuleVisible("treinos"),
                        onBoardroundClick = { onNavigate(AppRoute.Boardround) },
                        onTrainingClick = { onNavigate(AppRoute.Training) },
                    )
                }
            }

            if (dashboard.isModuleVisible("album")) {
                item(key = "freshmen-hunt") {
                    RadarAlbumCard(
                        foundCount = dashboard.capturedFreshmen,
                        totalCount = dashboard.totalMembers,
                        onClick = { onNavigate(AppRoute.Album) },
                    )
                }
            }

            if (dashboard.isModuleVisible("eventos") && dashboard.events.isNotEmpty()) {
                item(key = "events") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DashboardSectionHeader(
                            title = "Eventos",
                            icon = Icons.Outlined.CalendarMonth,
                            onViewAll = { onNavigate(AppRoute.Events) },
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(dashboard.events, key = { it.id }) { event ->
                                DashboardEventCard(
                                    event = event,
                                    modifier = Modifier.fillParentMaxWidth(),
                                    onClick = { onNavigate(AppRoute.eventDetail(event.id)) },
                                )
                            }
                        }
                    }
                }
            }

            if (dashboard.isModuleVisible("ligas") && dashboard.leagues.isNotEmpty()) {
                item(key = "leagues") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DashboardSectionHeader(
                            title = "Ligas Acadêmicas",
                            icon = Icons.Outlined.Groups,
                            accent = HomeGold,
                            onViewAll = { onNavigate(AppRoute.Leagues) },
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(dashboard.leagues, key = { it.id }) { league ->
                                DashboardLeagueCard(
                                    league = league,
                                    onClick = { onNavigate(AppRoute.leagueDetail(league.id)) },
                                )
                            }
                        }
                    }
                }
            }

            if (dashboard.isModuleVisible("loja")) {
                item(key = "store") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DashboardSectionHeader(
                            title = "Lojinha",
                            icon = Icons.Outlined.ShoppingBag,
                            accent = Color(0xFFA855F7),
                            onViewAll = { onNavigate(AppRoute.Store) },
                        )
                        if (dashboard.products.isEmpty()) {
                            DashboardEmptyStoreCard { onNavigate(AppRoute.Store) }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(dashboard.products, key = { it.id }) { product ->
                                    DashboardProductCard(
                                        product = product,
                                        modifier = Modifier.fillParentMaxWidth(),
                                        onClick = { onNavigate(AppRoute.productDetail(product.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (dashboard.isModuleVisible("parceiros") && standardPartners.isNotEmpty()) {
                item(key = "standard-partners") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = HomeZinc900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HomeZinc800),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            DashboardSectionHeader(
                                title = "Parceiros Standard",
                                icon = Icons.Outlined.Groups,
                                accent = HomeZinc500,
                                onViewAll = { onNavigate(AppRoute.Partners) },
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(standardPartners, key = { it.id }) { partner ->
                                    DashboardPartnerCard(
                                        partner = partner,
                                        onClick = { onNavigate(AppRoute.partnerDetail(partner.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (dashboard.isModuleVisible("comunidade")) {
                item(key = "community") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardSectionHeader(
                            title = "Comunidade",
                            icon = Icons.Outlined.Forum,
                            accent = HomeZinc500,
                            onViewAll = { onNavigate(AppRoute.Community) },
                        )
                        if (dashboard.posts.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, HomeZinc800),
                            ) {
                                Text(
                                    text = "Nenhuma mensagem recente.",
                                    color = HomeZinc500,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                        } else {
                            dashboard.posts.take(2).forEach { post ->
                                DashboardPostCard(
                                    post = post,
                                    onClick = { onNavigate(AppRoute.communityPostDetail(post.id)) },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "end-spacer") { Spacer(modifier = Modifier.height(8.dp)) }
        }

        FloatingBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHomeClick = {},
            onEventsClick = { onNavigate(AppRoute.Events) },
            onScannerClick = {
                onNavigate(
                    if (state.canUseAdministrativeScanner) AppRoute.Scanner else AppRoute.CacaCalouro,
                )
            },
            onWalletClick = { onNavigate(AppRoute.MembershipCard) },
            onMenuClick = onOpenDrawer,
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
            Text(text = "Tentar novamente", fontWeight = FontWeight.Black)
        }
    }
}

private fun String.firstName(): String =
    trim()
        .split(" ")
        .firstOrNull(String::isNotBlank)
        ?.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        .orEmpty()
