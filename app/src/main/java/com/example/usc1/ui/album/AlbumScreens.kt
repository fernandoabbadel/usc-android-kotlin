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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PhotoAlbum
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
import com.example.usc1.R
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
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
        PremiumHeader(title = "Álbum", subtitle = "Turmas, fotos e caça-calouro", icon = Icons.Outlined.PhotoAlbum)
        NativeModuleHeroCard("ÁLBUM DA GALERA", "Caça-calouro", "Fotos, turmas, ranking e pontuação visual da base.", R.drawable.capa_t9)
        PremiumCard(accent = PremiumAmber) {
            Text(text = "CAÇA-CALOURO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(text = "Pontuação por turma e ranking mockado.", color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            com.example.usc1.core.ui.PremiumPrimaryButton("Abrir caça-calouro", onCacaCalouroClick, icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber)
        }
        state.turmas.forEach { turma ->
            TurmaCard(turma = turma, onClick = { onTurmaClick(turma) })
        }
    }
}

@Composable
fun AlbumTurmaScreen(turma: AlbumTurma, state: AlbumUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = turma.name, subtitle = "Galeria da turma", icon = Icons.Outlined.PhotoAlbum, onBackClick = onBackClick)
        NativeModuleHeroCard(turma.name, "${turma.score} pontos", "${turma.members} membros no álbum.", turma.coverRes, accent = PremiumBrand)
        AlbumPhotoGrid(photos = state.photos)
    }
}

@Composable
fun CacaCalouroScreen(state: AlbumUiState, onRankingClick: () -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Caça-calouro", subtitle = "Missões e pontuação", icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber, onBackClick = onBackClick)
        NativeStatCard("Turma líder", state.turmas.maxBy { it.score }.name, icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber)
        state.turmas.forEach { turma -> TurmaCard(turma = turma, onClick = onRankingClick) }
    }
}

@Composable
fun CalouroRankingScreen(state: AlbumUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Ranking", subtitle = "Pontuação geral", icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber, onBackClick = onBackClick)
        state.turmas.sortedByDescending { it.score }.forEachIndexed { index, turma ->
            NativeStatCard("${index + 1}º • ${turma.name}", "${turma.score} XP", icon = Icons.Outlined.EmojiEvents, accent = if (index == 0) PremiumAmber else PremiumBrand)
        }
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
                            .border(1.dp, PremiumZinc800, RoundedCornerShape(22.dp)),
                    ) {
                        Image(
                            painter = painterResource(id = photo.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.82f,
                        )
                        Text(
                            text = photo.title.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                        )
                    }
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
            .border(1.dp, PremiumBrand.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = turma.coverRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.76f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = turma.name.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            Text(text = "${turma.score} pontos • ${turma.members} membros", color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
