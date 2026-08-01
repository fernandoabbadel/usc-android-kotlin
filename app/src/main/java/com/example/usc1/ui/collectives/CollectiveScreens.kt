package com.example.usc1.ui.collectives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400

/**
 * Catálogo público de coletivos.
 *
 * Web: `app/ligas_usc/page.tsx` (ligas, com Oráculo, curtir e seguir) e
 * `components/collectives/CollectiveCatalogPage.tsx` (comissões e diretório).
 */
@Composable
fun CollectiveCatalogScreen(
    state: CollectiveCatalogUiState,
    onGroupClick: (CollectiveGroup) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLikeClick: (CollectiveGroup) -> Unit = {},
    onFollowClick: (CollectiveGroup) -> Unit = {},
    onManageClick: (() -> Unit)? = null,
    onQuizToggleOption: (String) -> Unit = {},
    onQuizAdvance: () -> Unit = {},
    onQuizReset: () -> Unit = {},
) {
    if (state.isLoading && state.groups.isEmpty()) {
        PremiumLoadingState(text = "Carregando ${state.uiConfig.titulo}", modifier = modifier)
        return
    }

    val accent = collectiveAccent(state.kind)
    val isLeague = state.kind == CollectiveKind.League

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = state.uiConfig.titulo,
            subtitle = state.uiConfig.subtitulo,
            icon = Icons.Outlined.Groups,
            accent = accent,
            onBackClick = onBackClick,
        )
        CollectiveCatalogHero(
            uiConfig = state.uiConfig,
            kind = state.kind,
            publishedCount = state.publishedCount,
            accent = accent,
        )

        // O web só mostra "Gerenciar" quando `canManageCatalog` libera a área.
        if (state.canManageCatalog && onManageClick != null) {
            CollectiveInfoRow(
                title = "Gerenciar",
                subtitle = "Abrir a configuração desta área.",
                accent = accent,
                modifier = Modifier.clickableRow(onManageClick),
            )
        }

        if (isLeague) {
            LeagueQuizCard(
                quiz = state.quiz,
                hasCollectives = state.groups.isNotEmpty(),
                onToggleOption = onQuizToggleOption,
                onAdvance = onQuizAdvance,
                onReset = onQuizReset,
                onMatchClick = onGroupClick,
            )
        }

        when {
            state.errorMessage != null && state.groups.isEmpty() -> PremiumEmptyState(
                title = "Não foi possível carregar",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Info,
                accent = PremiumZinc400,
            )

            state.groups.isEmpty() -> PremiumEmptyState(
                title = "Nada publicado ainda",
                subtitle = "Assim que as páginas forem publicadas, elas vão aparecer aqui.",
                icon = Icons.Outlined.Groups,
                accent = accent,
            )

            else -> state.orderedGroups.forEach { group ->
                CollectiveCard(
                    group = group,
                    uiConfig = state.uiConfig,
                    membersCount = state.memberCountFor(group),
                    managementCount = group.managementMembers.size,
                    likesCount = group.likesCount,
                    isLiked = state.likedIds.contains(group.id),
                    isTogglingLike = state.togglingIds.contains(group.id),
                    // O web só oferece curtir em ligas e comissões, e seguir apenas em ligas.
                    showLikeAction = state.kind != CollectiveKind.Directory,
                    showFollowAction = isLeague,
                    isFollowing = state.followedIds.contains(group.id),
                    onClick = { onGroupClick(group) },
                    onLikeClick = { onLikeClick(group) },
                    onFollowClick = { onFollowClick(group) },
                )
            }
        }
    }
}

/**
 * Página pública do coletivo com as abas Visão geral, Membros, Agenda e Loja.
 *
 * Web: `components/collectives/CollectivePublicDetailClient.tsx` e
 * `app/ligas_usc/[leagueId]/_components/LeaguePublicDetailClient.tsx`.
 */
@Composable
fun CollectiveDetailScreen(
    state: CollectiveDetailUiState,
    onBackClick: () -> Unit,
    onTabClick: (CollectiveTab) -> Unit,
    onLikeClick: () -> Unit,
    onFollowClick: () -> Unit,
    onRoleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onManageClick: (() -> Unit)? = null,
    onProductClick: (CollectiveStoreProduct) -> Unit = {},
    onEventClick: (CollectiveEvent) -> Unit = {},
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando página", modifier = modifier)
        return
    }

    val group = state.group
    if (group == null) {
        CollectiveUnavailableScreen(
            title = state.emptyTitle,
            subtitle = state.errorMessage ?: state.emptyDescription,
            onBackClick = onBackClick,
            modifier = modifier,
        )
        return
    }

    val accent = collectiveAccent(state.kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = group.name,
            subtitle = state.uiConfig.rotuloCard,
            icon = Icons.Outlined.Groups,
            accent = accent,
            onBackClick = onBackClick,
        )
        CollectiveDetailHero(
            state = state,
            onLikeClick = onLikeClick,
            onFollowClick = onFollowClick,
            onManageClick = if (state.canManagePage) onManageClick else null,
        )
        CollectiveParticipationCard(state = state, onRoleClick = onRoleClick)
        if (group.bizu.isNotBlank()) {
            CollectiveBizuBox(text = group.bizu, title = "Bizu ${state.uiConfig.rotuloCard}")
        }
        CollectiveTabRow(
            activeTab = state.tab,
            accent = accent,
            storeEnabled = state.store.enabled,
            onTabClick = onTabClick,
        )

        when (state.tab) {
            CollectiveTab.Overview -> CollectiveOverviewTab(state = state, accent = accent)
            CollectiveTab.Members -> CollectiveMembersTab(state = state, accent = accent)
            CollectiveTab.Agenda -> CollectiveAgendaTab(state = state, accent = accent, onEventClick = onEventClick)
            CollectiveTab.Store -> CollectiveStoreTab(state = state, accent = accent, onProductClick = onProductClick)
        }
    }
}

@Composable
private fun CollectiveOverviewTab(
    state: CollectiveDetailUiState,
    accent: androidx.compose.ui.graphics.Color,
) {
    val group = state.group ?: return
    CollectiveSectionTitle(title = "Visão geral", accent = accent)
    if (group.overviewHighlights.isEmpty()) {
        PremiumEmptyState(
            title = "Sem visão geral",
            subtitle = "Essa página ainda não publicou a visão geral.",
            icon = Icons.Outlined.Info,
            accent = accent,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.overviewHighlights.forEach { highlight ->
                CollectiveInfoRow(title = "O que esse espaço faz", subtitle = highlight, accent = accent)
            }
        }
    }

    if (group.publicLinks.isNotEmpty()) {
        CollectiveSectionTitle(title = "Canais oficiais", accent = accent)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.publicLinks.forEach { link ->
                CollectiveLinkRow(link = link, accent = accent)
            }
        }
    }

    if (group.paymentInfo.hasAnyValue) {
        CollectivePaymentCard(paymentInfo = group.paymentInfo, accent = accent)
    }
}

@Composable
private fun CollectiveMembersTab(
    state: CollectiveDetailUiState,
    accent: androidx.compose.ui.graphics.Color,
) {
    val group = state.group ?: return
    val isCommission = state.kind == CollectiveKind.Commission
    CollectiveSectionTitle(title = if (isCommission) "Diretoria" else "Membros", accent = accent)

    val members = group.publicMembers
    if (members.isEmpty()) {
        PremiumEmptyState(
            title = "Nenhum membro publicado",
            subtitle = if (isCommission) {
                "Essa página ainda não publicou a diretoria da comissão."
            } else {
                "Essa página ainda não publicou os membros oficiais."
            },
            icon = Icons.Outlined.Groups,
            accent = accent,
        )
        return
    }

    val entityLabel = when (state.kind) {
        CollectiveKind.League -> "Membro oficial da liga nesta gestão."
        CollectiveKind.Commission -> "Membro oficial da comissão."
        CollectiveKind.Directory -> "Membro oficial do diretório."
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        members.forEach { member ->
            CollectiveMemberCard(member = member, entityLabel = entityLabel, accent = accent)
        }
    }
}

@Composable
private fun CollectiveAgendaTab(
    state: CollectiveDetailUiState,
    accent: androidx.compose.ui.graphics.Color,
    onEventClick: (CollectiveEvent) -> Unit,
) {
    if (state.visibleAgendaCount == 0) {
        CollectiveSectionTitle(title = "Agenda oficial", accent = accent)
        PremiumEmptyState(
            title = "Agenda vazia",
            subtitle = "A agenda desta página ainda está vazia.",
            icon = Icons.Outlined.CalendarMonth,
            accent = accent,
        )
        return
    }

    listOf(
        "Aberto ao público" to state.publicAgendaEvents,
        "Evento interno" to state.internalAgendaEvents,
    ).forEach { (title, events) ->
        if (events.isEmpty()) return@forEach
        CollectiveSectionTitle(title = "$title (${events.size})", accent = accent)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            events.forEach { event ->
                val eventModifier = if (event.resolvedEventId.isNotBlank()) {
                    Modifier.clickableRow { onEventClick(event) }
                } else {
                    Modifier
                }
                CollectiveAgendaCard(event = event, accent = accent, modifier = eventModifier)
            }
        }
    }
}

@Composable
private fun CollectiveStoreTab(
    state: CollectiveDetailUiState,
    accent: androidx.compose.ui.graphics.Color,
    onProductClick: (CollectiveStoreProduct) -> Unit,
) {
    val group = state.group ?: return
    val entityLabel = when (state.kind) {
        CollectiveKind.League -> "da liga"
        CollectiveKind.Commission -> "da comissão"
        CollectiveKind.Directory -> "do diretório"
    }

    CollectiveSectionTitle(
        title = if (state.store.enabled) "Loja publicada" else "Loja oculta",
        accent = accent,
    )

    when {
        !state.store.enabled -> PremiumEmptyState(
            title = "Loja indisponível",
            subtitle = "A loja $entityLabel está oculta no momento.",
            icon = Icons.Outlined.Storefront,
            accent = accent,
        )

        state.store.products.isEmpty() -> PremiumEmptyState(
            title = "Sem produtos",
            subtitle = "Ainda não existem produtos publicados nesta loja.",
            icon = Icons.Outlined.Storefront,
            accent = accent,
        )

        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.store.products.forEach { product ->
                CollectiveStoreProductCard(
                    product = product,
                    fallbackImageRes = group.imageRes,
                    accent = accent,
                    modifier = Modifier.clickableRow { onProductClick(product) },
                )
            }
        }
    }
}

/** Estado vazio equivalente ao `if (!league)` do web. */
@Composable
fun CollectiveUnavailableScreen(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = title,
            subtitle = "Essa página não está disponível",
            icon = Icons.Outlined.Groups,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = title,
            subtitle = subtitle,
            icon = Icons.Outlined.Info,
            accent = PremiumZinc400,
        )
    }
}

/** Estado da raiz `/diretorio` quando nenhuma página primária foi publicada. */
@Composable
fun PrimaryDirectoryUnavailableScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Diretório não configurado",
            subtitle = "Essa área ainda não foi publicada",
            icon = Icons.Outlined.Link,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Essa área ainda não foi publicada",
            subtitle = "Assim que o diretório for configurado na gestão, a página pública aparecerá aqui.",
            icon = Icons.Outlined.Info,
            accent = PremiumZinc400,
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
