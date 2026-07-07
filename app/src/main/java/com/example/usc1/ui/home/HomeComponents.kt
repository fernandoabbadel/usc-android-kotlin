package com.example.usc1.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.example.usc1.R

val HomeBlack = Color(0xFF050505)
val HomeZinc950 = Color(0xFF09090B)
val HomeZinc900 = Color(0xFF18181B)
val HomeZinc800 = Color(0xFF27272A)
val HomeZinc700 = Color(0xFF3F3F46)
val HomeZinc500 = Color(0xFF71717A)
val HomeZinc400 = Color(0xFFA1A1AA)
val HomeBrand = Color(0xFF10B981)
val HomeBrandAccent = Color(0xFF34D399)
val HomeGold = Color(0xFFEAB308)
val HomeAmber = Color(0xFFF59E0B)

private val DashboardCardShape = RoundedCornerShape(28.dp)

@Composable
fun DashboardHeader(
    firstName: String,
    tenantName: String,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Fala, $firstName!",
                color = Color.White,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Pronto para dominar?",
                color = HomeZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            NeonStatusChip(
                label = tenantName.uppercase(),
                icon = Icons.Outlined.CheckCircle,
            )
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(HomeZinc900)
                .border(2.dp, HomeBrand, CircleShape)
                .clickable(onClick = onAvatarClick)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_aaakn),
                contentDescription = "Perfil",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun NeonStatusChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = HomeBrandAccent,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = HomeBrand.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, HomeBrand.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = color,
                )
            }
            Text(
                text = label,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun SalesModeCard(
    eventTitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(DashboardCardShape)
            .clickable(onClick = onClick)
            .background(HomeZinc900)
            .border(1.dp, HomeAmber.copy(alpha = 0.62f), DashboardCardShape),
    ) {
        Image(
            painter = painterResource(id = R.drawable.battle_forest),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.38f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color(0xEE451A03),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeonStatusChip(
                    label = "MODO VENDAS",
                    icon = Icons.Outlined.Storefront,
                    color = Color(0xFFFCD34D),
                )
                Text(
                    text = "Menu do evento",
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = eventTitle.uppercase(),
                    color = HomeZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MembershipHomeCard(
    membershipCode: String,
    planName: String,
    tenantName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(DashboardCardShape)
            .clickable(onClick = onClick)
            .background(HomeZinc900)
            .border(1.dp, HomeZinc800, DashboardCardShape),
    ) {
        Image(
            painter = painterResource(id = R.drawable.carteirinha_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.42f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.86f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Image(
            painter = painterResource(id = R.drawable.logo_usc),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(108.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.22f,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = HomeBrand,
                )
                NeonStatusChip(label = "SÓCIO ATIVO")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Carteirinha",
                color = Color.White,
                fontSize = 27.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$tenantName • $planName".uppercase(),
                color = HomeZinc400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = membershipCode,
                color = HomeBrandAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun PremiumDashboardCard(
    title: String,
    eyebrow: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = HomeBrand,
    backgroundImageRes: Int? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(DashboardCardShape)
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        HomeZinc900,
                        Color.Black,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.30f), DashboardCardShape),
    ) {
        if (backgroundImageRes != null) {
            Image(
                painter = painterResource(id = backgroundImageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.2f,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-18).dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = accent,
                    )
                }
                Text(
                    text = eyebrow.uppercase(),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = body,
                        color = HomeZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = accent,
                    )
                }
            }
        }
    }
}

@Composable
fun RadarAlbumCard(
    foundCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(DashboardCardShape)
            .clickable(onClick = onClick)
            .background(Color.Black)
            .border(1.dp, HomeBrand.copy(alpha = 0.60f), DashboardCardShape),
    ) {
        NeonRadarBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = HomeBrand,
                        )
                        Text(
                            text = "Caça aos Calouros",
                            color = HomeBrand,
                            fontSize = 20.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "STATUS: EM OPERAÇÃO",
                        color = HomeZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HomeBrand.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, HomeBrand.copy(alpha = 0.45f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp),
                        tint = HomeBrand,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = foundCount.toString(),
                            color = HomeBrandAccent,
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "/$totalCount",
                            color = HomeZinc500,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        text = "ENCONTRADOS",
                        color = HomeZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                NeonStatusChip(
                    label = "ABRIR ÁLBUM",
                    icon = Icons.Outlined.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun NeonRadarBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = 22.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = HomeBrand.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += grid
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = HomeBrand.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += grid
        }

        val center = Offset(size.width * 0.56f, size.height * 0.54f)
        drawCircle(HomeBrand.copy(alpha = 0.16f), radius = size.width * 0.32f, center = center)
        drawCircle(
            color = HomeBrand.copy(alpha = 0.28f),
            radius = size.width * 0.52f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
        )
        drawCircle(
            color = HomeBrand.copy(alpha = 0.22f),
            radius = size.width * 0.34f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
        )
    }
}

@Composable
fun DashboardSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Star,
    accent: Color = HomeBrand,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
        )
    }
}

@Composable
fun FloatingBottomNavigation(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onEventsClick: () -> Unit,
    onScannerClick: () -> Unit,
    onWalletClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = RoundedCornerShape(27.dp),
            color = HomeZinc950.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    label = "Início",
                    icon = Icons.Outlined.Home,
                    selected = true,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = "Eventos",
                    icon = Icons.Outlined.Event,
                    onClick = onEventsClick,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(74.dp))
                BottomNavItem(
                    label = "Carteira",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onWalletClick,
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = "Menu",
                    icon = Icons.Outlined.Menu,
                    onClick = onMenuClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ScannerCenterButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            onClick = onScannerClick,
        )
    }
}

@Composable
fun ScannerCenterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.size(86.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(HomeBrand.copy(alpha = 0.18f)),
        )
        Surface(
            modifier = Modifier
                .size(66.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = HomeBrand,
            border = BorderStroke(4.dp, HomeZinc950),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "Scanner",
                    modifier = Modifier.size(30.dp),
                    tint = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) HomeBrandAccent else HomeZinc500
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun iconFor(kind: QuickActionKind): ImageVector = when (kind) {
    QuickActionKind.MembershipCard -> Icons.Outlined.CreditCard
    QuickActionKind.Events -> Icons.Outlined.Event
    QuickActionKind.Store -> Icons.Outlined.Storefront
    QuickActionKind.Training -> Icons.Outlined.FitnessCenter
    QuickActionKind.Community,
    QuickActionKind.Leagues,
    -> Icons.Outlined.Groups
    QuickActionKind.Profile -> Icons.Outlined.Person
}
