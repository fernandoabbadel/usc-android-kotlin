package com.example.usc1.ui.collectives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumGold
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

/** Acento por área, espelhando o brand de cada rota web. */
fun collectiveAccent(kind: CollectiveKind): Color = when (kind) {
    CollectiveKind.League -> PremiumBrand
    CollectiveKind.Directory -> PremiumPurple
    CollectiveKind.Commission -> PremiumAmber
}

/**
 * Cabeçalho do catálogo.
 *
 * Web: bloco `<section>` de `CollectiveCatalogPage.tsx` (comissões escondem rótulo e cards
 * de identidade) e o `<header>` de `ligas_usc/page.tsx`.
 */
@Composable
fun CollectiveCatalogHero(
    uiConfig: CollectiveAreaUiConfig,
    kind: CollectiveKind,
    publishedCount: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val showIdentityCards = kind != CollectiveKind.Commission
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = PremiumZinc900.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                            radius = 520f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showIdentityCards) {
                    PremiumChip(label = uiConfig.rotuloCard, accent = accent, filled = true)
                }
                Text(
                    text = uiConfig.titulo.uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    text = uiConfig.subtitulo,
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (showIdentityCards) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CollectivePillMetric(
                            value = publishedCount.toString(),
                            label = "Páginas ativas",
                            icon = Icons.Outlined.Groups,
                            accent = accent,
                        )
                        CollectivePillMetric(
                            value = uiConfig.sidebarLabel,
                            label = "Identidade",
                            icon = Icons.Outlined.Lightbulb,
                            accent = accent,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cartão do catálogo.
 *
 * Web: `<article>` de `CollectiveCatalogPage.tsx` e o card de liga de `ligas_usc/page.tsx`
 * (nome truncado por `clampLeagueCardName`, sigla, descrição, bizu, membros e ações).
 */
@Composable
fun CollectiveCard(
    group: CollectiveGroup,
    uiConfig: CollectiveAreaUiConfig,
    membersCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    managementCount: Int = 0,
    likesCount: Int = group.likesCount,
    isLiked: Boolean = false,
    isTogglingLike: Boolean = false,
    showLikeAction: Boolean = false,
    showFollowAction: Boolean = false,
    isFollowing: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onFollowClick: (() -> Unit)? = null,
) {
    val accent = collectiveAccent(group.kind)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = PremiumZinc900.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(212.dp)
                    .background(Color.Black),
            ) {
                CollectiveImage(
                    imageUrl = group.imageUrl,
                    imageRes = group.imageRes,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.76f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.34f),
                                    PremiumZinc900.copy(alpha = 0.98f),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PremiumChip(label = uiConfig.rotuloCard, accent = accent, filled = true)
                    if (group.turmaId.isNotBlank()) {
                        PremiumChip(label = group.turmaId, accent = accent)
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = CollectiveTextUtils.clampCardName(group.name).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.acronym.ifBlank { uiConfig.sidebarLabel }.uppercase(),
                        color = PremiumZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = group.description.ifBlank {
                        "${uiConfig.rotuloCard} oficial com identidade, membros e agenda própria."
                    },
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (group.bizu.isNotBlank()) {
                    CollectiveBizuBox(text = group.bizu, title = "Bizu")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CollectiveSmallMetric(membersCount.toString(), "membros", Icons.Outlined.Groups, accent)
                    if (group.kind == CollectiveKind.Commission) {
                        CollectiveSmallMetric(
                            managementCount.toString(),
                            "da diretoria",
                            Icons.Outlined.AutoAwesome,
                            accent,
                        )
                    } else if (group.overview.isNotBlank()) {
                        CollectiveSmallMetric("", "visão geral ativa", Icons.Outlined.AutoAwesome, accent)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                            .clickable(onClick = onClick),
                        shape = RoundedCornerShape(16.dp),
                        color = accent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ABRIR PÁGINA",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }
                    if (showLikeAction) {
                        CollectiveLikePill(
                            likesCount = likesCount,
                            isLiked = isLiked,
                            enabled = onLikeClick != null && !isTogglingLike,
                            showCount = group.kind == CollectiveKind.League,
                            onClick = { onLikeClick?.invoke() },
                        )
                    }
                    if (showFollowAction) {
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .clickable(enabled = onFollowClick != null) { onFollowClick?.invoke() },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isFollowing) PremiumBrand.copy(alpha = 0.14f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isFollowing) PremiumBrand.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.14f),
                            ),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isFollowing) "SEGUINDO" else "SEGUIR",
                                    color = if (isFollowing) PremiumBrand else Color.White.copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectiveLikePill(
    likesCount: Int,
    isLiked: Boolean,
    enabled: Boolean,
    showCount: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isLiked) PremiumRed.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isLiked) PremiumRed.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.14f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Remover curtida" else "Curtir",
                modifier = Modifier.size(18.dp),
                tint = if (isLiked) PremiumRed else Color.White.copy(alpha = 0.78f),
            )
            if (showCount) {
                Text(
                    text = likesCount.toString(),
                    color = if (isLiked) PremiumRed else Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/** Oráculo de compatibilidade (`QUIZ SECTION` de `ligas_usc/page.tsx`). */
@Composable
fun LeagueQuizCard(
    quiz: LeagueQuizUiState,
    hasCollectives: Boolean,
    onToggleOption: (String) -> Unit,
    onAdvance: () -> Unit,
    onReset: () -> Unit,
    onMatchClick: (CollectiveGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PremiumPurple
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = PremiumZinc900.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!quiz.showResult) {
                PremiumChip(label = "Oráculo", icon = Icons.Outlined.AutoAwesome, accent = accent, filled = true)
                Text(
                    text = quiz.question.text,
                    color = Color.White,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = "Selecione até ${LeagueQuizCatalog.MaxSelectedOptions} opções:",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quiz.question.options.forEach { option ->
                        val selected = quiz.selectedOptions.contains(option.label)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleOption(option.label) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) accent.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.40f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.62f) else PremiumZinc800,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (selected) Color.White else PremiumZinc400,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LeagueQuizCatalog.questions.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (index <= quiz.step) accent else PremiumZinc800),
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .clickable(enabled = quiz.canAdvance, onClick = onAdvance),
                        shape = RoundedCornerShape(14.dp),
                        color = if (quiz.canAdvance) Color.White else PremiumZinc800,
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "PRÓXIMA",
                                color = if (quiz.canAdvance) accent else PremiumZinc500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Compatibilidade por Liga",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onReset),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PremiumZinc500,
                        )
                        Text(text = "Refazer", color = PremiumZinc500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                when {
                    !hasCollectives || quiz.matches.isEmpty() -> Text(
                        text = "Nenhuma liga cadastrada para comparar.",
                        color = PremiumZinc500,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                    )

                    quiz.allZero -> Text(
                        text = "Nenhuma liga teve compatibilidade acima de 0% com este perfil.",
                        color = PremiumZinc500,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                    )

                    else -> Unit
                }
                quiz.matches.forEachIndexed { index, match ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMatchClick(match.collective) },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.40f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = accent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                            CollectiveImage(
                                imageUrl = match.collective.imageUrl,
                                imageRes = match.collective.imageRes,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = match.collective.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                LinearProgressIndicator(
                                    progress = { match.matchPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = accent,
                                    trackColor = PremiumZinc800,
                                )
                            }
                            Text(
                                text = "${match.matchPercent}%",
                                color = accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hero da página pública com identidade, curtidas, agenda e ações de curtir/seguir.
 *
 * Web: bloco `<section>` do topo de `CollectivePublicDetailClient.tsx`.
 */
@Composable
fun CollectiveDetailHero(
    state: CollectiveDetailUiState,
    onLikeClick: () -> Unit,
    onFollowClick: () -> Unit,
    onManageClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val group = state.group ?: return
    val accent = collectiveAccent(state.kind)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = PremiumZinc900.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
        ) {
            Box(modifier = Modifier.height(272.dp)) {
                CollectiveImage(
                    imageUrl = state.heroImageUrl,
                    imageRes = group.imageRes,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.72f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.52f),
                                    Color.Black.copy(alpha = 0.92f),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onManageClick != null) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(onClick = onManageClick),
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Abrir gestão",
                                modifier = Modifier.padding(10.dp),
                                tint = accent,
                            )
                        }
                    }
                    PremiumChip(label = state.uiConfig.rotuloCard, accent = accent)
                    if (group.turmaId.isNotBlank() && !group.turmaId.equals(group.acronym, ignoreCase = true)) {
                        PremiumChip(label = group.turmaId, accent = accent)
                    }
                    PremiumChip(label = group.displayAcronym, accent = accent, filled = true)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.headerLabel.uppercase(),
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                    )
                    Text(
                        text = group.name.uppercase(),
                        color = Color.White,
                        fontSize = 30.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.description.ifBlank {
                            "${state.uiConfig.rotuloCard} oficial com página própria para mostrar membros, agenda e identidade visual."
                        },
                        color = PremiumZinc400,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PremiumChip(label = "${state.displayMembersCount} membros", accent = accent)
            if (group.presidentName.isNotBlank()) {
                PremiumChip(label = "Presidente: ${group.presidentName}", accent = PremiumGold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveStatBox(
                label = "Curtidas",
                value = group.likesCount.toString(),
                icon = Icons.Outlined.FavoriteBorder,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            CollectiveStatBox(
                label = "Agenda",
                value = state.visibleAgendaCount.toString(),
                icon = Icons.Outlined.CalendarMonth,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveToggleBox(
                label = if (state.isLiked) "Curtida" else "Curtir",
                icon = if (state.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                active = state.isLiked,
                activeColor = PremiumRed,
                enabled = state.userId.isNotBlank() && !state.isTogglingLike,
                onClick = onLikeClick,
                modifier = Modifier.weight(1f),
            )
            CollectiveToggleBox(
                label = if (state.isFollowing) "Seguindo" else "Seguir",
                icon = Icons.Outlined.Groups,
                active = state.isFollowing,
                activeColor = PremiumBrand,
                enabled = state.userId.isNotBlank(),
                onClick = onFollowClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Abas públicas (`<nav>` de `CollectivePublicDetailClient.tsx`). */
@Composable
fun CollectiveTabRow(
    activeTab: CollectiveTab,
    accent: Color,
    storeEnabled: Boolean,
    onTabClick: (CollectiveTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = CollectiveTab.entries.filter { tab ->
        tab != CollectiveTab.Store || storeEnabled || activeTab == CollectiveTab.Store
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tabs.forEach { tab ->
            val selected = tab == activeTab
            Surface(
                modifier = Modifier
                    .height(56.dp)
                    .clickable { onTabClick(tab) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) accent.copy(alpha = 0.16f) else PremiumZinc900.copy(alpha = 0.86f),
                border = BorderStroke(
                    1.dp,
                    if (selected) accent.copy(alpha = 0.40f) else PremiumZinc800,
                ),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label.uppercase(),
                        color = if (selected) accent else PremiumZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                    )
                }
            }
        }
    }
}

/**
 * Card de participação/acesso.
 *
 * O envio da solicitação continua pendente: no web ele passa por
 * `POST /api/ligas/member-requests`, rota que grava com service role.
 */
@Composable
fun CollectiveParticipationCard(
    state: CollectiveDetailUiState,
    onRoleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(state.kind)
    val isCommission = state.kind == CollectiveKind.Commission
    val request = state.currentMemberRequest
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isCommission) "ACESSO À CONFIGURAÇÃO" else "PARTICIPAÇÃO NA PÁGINA",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = when {
                    state.requestBlockedByMembership && isCommission ->
                        "Seu perfil já está na equipe de gestão desta comissão."
                    state.requestBlockedByMembership ->
                        "Seu perfil já está na equipe oficial ${state.uiConfig.rotuloCard.lowercase()}."
                    request != null ->
                        "Solicitação enviada como ${request.requestedRole}."
                    isCommission ->
                        "Solicite acesso à configuração escolhendo um cargo da gestão."
                    else ->
                        "Escolha o cargo desejado e envie sua solicitação para a equipe analisar."
                },
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (request == null && !state.requestBlockedByMembership) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.requestRoleOptions.forEach { role ->
                        Box(modifier = Modifier.clickable { onRoleClick(role) }) {
                            PremiumChip(
                                label = role,
                                accent = accent,
                                filled = role == state.requestRole,
                            )
                        }
                    }
                }
                Text(
                    text = "O envio da solicitação é gravado pelo servidor do painel web " +
                        "(rota administrativa com service role) e ainda não roda no app.",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun CollectiveBizuBox(
    text: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PremiumGold.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PremiumGold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title.uppercase(),
                    color = PremiumGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.7.sp,
                )
                Text(
                    text = text,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun CollectiveMemberCard(
    member: CollectiveMember,
    entityLabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (member.photoUrl.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp),
                        tint = accent,
                    )
                }
            } else {
                AsyncImage(
                    model = member.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = member.role.uppercase(),
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = member.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entityLabel,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Card de agenda com badge de data (`getEventBadge` do web). */
@Composable
fun CollectiveAgendaCard(
    event: CollectiveEvent,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val (day, month) = CollectiveDateUtils.eventBadge(event.date)
    val badgeAccent = if (event.isInternal) PremiumAmber else accent
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.width(66.dp),
                shape = RoundedCornerShape(18.dp),
                color = badgeAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.34f)),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = day, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = month,
                        color = badgeAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = event.visibilityLabel.uppercase(),
                    color = badgeAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.time.isNotBlank() || event.place.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (event.time.isNotBlank()) PremiumChip(label = event.time, accent = badgeAccent)
                        if (event.place.isNotBlank()) PremiumChip(label = event.place, accent = badgeAccent)
                    }
                }
                Text(
                    text = event.description.ifBlank { "Evento publicado sem descrição adicional." },
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CollectiveStoreProductCard(
    product: CollectiveStoreProduct,
    fallbackImageRes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = PremiumZinc900.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .background(Color.Black),
            ) {
                CollectiveImage(
                    imageUrl = product.imageUrl,
                    imageRes = fallbackImageRes,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.82f,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            ),
                        ),
                )
                if (product.tagLabel.isNotBlank()) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(14.dp)) {
                        PremiumChip(label = product.tagLabel, accent = accent, filled = true)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = product.category.ifBlank { "Produto" }.uppercase(),
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.7.sp,
                )
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = product.priceLabel,
                        color = accent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                    PremiumChip(label = "Abrir loja", accent = accent)
                }
            }
        }
    }
}

@Composable
fun CollectiveLinkRow(
    link: CollectiveLink,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
            ) {
                Icon(
                    imageVector = collectiveLinkIcon(link.type),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = accent,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = link.label.ifBlank { collectiveLinkTypeLabel(link.type) },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = collectiveLinkTypeLabel(link.type).uppercase(),
                    color = PremiumZinc500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
            }
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = PremiumZinc500,
            )
        }
    }
}

@Composable
fun CollectivePaymentCard(
    paymentInfo: CollectivePaymentInfo,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Wallet,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
                Column {
                    Text(
                        text = "PAGAMENTO",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                    )
                    Text(
                        text = "Dados publicados",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (paymentInfo.pixKey.isNotBlank()) CollectiveInfoRow("Chave PIX", paymentInfo.pixKey, accent = accent)
            if (paymentInfo.bank.isNotBlank()) CollectiveInfoRow("Banco", paymentInfo.bank, accent = accent)
            if (paymentInfo.holder.isNotBlank()) CollectiveInfoRow("Titular", paymentInfo.holder, accent = accent)
            if (paymentInfo.whatsapp.isNotBlank()) CollectiveInfoRow("Comprovante", paymentInfo.whatsapp, accent = accent)
        }
    }
}

@Composable
fun CollectiveInfoRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(
                text = subtitle,
                color = PremiumZinc400,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CollectiveSectionTitle(title: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(17.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(accent),
        )
        Text(
            text = title.uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.4.sp,
        )
    }
}

/** `LEAGUE_LINK_TYPE_LABELS` do web. */
fun collectiveLinkTypeLabel(type: String): String = when (type.trim().lowercase()) {
    "instagram" -> "Instagram"
    "tiktok" -> "TikTok"
    "youtube" -> "YouTube"
    "site" -> "Site"
    "whatsapp" -> "WhatsApp"
    "linkedin" -> "LinkedIn"
    else -> "Link"
}

private fun collectiveLinkIcon(type: String): ImageVector = when (type.trim().lowercase()) {
    "site" -> Icons.Outlined.Language
    else -> Icons.Outlined.Link
}

@Composable
internal fun CollectiveImage(
    imageUrl: String?,
    imageRes: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    if (imageUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alpha = alpha,
        )
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alpha = alpha,
            fallback = painterResource(id = imageRes),
            error = painterResource(id = imageRes),
        )
    }
}

@Composable
private fun CollectivePillMetric(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = accent)
            Column {
                Text(text = label.uppercase(), color = PremiumZinc500, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CollectiveStatBox(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(17.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp), tint = accent)
            }
            Column {
                Text(
                    text = label.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                )
                Text(text = value, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CollectiveToggleBox(
    label: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (active) activeColor.copy(alpha = 0.12f) else PremiumZinc900.copy(alpha = 0.90f),
        border = BorderStroke(
            1.dp,
            if (active) activeColor.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.10f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (active) activeColor else PremiumZinc400.copy(alpha = if (enabled) 1f else 0.5f),
            )
            Text(
                text = label.uppercase(),
                color = if (active) activeColor else PremiumZinc400.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
            )
        }
    }
}

@Composable
private fun CollectiveSmallMetric(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = accent)
        if (value.isNotBlank()) {
            Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Start,
        )
    }
}
