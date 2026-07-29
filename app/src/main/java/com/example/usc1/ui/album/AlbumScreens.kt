package com.example.usc1.ui.album

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.ui.theme.UscTheme

@Composable
fun AlbumScreen(
    state: AlbumUiState,
    onTurmaClick: (AlbumTurma) -> Unit,
    onCacaCalouroClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = state.title,
            subtitle = "Turmas, fotos e caça-calouro",
            icon = Icons.Outlined.PhotoAlbum,
        )

        NativeModuleHeroCard(
            title = "CAÇA AOS BIXOS",
            subtitle = state.heroHeadline,
            body = state.subtitle,
            imageRes = R.drawable.capa_t8,
            imageUrl = state.heroCoverUrl,
            accent = PremiumBrand,
        )

        if (state.errorMessage != null) {
            PremiumEmptyState(
                title = "Dados parciais",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.PhotoAlbum,
                accent = PremiumAmber,
            )
        }

        PremiumCard(accent = PremiumAmber) {
            Text(text = "CAÇA-CALOURO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                text = "${state.currentUserCollected} figurinhas capturadas no seu álbum.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumPrimaryButton(
                text = "Abrir caça-calouro",
                onClick = onCacaCalouroClick,
                icon = Icons.Outlined.EmojiEvents,
                accent = PremiumAmber,
            )
        }

        NativeSectionTitle(title = if (state.isLoading) "Turmas • sincronizando" else "Turmas • ${state.turmas.size} publicadas")
        state.turmas.forEach { turma ->
            TurmaCard(turma = turma, onClick = { onTurmaClick(turma) })
        }
    }
}

@Composable
fun AlbumTurmaScreen(
    turma: AlbumTurma,
    state: AlbumUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turmaPhotos = state.photos.filter { it.turma.equals(turma.id, ignoreCase = true) }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = turma.name,
            subtitle = "Galeria da turma",
            icon = Icons.Outlined.PhotoAlbum,
            onBackClick = onBackClick,
        )
        NativeModuleHeroCard(
            title = turma.id,
            subtitle = turma.name,
            body = "${turma.mascot} • ${turma.members} membros • ${turma.score} capturas",
            imageRes = turma.coverRes,
            imageUrl = turma.coverUrl,
            accent = PremiumBrand,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard("Membros", turma.members.toString(), icon = Icons.Outlined.PhotoAlbum, accent = PremiumBrand, modifier = Modifier.weight(1f))
            NativeStatCard("Capturas", turma.score.toString(), icon = Icons.Outlined.CameraAlt, accent = PremiumAmber, modifier = Modifier.weight(1f))
        }
        NativeSectionTitle(title = "Figurinhas • integrantes reais")
        if (turmaPhotos.isEmpty()) {
            PremiumEmptyState(
                title = "Turma sem figurinhas",
                subtitle = "Ainda não há integrantes publicados para esta turma.",
                icon = Icons.Outlined.PhotoAlbum,
                accent = PremiumBrand,
            )
        } else {
            AlbumPhotoGrid(photos = turmaPhotos)
        }
    }
}

@Composable
fun CacaCalouroScreen(
    state: AlbumUiState,
    onRankingClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leader = state.turmas.maxByOrNull { it.score }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Caça-calouro",
            subtitle = "Missões e pontuação",
            icon = Icons.Outlined.EmojiEvents,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )
        NativeStatCard(
            label = "Turma líder",
            value = leader?.name ?: "Sem ranking",
            icon = Icons.Outlined.EmojiEvents,
            accent = PremiumAmber,
        )
        PremiumPrimaryButton(
            text = "Ver ranking completo",
            onClick = onRankingClick,
            icon = Icons.Outlined.EmojiEvents,
            accent = PremiumAmber,
        )
        state.turmas.sortedByDescending { it.score }.forEach { turma ->
            TurmaCard(turma = turma, onClick = onRankingClick)
        }
    }
}

@Composable
fun CalouroRankingScreen(
    state: AlbumUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Ranking",
            subtitle = "Pontuação geral",
            icon = Icons.Outlined.EmojiEvents,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )
        if (state.ranking.isNotEmpty()) {
            state.ranking.forEachIndexed { index, entry ->
                RankingCard(index = index, entry = entry)
            }
        } else {
            state.turmas.sortedByDescending { it.score }.forEachIndexed { index, turma ->
                NativeStatCard(
                    label = "${index + 1}º • ${turma.name}",
                    value = "${turma.score} capturas",
                    icon = Icons.Outlined.EmojiEvents,
                    accent = if (index == 0) PremiumAmber else PremiumBrand,
                )
            }
        }
    }
}

@Composable
fun AlbumUnavailableScreen(
    title: String = "Turma não encontrada",
    subtitle: String = "A turma pode estar oculta ou ainda não foi publicada nesta atlética.",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Álbum", subtitle = "Caça aos bixos", icon = Icons.Outlined.PhotoAlbum, onBackClick = onBackClick)
        PremiumEmptyState(title = title, subtitle = subtitle, icon = Icons.Outlined.PhotoAlbum, accent = PremiumAmber)
    }
}

@Composable
fun AlbumPhotoGrid(
    photos: List<AlbumPhoto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        photos.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { photo ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black)
                            .border(
                                width = 1.dp,
                                color = if (photo.collected) PremiumBrand.copy(alpha = 0.72f) else PremiumZinc800,
                                shape = RoundedCornerShape(22.dp),
                            ),
                    ) {
                        if (photo.imageUrl != null) {
                            AsyncImage(
                                model = photo.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = photo.imageRes),
                                error = painterResource(id = photo.imageRes),
                                alpha = 0.82f,
                            )
                        } else {
                            Image(
                                painter = painterResource(id = photo.imageRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.82f,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f)))),
                        )
                        if (photo.collected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = PremiumBrand,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(18.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(text = photo.title.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            if (photo.subtitle.isNotBlank()) {
                                Text(text = photo.subtitle.uppercase(), color = PremiumZinc400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TurmaCard(turma: AlbumTurma, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.65f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = if (turma.hidden) PremiumAmber.copy(alpha = 0.34f) else PremiumBrand.copy(alpha = 0.28f),
                shape = RoundedCornerShape(28.dp),
            )
            .clickable(onClick = onClick),
    ) {
        if (turma.coverUrl != null) {
            AsyncImage(
                model = turma.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = turma.coverRes),
                error = painterResource(id = turma.coverRes),
                alpha = 0.76f,
            )
        } else {
            Image(
                painter = painterResource(id = turma.coverRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.76f,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = turma.id.uppercase(), color = PremiumZinc400, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(text = turma.name.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            Text(text = "${turma.mascot} • ${turma.score} capturas • ${turma.members} membros", color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RankingCard(index: Int, entry: AlbumRankingEntry) {
    PremiumCard(accent = if (index == 0) PremiumAmber else PremiumBrand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (index == 0) PremiumAmber else PremiumBrand),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "${index + 1}", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = entry.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(text = entry.turma.ifBlank { "Sem turma" }, color = PremiumZinc400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "${entry.totalCollected}", color = if (index == 0) PremiumAmber else PremiumBrand, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun AlbumScreenPreview() {
    UscTheme(darkTheme = true) {
        AlbumScreen(AlbumUiState(), {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CacaCalouroScreenPreview() {
    UscTheme(darkTheme = true) {
        CacaCalouroScreen(AlbumUiState(), {}, {})
    }
}
