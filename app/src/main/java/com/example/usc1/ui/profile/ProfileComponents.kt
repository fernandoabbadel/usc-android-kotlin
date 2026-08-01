package com.example.usc1.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc600
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.core.ui.TurmaVisuals
import com.example.usc1.domain.model.ProfileEventItem
import com.example.usc1.domain.model.ProfileFollowUser
import com.example.usc1.domain.model.ProfileLeagueItem
import com.example.usc1.domain.model.ProfilePost
import com.example.usc1.domain.model.ProfileTrainingItem

/** Capa da turma desfocada + avatar sobreposto, como no `/perfil/[id]` do web. */
@Composable
fun ProfileCover(
    turma: String,
    avatarUrl: String?,
    initials: String,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp),
        ) {
            Image(
                painter = painterResource(TurmaVisuals.coverDrawable(turma)),
                contentDescription = "Capa da turma",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(2.dp)
                    .alpha(0.6f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PremiumBrand.copy(alpha = 0.12f),
                                Color(0xFF050505).copy(alpha = 0.55f),
                                Color(0xFF050505),
                            ),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPaused) {
                            Brush.linearGradient(listOf(PremiumZinc600, PremiumZinc900))
                        } else {
                            Brush.linearGradient(listOf(PremiumBrandAccent, PremiumBrand))
                        },
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFF050505), CircleShape)
                        .background(PremiumZinc900),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Foto do perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = initials,
                            color = PremiumBrand,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF050505), CircleShape)
                    .background(PremiumZinc900),
            ) {
                Image(
                    painter = painterResource(TurmaVisuals.photoDrawable(turma)),
                    contentDescription = "Turma $turma",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }
    }
}

/** Pilula redonda de plano/patente (PlanBadge e LevelBadge do web). */
@Composable
fun ProfileCircleBadge(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(46.dp),
        shape = CircleShape,
        color = PremiumZinc900,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.padding(13.dp),
        )
    }
}

@Composable
fun ProfileStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ProfileSmallChip(
    label: String,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = PremiumZinc800,
        border = BorderStroke(1.dp, PremiumZinc700),
    ) {
        Text(
            text = if (locked) "$label 🔒" else label.uppercase(),
            color = PremiumZinc300,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun ProfileTabsRow(
    tabs: List<ProfileTab>,
    activeTab: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val active = tab == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tab.label.uppercase(),
                        color = if (active) PremiumBrand else PremiumZinc500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(vertical = 9.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (active) PremiumBrand else Color.Transparent),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PremiumZinc800),
        )
    }
}

@Composable
fun ProfilePostCard(post: ProfilePost, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PremiumZinc900.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "“${post.texto}”",
                color = PremiumZinc300,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileMetric(Icons.Outlined.FavoriteBorder, post.likes.toString())
                    ProfileMetric(Icons.Outlined.ChatBubbleOutline, post.comentarios.toString())
                }
                Text(
                    text = post.timeLabel,
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileMetric(icon: ImageVector, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumZinc500,
            modifier = Modifier.size(12.dp),
        )
        Text(text = value, color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileEventCard(
    event: ProfileEventItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f),
            ) {
                if (!event.imagem.isNullOrBlank()) {
                    AsyncImage(
                        model = event.imagem,
                        contentDescription = event.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(PremiumZinc800))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF09090B)),
                            ),
                        ),
                )
                Text(
                    text = event.titulo.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = PremiumBrand,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = event.data.ifBlank { "Data a definir" },
                        color = PremiumZinc400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PremiumBrand),
                )
            }
        }
    }
}

@Composable
fun ProfileTrainingCard(
    training: ProfileTrainingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(96.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row {
            Box(modifier = Modifier.width(96.dp).fillMaxSize()) {
                if (!training.imagem.isNullOrBlank()) {
                    AsyncImage(
                        model = training.imagem,
                        contentDescription = training.modalidade,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(PremiumZinc800))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, PremiumZinc900),
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = training.modalidade.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = PremiumBrand,
                            modifier = Modifier.size(9.dp),
                        )
                        Text(
                            text = "EU VOU",
                            color = PremiumBrand,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                ProfileTrainingLine(Icons.Outlined.CalendarMonth, training.dia)
                ProfileTrainingLine(Icons.Outlined.Schedule, training.horario)
                ProfileTrainingLine(Icons.Outlined.LocationOn, training.local)
            }
        }
    }
}

@Composable
private fun ProfileTrainingLine(icon: ImageVector, value: String) {
    if (value.isBlank()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumBrand,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = value.uppercase(),
            color = PremiumZinc400,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProfileLeagueTile(
    league: ProfileLeagueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .border(2.dp, PremiumZinc800, CircleShape)
                .background(PremiumZinc900),
            contentAlignment = Alignment.Center,
        ) {
            if (!league.logo.isNullOrBlank()) {
                AsyncImage(
                    model = league.logo,
                    contentDescription = league.nome,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = PremiumZinc500,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = league.sigla.ifBlank { league.nome }.uppercase(),
            color = PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProfileFollowRow(
    person: ProfileFollowUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PremiumZinc800),
                contentAlignment = Alignment.Center,
            ) {
                if (!person.foto.isNullOrBlank()) {
                    AsyncImage(
                        model = person.foto,
                        contentDescription = person.nome,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(
                        text = person.nome.take(1).uppercase(),
                        color = PremiumBrand,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.nome,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = person.turma.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun ProfileBioCard(bio: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, PremiumZinc800.copy(alpha = 0.6f)),
    ) {
        Text(
            text = "“$bio”",
            color = PremiumZinc300,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun ProfilePreferenceChip(badge: ProfilePreferenceBadge, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = badge.icon, fontSize = 11.sp)
            Text(
                text = badge.label,
                color = PremiumZinc300,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProfileEmptyTabState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = PremiumZinc600,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 22.dp),
    )
}
