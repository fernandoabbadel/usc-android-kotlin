package com.example.usc1.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBackground
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumMenuRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.core.ui.TurmaVisuals

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onShortcutClick: (ProfileShortcutUiModel) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onTabSelect: (ProfileTab) -> Unit = {},
    onFollowClick: () -> Unit = {},
    onAffinityClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onOpenFollowList: (ProfileFollowListMode) -> Unit = {},
    onCloseFollowList: () -> Unit = {},
    onOpenPeople: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {},
    onOpenTraining: (String) -> Unit = {},
    onOpenLeague: (String) -> Unit = {},
    onOpenCommunity: () -> Unit = {},
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando perfil", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Perfil",
                subtitle = "Não foi possível carregar seus dados",
                icon = Icons.Outlined.Person,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Erro no perfil",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Person,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        state.detail == null && state.profile.name.isBlank() -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Perfil",
                subtitle = "Dados do sócio USC",
                icon = Icons.Outlined.Person,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Perfil não carregado",
                subtitle = "Entre com Google e aguarde a sessão real do Supabase.",
                icon = Icons.Outlined.Person,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        state.followListMode != null -> ProfileFollowListContent(
            state = state,
            onBackClick = onCloseFollowList,
            onOpenPeople = onOpenPeople,
            modifier = modifier,
        )
        else -> ProfileLoadedContent(
            state = state,
            onShortcutClick = onShortcutClick,
            onBackClick = onBackClick,
            onTabSelect = onTabSelect,
            onFollowClick = onFollowClick,
            onAffinityClick = onAffinityClick,
            onEditProfileClick = onEditProfileClick,
            onOpenFollowList = onOpenFollowList,
            onOpenEvent = onOpenEvent,
            onOpenTraining = onOpenTraining,
            onOpenLeague = onOpenLeague,
            onOpenCommunity = onOpenCommunity,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileLoadedContent(
    state: ProfileUiState,
    onShortcutClick: (ProfileShortcutUiModel) -> Unit,
    onBackClick: (() -> Unit)?,
    onTabSelect: (ProfileTab) -> Unit,
    onFollowClick: () -> Unit,
    onAffinityClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenFollowList: (ProfileFollowListMode) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTraining: (String) -> Unit,
    onOpenLeague: (String) -> Unit,
    onOpenCommunity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val turma = detail?.turma.orEmpty().ifBlank { state.profile.className }
    val fullName = detail?.nome.orEmpty().ifBlank { state.profile.name }
    val displayName = detail?.displayName.orEmpty().ifBlank { fullName.substringBefore(" ") }

    PremiumBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Box {
                ProfileCover(
                    turma = turma,
                    avatarUrl = state.profile.avatarUrl,
                    initials = state.profile.initials,
                    isPaused = detail?.isPaused == true,
                )
                if (onBackClick != null) {
                    Surface(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 20.dp, top = 12.dp)
                            .size(40.dp)
                            .clickable(onClick = onBackClick),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(74.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayName.uppercase(),
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    when {
                        detail?.isAdminLike == true -> Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Diretoria",
                            tint = PremiumRed,
                            modifier = Modifier.size(18.dp),
                        )
                        detail?.isMiniVendor == true -> Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = "Mini Vendor",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = fullName.uppercase(),
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (detail?.isPaused == true && state.isOwnProfile) {
                    Surface(
                        shape = CircleShape,
                        color = PremiumRed.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, PremiumRed.copy(alpha = 0.32f)),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = "PERFIL OCULTO (CONTA PAUSADA)",
                            color = PremiumRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    ProfileSmallChip(label = TurmaVisuals.label(turma))
                    val age = state.age
                    if (state.showAge && age != null) {
                        ProfileSmallChip(
                            label = "$age Anos",
                            locked = detail?.idadePublica == false,
                        )
                    }
                    if (state.showRelationship) {
                        ProfileSmallChip(label = detail?.statusRelacionamento.orEmpty())
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileCircleBadge(
                        icon = Icons.Outlined.Verified,
                        tint = planColor(detail?.planoCor.orEmpty().ifBlank { state.profile.planColorKey }),
                        contentDescription = "Plano ${detail?.plano.orEmpty()}",
                    )

                    if (state.isOwnProfile) {
                        ProfilePillButton(
                            text = "Editar perfil",
                            icon = Icons.Outlined.Edit,
                            onClick = onEditProfileClick,
                        )
                    } else {
                        ProfilePillButton(
                            text = if (state.isFollowing) "Seguindo" else "Seguir",
                            icon = Icons.Outlined.PersonAdd,
                            highlighted = !state.isFollowing,
                            loading = state.isSubmittingFollow,
                            onClick = onFollowClick,
                        )
                    }

                    ProfileCircleBadge(
                        icon = Icons.Outlined.MilitaryTech,
                        tint = patenteColor(detail?.patenteCor.orEmpty()),
                        contentDescription = "${detail?.patente.orEmpty()} • ${detail?.xp ?: 0} XP",
                    )
                }

                if (!state.isOwnProfile) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfilePillButton(
                            text = if (state.affinitySent) "Remover crush" else "Enviar crush",
                            icon = Icons.Outlined.LocalFireDepartment,
                            accent = PremiumAmber,
                            loading = state.isSubmittingAffinity,
                            onClick = onAffinityClick,
                        )
                    }
                }

                if (state.actionMessage != null) {
                    Text(
                        text = state.actionMessage,
                        color = PremiumAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileStatTile(
                        value = state.followersCount.toString(),
                        label = "Seguidores",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFollowList(ProfileFollowListMode.Followers) },
                    )
                    ProfileStatTile(
                        value = state.followingCount.toString(),
                        label = "Seguindo",
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFollowList(ProfileFollowListMode.Following) },
                    )
                    ProfileStatTile(
                        value = (detail?.xp ?: 0).toString(),
                        label = "XP Total",
                        modifier = Modifier.weight(1f),
                    )
                }

                if (!detail?.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileBioCard(bio = detail.bio)
                }

                if (!detail?.cidadeOrigem.isNullOrBlank() || state.showSign) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!detail?.cidadeOrigem.isNullOrBlank()) {
                            ProfilePreferenceChip(
                                ProfilePreferenceBadge("📍", detail.cidadeOrigem),
                            )
                        }
                        if (state.showSign) {
                            ProfilePreferenceChip(
                                ProfilePreferenceBadge("✨", detail?.signo.orEmpty()),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileSocialTile(
                        icon = Icons.Outlined.PhotoCamera,
                        enabled = state.showInstagram,
                        accent = Color(0xFFDB2777),
                        contentDescription = "Instagram",
                    )
                    ProfileSocialTile(
                        icon = Icons.Outlined.Chat,
                        enabled = state.showWhatsapp,
                        accent = PremiumBrand,
                        contentDescription = "WhatsApp",
                    )
                    ProfileSocialTile(
                        icon = Icons.Outlined.Share,
                        enabled = true,
                        accent = PremiumZinc400,
                        contentDescription = "Compartilhar",
                    )
                }

                val badges = state.preferenceBadges
                if (badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "PREFERÊNCIAS",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        badges.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                row.forEach { badge ->
                                    ProfilePreferenceChip(
                                        badge = badge,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                ProfileTabsRow(
                    tabs = ProfileTab.entries,
                    activeTab = state.activeTab,
                    onSelect = onTabSelect,
                )

                Spacer(modifier = Modifier.height(14.dp))

                ProfileTabContent(
                    state = state,
                    onOpenEvent = onOpenEvent,
                    onOpenTraining = onOpenTraining,
                    onOpenLeague = onOpenLeague,
                    onOpenCommunity = onOpenCommunity,
                )

                if (state.shortcuts.isNotEmpty() && state.isOwnProfile) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ATALHOS DO PERFIL",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.shortcuts.forEach { shortcut ->
                            PremiumMenuRow(
                                title = shortcut.title,
                                subtitle = shortcut.description,
                                icon = shortcutIcon(shortcut),
                                onClick = { onShortcutClick(shortcut) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    state: ProfileUiState,
    onOpenEvent: (String) -> Unit,
    onOpenTraining: (String) -> Unit,
    onOpenLeague: (String) -> Unit,
    onOpenCommunity: () -> Unit,
) {
    when (state.activeTab) {
        ProfileTab.Posts -> {
            if (state.posts.isEmpty()) {
                ProfileEmptyTabState("Nenhum post recente.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.posts.forEach { post -> ProfilePostCard(post = post) }
                    Text(
                        text = "Ver mais na comunidade",
                        color = PremiumBrand,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenCommunity)
                            .padding(top = 6.dp),
                    )
                }
            }
        }

        ProfileTab.Eventos -> {
            if (state.eventos.isEmpty()) {
                ProfileEmptyTabState("Nenhum evento marcado.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.eventos.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { event ->
                                ProfileEventCard(
                                    event = event,
                                    onClick = { onOpenEvent(event.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        ProfileTab.Treinos -> {
            if (state.treinos.isEmpty()) {
                ProfileEmptyTabState("Nenhum treino confirmado.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.treinos.forEach { training ->
                        ProfileTrainingCard(
                            training = training,
                            onClick = { onOpenTraining(training.id) },
                        )
                    }
                }
            }
        }

        ProfileTab.Ligas -> {
            if (state.ligas.isEmpty()) {
                ProfileEmptyTabState("Não participa de ligas.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.ligas.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { league ->
                                ProfileLeagueTile(
                                    league = league,
                                    onClick = { onOpenLeague(league.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFollowListContent(
    state: ProfileUiState,
    onBackClick: () -> Unit,
    onOpenPeople: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFollowers = state.followListMode == ProfileFollowListMode.Followers
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp) {
        PremiumHeader(
            title = if (isFollowers) "Seguidores" else "Seguindo",
            subtitle = state.profile.name.ifBlank { "Perfil USC" },
            icon = Icons.Outlined.Person,
            onBackClick = onBackClick,
        )
        when {
            state.followListLoading -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = PremiumBrand, strokeWidth = 3.dp)
            }
            state.followList.isEmpty() -> PremiumEmptyState(
                title = if (isFollowers) "Sem seguidores ainda" else "Ainda não segue ninguém",
                subtitle = "Interaja na comunidade para começar a conectar com a galera.",
                icon = Icons.Outlined.Person,
            )
            else -> state.followList.forEach { person ->
                ProfileFollowRow(
                    person = person,
                    onClick = { onOpenPeople(person.uid) },
                )
            }
        }
    }
}

@Composable
private fun ProfilePillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    loading: Boolean = false,
    accent: Color = PremiumBrand,
) {
    Surface(
        modifier = modifier.clickable(enabled = !loading, onClick = onClick),
        shape = CircleShape,
        color = if (highlighted) accent.copy(alpha = 0.92f) else PremiumZinc800,
        border = BorderStroke(1.dp, if (highlighted) accent else PremiumZinc700),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = if (highlighted) Color.White else accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlighted) Color.White else PremiumZinc300,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text.uppercase(),
                color = if (highlighted) Color.White else PremiumZinc300,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProfileSocialTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    accent: Color,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) accent.copy(alpha = 0.16f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.42f) else PremiumZinc800),
    ) {
        Icon(
            imageVector = if (enabled) icon else Icons.Outlined.Lock,
            contentDescription = contentDescription,
            tint = if (enabled) accent else PremiumZinc700,
            modifier = Modifier.padding(13.dp),
        )
    }
}

/** Espelha `resolvePlanTheme` do web-reference (classes tailwind -> cor). */
private fun planColor(colorKey: String): Color {
    val key = colorKey.lowercase()
    return when {
        key.contains("emerald") || key.contains("green") -> PremiumBrand
        key.contains("amber") || key.contains("yellow") || key.contains("gold") -> PremiumAmber
        key.contains("red") || key.contains("rose") -> PremiumRed
        key.contains("blue") || key.contains("cyan") -> Color(0xFF3B82F6)
        key.contains("purple") || key.contains("violet") -> Color(0xFFA855F7)
        key.contains("orange") -> Color(0xFFF97316)
        else -> PremiumZinc500
    }
}

private fun patenteColor(colorKey: String): Color {
    val key = colorKey.lowercase()
    return when {
        key.contains("orange") -> Color(0xFFF97316)
        key.contains("red") -> PremiumRed
        key.contains("emerald") || key.contains("green") -> PremiumBrand
        key.contains("blue") -> Color(0xFF3B82F6)
        key.contains("yellow") || key.contains("amber") -> PremiumAmber
        else -> PremiumZinc500
    }
}

private fun shortcutIcon(shortcut: ProfileShortcutUiModel) =
    when {
        shortcut.title.contains("Pedido", ignoreCase = true) -> Icons.Outlined.CreditCard
        shortcut.title.contains("Carteirinha", ignoreCase = true) -> Icons.Outlined.CreditCard
        shortcut.title.contains("Plano", ignoreCase = true) -> Icons.Outlined.CreditCard
        shortcut.title.contains("Ingresso", ignoreCase = true) -> Icons.Outlined.Event
        shortcut.title.contains("Evento", ignoreCase = true) -> Icons.Outlined.Event
        shortcut.title.contains("Loja", ignoreCase = true) -> Icons.Outlined.Storefront
        shortcut.title.contains("Config", ignoreCase = true) -> Icons.Outlined.Settings
        shortcut.title.contains("Conquista", ignoreCase = true) -> Icons.Outlined.AutoAwesome
        else -> Icons.Outlined.Person
    }
