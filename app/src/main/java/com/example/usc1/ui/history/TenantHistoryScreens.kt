package com.example.usc1.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
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
import com.example.usc1.domain.model.OrganogramDisplayMember

/** `/historico` — capa configurável e linha do tempo de `historic_events`. */
@Composable
fun TenantHistoryScreen(
    state: TenantHistoryUiState,
    onOrganogramClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = state.config.title,
            subtitle = state.config.subtitle,
            icon = Icons.Outlined.EmojiEvents,
            onBackClick = onBackClick,
        )

        if (!state.config.coverPhotoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = state.config.coverPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.55f,
                )
            }
        }

        PremiumSecondaryButton(
            text = "Ver organograma",
            onClick = onOrganogramClick,
            icon = Icons.Outlined.AccountTree,
        )

        when {
            state.isLoading -> PremiumLoadingState(text = "Resgatando arquivos")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Histórico indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.EmojiEvents,
            )
            state.events.isEmpty() -> PremiumEmptyState(
                title = "Nenhuma história contada ainda",
                subtitle = "A diretoria pode montar a linha do tempo no painel administrativo.",
                icon = Icons.Outlined.EmojiEvents,
            )
            else -> state.events.forEach { event ->
                PremiumCard(accent = PremiumBrand) {
                    if (!event.photoUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black),
                        ) {
                            AsyncImage(
                                model = event.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = event.year,
                                color = PremiumBrand,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                text = event.title.uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = PremiumBrand,
                        )
                    }
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            color = PremiumZinc400,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryMetaChip(
                            icon = Icons.Outlined.CalendarMonth,
                            label = event.date.ifBlank { "Data a definir" },
                        )
                        HistoryMetaChip(
                            icon = Icons.Outlined.LocationOn,
                            label = event.location.ifBlank { "Local a definir" },
                        )
                    }
                }
            }
        }
    }
}

/** `/historico/organograma` — núcleos e membros publicados. */
@Composable
fun OrganogramScreen(
    state: OrganogramUiState,
    onMemberClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = state.config.title,
            subtitle = state.config.subtitle,
            icon = Icons.Outlined.AccountTree,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "PARTICIPAÇÃO",
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            when (state.membershipState) {
                OrganogramMembershipState.Pending -> {
                    PremiumChip(label = "Solicitação pendente", accent = PremiumAmber, filled = true)
                    Text(
                        text = "Sua solicitação já está com a diretoria, aguardando aprovação.",
                        color = PremiumZinc400,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OrganogramMembershipState.Published -> {
                    PremiumChip(label = "Você já participa", accent = PremiumBrand, filled = true)
                    Text(
                        text = "Seu perfil já está publicado no organograma da atlética.",
                        color = PremiumZinc400,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OrganogramMembershipState.None -> Text(
                    text = "O pedido de entrada no organograma é gravado pelo servidor do painel web " +
                        "(rota administrativa com service role) e ainda não roda no app.",
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        when {
            state.isLoading -> PremiumLoadingState(text = "Montando organograma")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Organograma indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.AccountTree,
            )
            state.sections.isEmpty() -> PremiumEmptyState(
                title = "Organograma ainda vazio",
                subtitle = "A diretoria pode montar esta página no painel administrativo.",
                icon = Icons.Outlined.AccountTree,
            )
            else -> state.sections.forEach { section ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PremiumBrand.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.3f)),
                    ) {
                        Icon(
                            imageVector = sectionIcon(section.name),
                            contentDescription = null,
                            modifier = Modifier.padding(11.dp),
                            tint = PremiumBrand,
                        )
                    }
                    Column {
                        Text(
                            text = "NÚCLEO",
                            color = PremiumZinc500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = section.name.uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                section.members.forEach { member ->
                    OrganogramMemberCard(entry = member, onClick = onMemberClick)
                }
            }
        }
    }
}

@Composable
private fun OrganogramMemberCard(
    entry: OrganogramDisplayMember,
    onClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PremiumZinc800)
                        .border(1.dp, PremiumZinc700, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!entry.displayPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = entry.displayPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (entry.hasCanonicalVisual) 1f else 0.7f,
                        )
                    } else {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = PremiumZinc500)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = entry.member.role.uppercase(),
                        color = PremiumBrand,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = entry.displayName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.displayDetail.uppercase(),
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!entry.hasCanonicalVisual) {
                    PremiumChip(label = "Vinculação pendente", accent = PremiumZinc500)
                }
                if (entry.member.userId.isNotBlank()) {
                    PremiumSecondaryButton(
                        text = "Abrir perfil",
                        onClick = { onClick(entry.member.userId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryMetaChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = PremiumBrand)
            Text(
                text = label,
                color = PremiumZinc400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** `sectionIcon` da página do web. */
private fun sectionIcon(section: String): ImageVector {
    val normalized = section.trim().lowercase()
    return when {
        normalized.contains("presid") -> Icons.Outlined.WorkspacePremium
        normalized.contains("diret") -> Icons.Outlined.Shield
        else -> Icons.Outlined.Groups
    }
}
