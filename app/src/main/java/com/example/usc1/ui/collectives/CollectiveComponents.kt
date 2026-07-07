package com.example.usc1.ui.collectives

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumGold
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun LeagueCard(
    league: CollectiveGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveCard(group = league, onClick = onClick, modifier = modifier)
}

@Composable
fun DirectoryCard(
    directory: CollectiveGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveCard(group = directory, onClick = onClick, modifier = modifier)
}

@Composable
fun CommissionCard(
    commission: CollectiveGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveCard(group = commission, onClick = onClick, modifier = modifier)
}

@Composable
fun CollectiveCard(
    group: CollectiveGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(202.dp)
                    .background(Color.Black),
            ) {
                Image(
                    painter = painterResource(id = group.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.70f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.42f),
                                    PremiumZinc900,
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PremiumChip(label = group.kind.label, icon = Icons.Outlined.Groups, accent = accent, filled = true)
                    PremiumChip(label = group.status, accent = accent)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(42.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = Color.Black,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Text(
                    text = group.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.description,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CollectiveSmallMetric("${group.memberCount}", "membros", Icons.Outlined.Groups, accent)
                    CollectiveSmallMetric("${group.agenda.size}", "agenda", Icons.Outlined.CalendarMonth, accent)
                    CollectiveSmallMetric("${group.store.size}", "loja", Icons.Outlined.Storefront, accent)
                }
            }
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
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = subtitle,
                color = PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CollectiveSmallMetric(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = accent)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(text = label.uppercase(), color = PremiumZinc500, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

fun collectiveAccent(group: CollectiveGroup): Color = when (group.kind) {
    CollectiveKind.League -> PremiumBrand
    CollectiveKind.Directory -> PremiumPurple
    CollectiveKind.Commission -> PremiumAmber
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun CollectiveCardPreview() {
    UscTheme(darkTheme = true) {
        LeagueCard(
            league = CollectiveMockData.leagues.first(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
