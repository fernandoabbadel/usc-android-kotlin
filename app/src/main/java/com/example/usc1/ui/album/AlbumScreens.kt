package com.example.usc1.ui.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun AlbumScreen(
    state: AlbumUiState,
    onTurmaClick: (AlbumTurma) -> Unit,
    onCacaCalouroClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMyQr by remember { mutableStateOf(false) }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = state.title,
            subtitle = "Turmas, QR e caça-calouro",
            icon = Icons.Outlined.PhotoAlbum,
        )

        NativeModuleHeroCard(
            title = "ÁLBUM AAAKN",
            subtitle = "CAÇA AOS BIXOS",
            body = state.heroHeadline,
            imageRes = R.drawable.capa_t8,
            imageUrl = state.heroCoverUrl,
            accent = PremiumBrand,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AlbumQuickActionCard(
                title = "Meu QR",
                subtitle = "Mostra seu código para ser capturado.",
                icon = Icons.Outlined.QrCodeScanner,
                accent = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { showMyQr = true },
            )
            AlbumQuickActionCard(
                title = "Ler QR agora",
                subtitle = "Capturar figurinha na sua turma.",
                icon = Icons.Outlined.CameraAlt,
                accent = PremiumBrand,
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = onCacaCalouroClick,
            )
        }

        if (state.errorMessage != null) {
            PremiumEmptyState(
                title = "Dados parciais",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.PhotoAlbum,
                accent = PremiumAmber,
            )
        }

        PremiumCard(accent = PremiumBrand, containerColor = PremiumBrand.copy(alpha = 0.13f), borderAlpha = 0.55f) {
            Text(
                text = "ESCOLHA A TURMA E DOMINE O ÁLBUM",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = state.subtitle,
                color = PremiumZinc400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumChip(label = "${state.currentUserCollected} capturas", icon = Icons.Outlined.CheckCircle, accent = PremiumBrand)
                PremiumChip(label = "${state.turmas.size} turmas", icon = Icons.Outlined.PhotoAlbum, accent = PremiumAmber)
            }
        }

        NativeSectionTitle(title = if (state.isLoading) "Turmas • sincronizando" else "Turmas • ${state.turmas.size} publicadas")
        state.turmas.forEach { turma ->
            TurmaCard(turma = turma, onClick = { onTurmaClick(turma) })
        }
    }

    if (showMyQr) {
        AlbumQrDialog(
            payload = state.myQrPayload,
            enabled = state.canUseQr,
            onDismiss = { showMyQr = false },
        )
    }
}

@Composable
fun AlbumTurmaScreen(
    turma: AlbumTurma,
    state: AlbumUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenProfile: (String) -> Unit = {},
) {
    var showMyQr by remember { mutableStateOf(false) }
    val turmaPhotos = state.photos.filter { it.turma.equals(turma.id, ignoreCase = true) }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = turma.name,
            subtitle = "Jornal da turma",
            icon = Icons.Outlined.PhotoAlbum,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AlbumQuickActionCard(
                title = "Meu QR",
                subtitle = "Código oficial de captura.",
                icon = Icons.Outlined.QrCodeScanner,
                accent = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { showMyQr = true },
            )
            AlbumQuickActionCard(
                title = "Ler QR",
                subtitle = "Abrir leitor da galera.",
                icon = Icons.Outlined.CameraAlt,
                accent = PremiumBrand,
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = { },
            )
        }

        NativeModuleHeroCard(
            title = turma.id,
            subtitle = turma.name,
            body = "${turma.mascot} • ${turma.score} capturas • ${turma.members} membros",
            imageRes = turma.coverRes,
            imageUrl = turma.coverUrl,
            accent = PremiumBrand,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard("Membros", turma.members.toString(), icon = Icons.Outlined.Person, accent = PremiumBrand, modifier = Modifier.weight(1f))
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
            AlbumPhotoList(
                photos = turmaPhotos,
                onPhotoClick = { photo ->
                    photo.publicProfileUrl?.takeIf(String::isNotBlank)?.let(onOpenProfile)
                },
            )
        }
    }

    if (showMyQr) {
        AlbumQrDialog(
            payload = state.myQrPayload,
            enabled = state.canUseQr,
            onDismiss = { showMyQr = false },
        )
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
fun AlbumPhotoList(
    photos: List<AlbumPhoto>,
    modifier: Modifier = Modifier,
    onPhotoClick: (AlbumPhoto) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.forEach { photo ->
            AlbumPersonCard(photo = photo, onClick = { onPhotoClick(photo) })
        }
    }
}

@Composable
fun AlbumPhotoGrid(
    photos: List<AlbumPhoto>,
    modifier: Modifier = Modifier,
    onPhotoClick: (AlbumPhoto) -> Unit = {},
) {
    AlbumPhotoList(photos = photos, modifier = modifier, onPhotoClick = onPhotoClick)
}

@Composable
private fun AlbumPersonCard(photo: AlbumPhoto, onClick: () -> Unit = {}) {
    val accent = if (photo.collected) PremiumBrand else PremiumZinc700
    val canOpenProfile = photo.collected && photo.profileVisible && !photo.publicProfileUrl.isNullOrBlank()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpenProfile, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = PremiumZinc900.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (photo.collected) 0.62f else 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(2.dp, accent.copy(alpha = 0.88f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (photo.imageUrl != null) {
                    AsyncImage(
                        model = photo.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = photo.imageRes),
                        error = painterResource(id = photo.imageRes),
                        alpha = if (photo.collected) 1f else 0.44f,
                    )
                } else {
                    Image(
                        painter = painterResource(id = photo.imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (photo.collected) 1f else 0.44f,
                    )
                }
                if (!photo.collected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = PremiumZinc400)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PremiumChip(
                        label = if (photo.collected) "capturado" else "bloqueado",
                        icon = if (photo.collected) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                        accent = accent,
                    )
                    if (photo.turma.isNotBlank()) {
                        PremiumChip(label = photo.turma, accent = PremiumBrand)
                    }
                }
                Text(
                    text = photo.title.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (photo.subtitle.isNotBlank()) {
                    Text(text = photo.subtitle, color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (!photo.profileVisible) {
                    PremiumChip(label = "Perfil invisível", icon = Icons.Outlined.Lock, accent = PremiumZinc500)
                } else if (photo.collected) {
                    // Dados preenchidos no cadastro/edicao de perfil, como no album do web.
                    val chips = buildList {
                        if (photo.ageLabel.isNotBlank()) add(AlbumInfoChip(photo.ageLabel, PremiumBrand))
                        if (photo.petLabel.isNotBlank()) add(AlbumInfoChip("🐾 ${photo.petLabel}", Color(0xFFF97316)))
                        if (photo.origin.isNotBlank()) add(AlbumInfoChip("📍 ${photo.origin}", PremiumBrand))
                        if (photo.relationship.isNotBlank()) {
                            add(AlbumInfoChip("💗 ${photo.relationship}", Color(0xFFEC4899)))
                        }
                        photo.sports.take(3).forEach { sport ->
                            add(AlbumInfoChip("🏆 $sport", PremiumZinc400))
                        }
                        if (photo.sports.size > 3) {
                            add(AlbumInfoChip("+${photo.sports.size - 3}", PremiumZinc500))
                        }
                    }
                    if (chips.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            chips.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    row.forEach { chip ->
                                        AlbumMiniChip(label = chip.label, accent = chip.accent)
                                    }
                                }
                            }
                        }
                    }
                    if (photo.bio.isNotBlank()) {
                        Text(
                            text = "“${photo.bio}”",
                            color = PremiumZinc400,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (photo.instagram.isNotBlank()) {
                        Text(
                            text = "@${photo.instagram}",
                            color = Color(0xFFEC4899),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = PremiumZinc700,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "BLOQUEADO",
                            color = PremiumZinc700,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }
    }
}

private data class AlbumInfoChip(val label: String, val accent: Color)

@Composable
private fun AlbumMiniChip(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = 0.12f),
    ) {
        Text(
            text = label.uppercase(),
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TurmaCard(turma: AlbumTurma, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.58f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = if (turma.hidden) PremiumAmber.copy(alpha = 0.34f) else PremiumBrand.copy(alpha = 0.36f),
                shape = RoundedCornerShape(32.dp),
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
                alpha = 0.82f,
            )
        } else {
            Image(
                painter = painterResource(id = turma.coverRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.82f,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(58.dp),
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "→", color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PremiumChip(label = turma.id, accent = PremiumBrand)
            Text(text = turma.name.uppercase(), color = Color.White, fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            Text(text = "${turma.mascot} • ${turma.score} capturas • ${turma.members} membros", color = PremiumZinc400, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlbumQuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = if (filled) accent.copy(alpha = 0.18f) else PremiumZinc900.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (filled) 0.64f else 0.38f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (filled) accent else Color.White,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = if (filled) Color.Black else Color.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title.uppercase(), color = if (filled) accent else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(text = subtitle, color = if (filled) PremiumBrand else PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AlbumQrDialog(
    payload: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        PremiumCard(accent = PremiumBrand, containerColor = Color.Black.copy(alpha = 0.96f), borderAlpha = 0.65f) {
            Text(
                text = "MEU QR",
                color = PremiumBrand,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = if (enabled) "Mostre este código para ser capturado no Álbum da Galera." else "Entre com sua conta para gerar o QR oficial.",
                color = PremiumZinc400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PremiumQrCode(
                    payload = payload,
                    cells = 15,
                    cellSize = 7.dp,
                    label = if (enabled) "USC OFICIAL" else "LOGIN NECESSÁRIO",
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            PremiumPrimaryButton(
                text = "Fechar",
                onClick = onDismiss,
                icon = Icons.Outlined.CheckCircle,
                accent = PremiumBrand,
            )
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
