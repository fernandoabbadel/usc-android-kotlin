package com.example.usc1.ui.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumGold
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc600
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.core.ui.TurmaVisuals
import com.example.usc1.domain.model.RankingClass
import com.example.usc1.domain.model.RankingUser

private val BronzeColor = Color(0xFFB45309)

/** `/ranking` — pódio e classificação, individual ou por turma. */
@Composable
fun RankingScreen(
    state: RankingUiState,
    onTabClick: (RankingTab) -> Unit,
    onUserClick: (String) -> Unit,
    onClassClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Ranking Geral",
            subtitle = "Pontuação da atlética",
            icon = Icons.Outlined.EmojiEvents,
            accent = PremiumGold,
            onBackClick = onBackClick,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PremiumZinc900.copy(alpha = 0.6f))
                .border(1.dp, PremiumZinc800, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RankingTab.entries.forEach { tab ->
                RankingTabButton(
                    tab = tab,
                    isActive = state.activeTab == tab,
                    onClick = { onTabClick(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            state.isLoading -> PremiumLoadingState(text = "Calculando ranking")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Ranking indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.EmojiEvents,
            )
            state.activeTab == RankingTab.Individual -> RankingIndividualContent(
                users = state.users,
                onUserClick = onUserClick,
            )
            else -> RankingClassesContent(
                classes = state.classes,
                onClassClick = onClassClick,
            )
        }
    }
}

@Composable
private fun RankingIndividualContent(
    users: List<RankingUser>,
    onUserClick: (String) -> Unit,
) {
    if (users.isEmpty()) {
        PremiumEmptyState(
            title = "Nenhum dado encontrado",
            subtitle = "Ninguém pontuou no ranking desta atlética ainda.",
            icon = Icons.Outlined.EmojiEvents,
        )
        return
    }

    RankingPodium(
        entries = users.take(3).map { user ->
            RankingPodiumEntry(
                id = user.id,
                label = user.podiumLabel,
                points = user.xp,
                photoUrl = user.photoUrl,
                className = null,
            )
        },
        onClick = onUserClick,
    )

    users.drop(3).forEachIndexed { index, user ->
        RankingRow(
            position = index + 4,
            title = user.name,
            subtitle = "Turma ${user.className}",
            points = user.xp,
            photoUrl = user.photoUrl,
            className = null,
            onClick = { onUserClick(user.id) },
        )
    }
}

@Composable
private fun RankingClassesContent(
    classes: List<RankingClass>,
    onClassClick: (String) -> Unit,
) {
    if (classes.isEmpty()) {
        PremiumEmptyState(
            title = "Nenhum dado encontrado",
            subtitle = "Nenhuma turma pontuou nesta atlética ainda.",
            icon = Icons.Outlined.Group,
        )
        return
    }

    RankingPodium(
        entries = classes.take(3).map { entry ->
            RankingPodiumEntry(
                id = entry.id,
                label = entry.name,
                points = entry.points,
                photoUrl = null,
                className = entry.name,
            )
        },
        onClick = onClassClick,
    )

    classes.drop(3).forEachIndexed { index, entry ->
        RankingRow(
            position = index + 4,
            title = entry.name,
            subtitle = "${entry.members} membros",
            points = entry.points,
            photoUrl = null,
            className = entry.name,
            onClick = { onClassClick(entry.id) },
        )
    }
}

/** `/ranking/[turmaId]` — banner da turma e classificação interna. */
@Composable
fun RankingClassScreen(
    state: RankingClassUiState,
    onUserClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Ranking ${state.className}",
            subtitle = "Classificação interna",
            icon = Icons.Outlined.Group,
            accent = PremiumGold,
            onBackClick = onBackClick,
        )

        if (state.isLoading) {
            PremiumLoadingState(text = "Carregando turma")
            return@PremiumScreen
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = PremiumZinc900,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RankingClassAvatar(className = state.className, size = 72.dp)
                Text(
                    text = "TURMA ${state.className.uppercase()}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = "${state.members.size} ALUNOS",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${state.totalPoints} PTS",
                        color = PremiumBrand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        when {
            state.errorMessage != null -> PremiumEmptyState(
                title = "Turma indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Group,
            )
            state.members.isEmpty() -> PremiumEmptyState(
                title = "Sem pontuação",
                subtitle = "Nenhum aluno dessa turma pontuou ainda.",
                icon = Icons.Outlined.Group,
            )
            else -> state.members.forEachIndexed { index, member ->
                RankingRow(
                    position = index + 1,
                    title = member.nickname.ifBlank { member.name },
                    subtitle = "Atleta da ${member.className}",
                    points = member.xp,
                    photoUrl = member.photoUrl,
                    className = null,
                    onClick = { onUserClick(member.id) },
                )
            }
        }
    }
}

private data class RankingPodiumEntry(
    val id: String,
    val label: String,
    val points: Int,
    val photoUrl: String?,
    val className: String?,
)

@Composable
private fun RankingPodium(
    entries: List<RankingPodiumEntry>,
    onClick: (String) -> Unit,
) {
    if (entries.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.getOrNull(1)?.let { entry ->
            RankingPodiumSlot(entry = entry, position = 2, size = 60.dp, accent = PremiumZinc300, onClick = onClick)
        }
        entries.getOrNull(0)?.let { entry ->
            RankingPodiumSlot(entry = entry, position = 1, size = 88.dp, accent = PremiumGold, onClick = onClick)
        }
        entries.getOrNull(2)?.let { entry ->
            RankingPodiumSlot(entry = entry, position = 3, size = 60.dp, accent = BronzeColor, onClick = onClick)
        }
    }
}

@Composable
private fun RankingPodiumSlot(
    entry: RankingPodiumEntry,
    position: Int,
    size: Dp,
    accent: Color,
    onClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick(entry.id) }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(PremiumZinc800)
                .border(3.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.className != null) {
                RankingClassAvatar(className = entry.className, size = size)
            } else if (!entry.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = PremiumZinc500)
            }
        }
        Surface(shape = CircleShape, color = accent) {
            Text(
                text = "${position}º",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = entry.label,
            color = Color.White,
            fontSize = if (position == 1) 13.sp else 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${entry.points} pts",
            color = if (position == 1) PremiumGold else PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun RankingRow(
    position: Int,
    title: String,
    subtitle: String,
    points: Int,
    photoUrl: String?,
    className: String?,
    onClick: () -> Unit,
) {
    val positionColor = when (position) {
        1 -> PremiumGold
        2 -> PremiumZinc300
        3 -> BronzeColor
        else -> PremiumZinc600
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${position}º",
                modifier = Modifier.width(28.dp),
                color = positionColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PremiumZinc800),
                contentAlignment = Alignment.Center,
            ) {
                if (className != null) {
                    RankingClassAvatar(className = className, size = 40.dp)
                } else if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PremiumZinc500,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = points.toString(),
                    color = PremiumBrand,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "PTS",
                    color = PremiumZinc600,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/** `getTurmaImage` do web: brasão local por turma. */
@Composable
private fun RankingClassAvatar(className: String, size: Dp) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = TurmaVisuals.photoDrawable(className)),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun RankingTabButton(
    tab: RankingTab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) PremiumBrand else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (tab == RankingTab.Individual) {
                    Icons.Outlined.Person
                } else {
                    Icons.Outlined.Group
                },
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (isActive) Color.Black else PremiumZinc400,
            )
            Text(
                text = "  ${tab.label.uppercase()}",
                color = if (isActive) Color.Black else PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}
