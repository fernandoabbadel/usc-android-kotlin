package com.example.usc1.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.navigation.AppRoute

private data class HomeDrawerLink(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val moduleKey: String? = null,
    val badge: String? = null,
)

@Composable
fun HomeSideDrawer(
    visible: Boolean,
    state: HomeUiState,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 2 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(onClick = onDismiss),
        ) {
            val drawerInteraction = remember { MutableInteractionSource() }
            val drawerWidth = minOf(maxWidth * 0.85f, 320.dp)
            Column(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .background(HomeZinc950)
                    .clickable(
                        interactionSource = drawerInteraction,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                DrawerHeader(onDismiss = onDismiss)
                DrawerProfile(state = state) {
                    onDismiss()
                    onNavigate(AppRoute.Profile)
                }
                DrawerPlanBanner {
                    onDismiss()
                    onNavigate(AppRoute.Plans)
                }

                val mainLinks = listOfNotNull(
                    HomeDrawerLink("Início", AppRoute.Dashboard, Icons.Outlined.Home, "dashboard"),
                    HomeDrawerLink("Lojinha", AppRoute.Store, Icons.Outlined.Storefront, "loja"),
                    HomeDrawerLink("Eventos", AppRoute.Events, Icons.Outlined.Event, "eventos"),
                    state.dashboard.activeSalesEvent?.let { event ->
                        HomeDrawerLink(
                            "Modo Vendas",
                            AppRoute.eventDetail(event.id),
                            Icons.Outlined.ShoppingBag,
                            "eventos",
                            "AO VIVO",
                        )
                    },
                    HomeDrawerLink("Carteirinha", AppRoute.MembershipCard, Icons.Outlined.CreditCard, "carteirinha"),
                    HomeDrawerLink("Parceiros", AppRoute.Partners, Icons.Outlined.Handshake, "parceiros"),
                    HomeDrawerLink("Comunidade", AppRoute.Community, Icons.Outlined.Forum, "comunidade"),
                    HomeDrawerLink("Álbum da Galera", AppRoute.Album, Icons.Outlined.Album, "album"),
                ).filter { link ->
                    link.moduleKey == null || state.dashboard.isModuleVisible(link.moduleKey)
                }
                val athleteLinks = listOf(
                    HomeDrawerLink("Treinos", AppRoute.Training, Icons.Outlined.FitnessCenter, "treinos"),
                    HomeDrawerLink("BoardRound", AppRoute.Boardround, Icons.Outlined.Games, "sharkround", "EM BREVE"),
                    HomeDrawerLink("Arena Games", AppRoute.Games, Icons.Outlined.Games, "arena_games", "EM BREVE"),
                ).filter { it.moduleKey == null || state.dashboard.isModuleVisible(it.moduleKey) }
                val infoLinks = listOf(
                    HomeDrawerLink("Área das Ligas", AppRoute.Leagues, Icons.Outlined.Groups, "ligas"),
                    HomeDrawerLink("Comissões", AppRoute.Commissions, Icons.Outlined.Groups, "comissoes"),
                    HomeDrawerLink("Diretório", AppRoute.Directory, Icons.Outlined.School, "diretorio"),
                    HomeDrawerLink("Guia", AppRoute.Guide, Icons.Outlined.HelpOutline, "guia"),
                ).filter { it.moduleKey == null || state.dashboard.isModuleVisible(it.moduleKey) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    item { DrawerSectionLabel("MENU PRINCIPAL", Icons.Outlined.Home) }
                    items(mainLinks, key = { "main:${it.route}" }) { link ->
                        DrawerMenuRow(link) {
                            onDismiss()
                            onNavigate(link.route)
                        }
                    }
                    item { DrawerSectionLabel("ÁREA DO ATLETA", Icons.Outlined.FitnessCenter, HomeBrand) }
                    items(athleteLinks, key = { "athlete:${it.route}" }) { link ->
                        DrawerMenuRow(link) {
                            onDismiss()
                            onNavigate(link.route)
                        }
                    }
                    item { DrawerSectionLabel("CENTRAL DE INFO", Icons.Outlined.Groups) }
                    items(infoLinks, key = { "info:${it.route}" }) { link ->
                        DrawerMenuRow(link) {
                            onDismiss()
                            onNavigate(link.route)
                        }
                    }
                }

                DrawerFooter(
                    canManageTenant = state.canManageTenant,
                    onAdminClick = {
                        onDismiss()
                        onNavigate(AppRoute.Admin)
                    },
                    onSettingsClick = {
                        onDismiss()
                        onNavigate(AppRoute.Settings)
                    },
                    onSignOut = {
                        onDismiss()
                        onSignOut()
                    },
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(onDismiss: () -> Unit) {
    val tenantStyle = HomeTenantTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.42f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = tenantStyle.logoUrl,
                contentDescription = "Logo ${tenantStyle.displayName}",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HomeBrand.copy(alpha = 0.16f))
                    .padding(3.dp),
                contentScale = ContentScale.Fit,
                fallback = painterResource(id = R.drawable.logo_usc),
                error = painterResource(id = R.drawable.logo_usc),
            )
            Column {
                Text(
                    text = tenantStyle.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "APP OFICIAL",
                    color = HomeZinc500,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DrawerCircleIcon(Icons.Outlined.NotificationsNone, "Notificações", onClick = {})
            DrawerCircleIcon(Icons.Outlined.Close, "Fechar menu", onClick = onDismiss)
        }
    }
}

@Composable
private fun DrawerCircleIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = HomeZinc900,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.padding(9.dp),
            tint = HomeZinc400,
        )
    }
}

@Composable
private fun DrawerProfile(state: HomeUiState, onClick: () -> Unit) {
    val tenantStyle = HomeTenantTheme.current
    Surface(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = HomeZinc900.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, HomeZinc800),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = state.userAvatarUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.5.dp, HomeZinc700, CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = R.drawable.logo_usc),
                    error = painterResource(id = R.drawable.logo_usc),
                )
                AsyncImage(
                    model = tenantStyle.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, HomeZinc900, CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = R.drawable.logo_usc),
                    error = painterResource(id = R.drawable.logo_usc),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.userName.firstNameForDrawer(),
                    color = HomeBrandAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "NÍVEL ${state.dashboard.profile.level} • ${state.accountStatus.ifBlank { "MEMBRO" }.uppercase()}",
                    color = HomeZinc500,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (state.canManageTenant) Color(0xFFEF4444) else HomeBrand,
            )
        }
    }
}

@Composable
private fun DrawerPlanBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF422006), Color(0xFF78350F), Color(0xFF422006)),
                ),
            )
            .border(1.dp, HomeGold.copy(alpha = 0.36f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = HomeGold,
            )
            Column {
                Text("VER PLANOS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("Confira níveis e benefícios", color = Color(0xFFD4D4D8), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(label: String, icon: ImageVector, accent: Color = HomeZinc500) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = accent)
        Text(
            text = label,
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun DrawerMenuRow(link: HomeDrawerLink, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = link.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = HomeZinc500,
        )
        Text(
            text = link.label.uppercase(),
            color = HomeZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        link.badge?.let { badge ->
            Text(
                text = badge,
                color = if (badge == "AO VIVO") HomeBrandAccent else HomeZinc500,
                fontSize = 6.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (badge == "AO VIVO") HomeBrand.copy(alpha = 0.45f) else HomeZinc700,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun DrawerFooter(
    canManageTenant: Boolean,
    onAdminClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeZinc950)
            .navigationBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canManageTenant) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clickable(onClick = onAdminClick),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF450A0A).copy(alpha = 0.38f),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D).copy(alpha = 0.72f)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFFEF4444),
                    )
                    Spacer(Modifier.size(7.dp))
                    Text(
                        "PAINEL ADMIN",
                        color = Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrawerFooterButton(
                label = "AJUSTES",
                icon = Icons.Outlined.Settings,
                modifier = Modifier.weight(1f),
                onClick = onSettingsClick,
            )
            DrawerFooterButton(
                label = "SAIR",
                icon = Icons.Outlined.Logout,
                modifier = Modifier.weight(1f),
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun DrawerFooterButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = HomeZinc900,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = HomeZinc500)
            Text(label, color = HomeZinc500, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.firstNameForDrawer(): String =
    trim().split(" ").firstOrNull(String::isNotBlank).orEmpty().ifBlank { "Membro" }
