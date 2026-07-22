package com.example.usc1.ui.home

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
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
import com.example.usc1.domain.model.HomeDashboardEvent
import com.example.usc1.domain.model.HomeDashboardLeague
import com.example.usc1.domain.model.HomeDashboardPartner
import com.example.usc1.domain.model.HomeDashboardPost
import com.example.usc1.domain.model.HomeDashboardProduct
import java.text.NumberFormat
import java.util.Locale

private val DashboardSectionCardShape = RoundedCornerShape(24.dp)

@Composable
fun DashboardSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = HomeBrand,
    onViewAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onViewAll != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "VER TODOS",
                    color = accent.copy(alpha = 0.78f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = accent.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
fun DashboardFeatureGrid(
    trainingImageUrls: List<String>,
    showBoardround: Boolean,
    showTraining: Boolean,
    onBoardroundClick: () -> Unit,
    onTrainingClick: () -> Unit,
) {
    if (!showBoardround && !showTraining) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showBoardround) {
            BoardroundDashboardCard(
                modifier = Modifier.weight(1f),
                onClick = onBoardroundClick,
            )
        }
        if (showTraining) {
            TrainingDashboardCard(
                imageUrls = trainingImageUrls,
                modifier = Modifier.weight(1f),
                onClick = onTrainingClick,
            )
        }
    }
}

@Composable
private fun BoardroundDashboardCard(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(DashboardSectionCardShape)
            .background(HomeBrand)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(100.dp)
                .background(HomeBrandAccent.copy(alpha = 0.24f), CircleShape),
        )
        Text(
            text = "EM BREVE",
            color = Color.Black,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(HomeAmber)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Black,
            )
            Text(
                text = "BOARDROUND",
                color = Color.Black,
                fontSize = 19.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun TrainingDashboardCard(
    imageUrls: List<String>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(DashboardSectionCardShape)
            .background(HomeZinc900)
            .border(1.dp, HomeZinc800, DashboardSectionCardShape)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(2) { rowIndex ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(2) { columnIndex ->
                        val index = rowIndex * 2 + columnIndex
                        val imageUrl = imageUrls.getOrNull(index)
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (index % 2 == 0) HomeZinc800 else HomeZinc700),
                            contentScale = ContentScale.Crop,
                            fallback = painterResource(id = R.drawable.battle_forest),
                            error = painterResource(id = R.drawable.battle_forest),
                            alpha = if (imageUrl == null) 0.18f else 0.48f,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFF97316),
            )
            Text(
                text = "TREINOS",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
fun DashboardEventCard(
    event: HomeDashboardEvent,
    modifier: Modifier = Modifier.width(316.dp),
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(450.dp)
            .clip(DashboardSectionCardShape)
            .clickable(onClick = onClick),
        shape = DashboardSectionCardShape,
        color = HomeZinc950,
        border = BorderStroke(1.dp, HomeZinc800),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp),
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = "Capa do evento ${event.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(
                        horizontalBias = 0f,
                        verticalBias = (((event.imagePositionY ?: 50.0) / 50.0) - 1.0).toFloat(),
                    ),
                    fallback = painterResource(id = R.drawable.battle_forest),
                    error = painterResource(id = R.drawable.battle_forest),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, HomeZinc950))),
                )
                Text(
                    text = event.type.ifBlank { "EVENTO" }.uppercase(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = event.title.uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DashboardEventMetadata(
                            icon = Icons.Outlined.CalendarMonth,
                            label = listOf(event.date, event.time)
                                .filter(String::isNotBlank)
                                .joinToString(" • "),
                        )
                        if (event.location.isNotBlank()) {
                            DashboardEventMetadata(
                                icon = Icons.Outlined.LocationOn,
                                label = event.location,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = HomeZinc500,
                        )
                        Text(
                            text = event.likesCount.toString(),
                            color = HomeZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    NeonStatusChip(
                        label = if (event.viewerIsInterested) "CONFIRMADO" else "VER DETALHES",
                        icon = if (event.viewerIsInterested) Icons.Outlined.CheckCircle else Icons.Outlined.ArrowForward,
                        color = if (event.viewerIsInterested) HomeBrandAccent else HomeZinc400,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardEventMetadata(
    icon: ImageVector,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = HomeBrand,
        )
        Text(
            text = label,
            color = HomeZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DashboardProductCard(
    product: HomeDashboardProduct,
    modifier: Modifier = Modifier.width(316.dp),
    onClick: () -> Unit,
) {
    val price = remember(product.price) {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(product.price)
    }
    Surface(
        modifier = modifier
            .height(450.dp)
            .clip(DashboardSectionCardShape)
            .clickable(onClick = onClick),
        shape = DashboardSectionCardShape,
        color = HomeZinc950,
        border = BorderStroke(1.dp, HomeZinc800),
    ) {
        Column {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = "Imagem do produto ${product.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(Color.White),
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.logo_usc_wide),
                error = painterResource(id = R.drawable.logo_usc_wide),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = product.name.uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = price,
                        color = Color(0xFFD946EF),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (product.viewerHasLiked) Color(0xFFEF4444) else HomeZinc500,
                            )
                            Text(product.likesCount.toString(), color = HomeZinc500, fontSize = 11.sp)
                        }
                        NeonStatusChip(
                            label = "COMPRAR",
                            icon = Icons.Outlined.ShoppingBag,
                            color = Color(0xFFE879F9),
                        )
                    }
                    if (product.topClasses.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            product.topClasses.forEach { stat ->
                                DashboardClassStatChip(stat.className, stat.count)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardClassStatChip(className: String, count: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(HomeZinc800.copy(alpha = 0.58f))
            .border(1.dp, HomeZinc700.copy(alpha = 0.55f), CircleShape)
            .padding(start = 2.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = turmaDashboardImage(className)),
            contentDescription = "Turma $className",
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = "+$count",
            color = HomeZinc400,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun DashboardLeagueCard(
    league: HomeDashboardLeague,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .width(166.dp)
            .height(208.dp)
            .clip(DashboardSectionCardShape)
            .clickable(onClick = onClick),
        shape = DashboardSectionCardShape,
        color = HomeZinc950,
        border = BorderStroke(1.dp, HomeZinc800),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = league.logoUrl,
                    contentDescription = "Logo ${league.name}",
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, HomeGold.copy(alpha = 0.55f), CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = R.drawable.logo_usc),
                    error = painterResource(id = R.drawable.logo_usc),
                )
                if (league.weeklyTip.isNotBlank()) {
                    Surface(shape = CircleShape, color = HomeGold) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = "Bizu recente",
                            modifier = Modifier.padding(4.dp).size(12.dp),
                            tint = Color.Black,
                        )
                    }
                }
            }
            Text(
                text = league.acronym.ifBlank { league.name }.uppercase(),
                color = HomeBrandAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeZinc900.copy(alpha = 0.72f), RoundedCornerShape(9.dp))
                    .border(1.dp, HomeZinc800, RoundedCornerShape(9.dp))
                    .padding(8.dp),
            ) {
                Text(
                    text = league.weeklyTip.ifBlank { league.description.ifBlank { "Liga acadêmica em destaque." } },
                    color = HomeZinc400,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DashboardPartnerCard(
    partner: HomeDashboardPartner,
    modifier: Modifier = Modifier.width(152.dp),
    premium: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = when (partner.tier.lowercase()) {
        "ouro" -> HomeGold
        "prata" -> Color(0xFFD4D4D8)
        else -> HomeZinc500
    }
    Surface(
        modifier = modifier
            .height(if (premium) 450.dp else 176.dp)
            .clip(DashboardSectionCardShape)
            .clickable(onClick = onClick),
        shape = DashboardSectionCardShape,
        color = Color.Black,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
    ) {
        Box {
            AsyncImage(
                model = partner.coverUrl ?: partner.logoUrl,
                contentDescription = "Capa ${partner.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.logo_usc),
                error = painterResource(id = R.drawable.logo_usc),
                alpha = if (premium) 0.42f else 0.30f,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))),
            )
            Column(
                modifier = Modifier
                    .align(if (premium) Alignment.BottomStart else Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = if (premium) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = partner.logoUrl ?: partner.coverUrl,
                    contentDescription = "Logo ${partner.name}",
                    modifier = Modifier
                        .size(if (premium) 88.dp else 76.dp)
                        .clip(if (premium) RoundedCornerShape(18.dp) else CircleShape)
                        .background(Color.Black)
                        .border(2.dp, accent.copy(alpha = 0.72f), if (premium) RoundedCornerShape(18.dp) else CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = R.drawable.logo_usc),
                    error = painterResource(id = R.drawable.logo_usc),
                )
                Text(
                    text = partner.name.uppercase(),
                    color = Color.White,
                    fontSize = if (premium) 20.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = if (premium) FontStyle.Italic else FontStyle.Normal,
                    maxLines = if (premium) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (premium) {
                    Text(
                        text = "BENEFÍCIOS EM DESTAQUE PARA A BASE",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardPostCard(
    post: HomeDashboardPost,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = HomeZinc900,
        border = BorderStroke(1.dp, HomeZinc800),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AsyncImage(
                    model = post.avatarUrl,
                    contentDescription = "Avatar de ${post.userName}",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(id = R.drawable.logo_usc),
                    error = painterResource(id = R.drawable.logo_usc),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = post.text,
                        color = HomeZinc400,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Message,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = HomeZinc500,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (post.viewerHasLiked) Color(0xFFEF4444) else HomeZinc500,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(post.likesCount.toString(), color = HomeZinc500, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun DashboardEmptyStoreCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DashboardSectionCardShape)
            .clickable(onClick = onClick),
        shape = DashboardSectionCardShape,
        color = HomeZinc900.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, HomeZinc700),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SEM PRODUTOS NO MOMENTO",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Abra a lojinha para acompanhar as próximas novidades.",
                    color = HomeZinc500,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF7E22CE).copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.35f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = Color(0xFFC084FC),
                )
            }
        }
    }
}

fun turmaDashboardImage(className: String): Int {
    return when (className.filter(Char::isDigit).toIntOrNull()) {
        1 -> R.drawable.turma1
        2 -> R.drawable.turma2
        3 -> R.drawable.turma3
        4 -> R.drawable.turma4
        5 -> R.drawable.turma5
        6 -> R.drawable.turma6
        7 -> R.drawable.turma7
        8 -> R.drawable.turma8
        9 -> R.drawable.turma9
        else -> R.drawable.carteirinha_bg
    }
}
