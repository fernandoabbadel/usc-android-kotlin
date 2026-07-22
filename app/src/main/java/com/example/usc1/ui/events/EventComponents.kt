package com.example.usc1.ui.events

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
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventStatus

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PremiumZinc900)
            .border(1.dp, PremiumZinc800, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            EventCover(
                event = event,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    EventMetaLine(event = event)
                }

                EventClassPresenceRow(confirmados = event.availableSpots)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PremiumZinc800, RoundedCornerShape(1.dp))
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "A PARTIR DE",
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = event.priceLabel,
                            color = if (event.status == EventStatus.Open) Color.White else PremiumZinc500,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = event.lotName.uppercase(),
                            color = PremiumBrandAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black,
                            border = BorderStroke(1.dp, PremiumZinc800),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFFEF4444),
                                )
                                Text(
                                    text = event.likesCount.toString(),
                                    color = PremiumZinc400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(42.dp),
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
                }
            }
        }
    }
}

@Composable
fun EventCover(
    event: Event,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        Image(
            painter = painterResource(id = eventImage(event)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.78f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent,
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
            PremiumChip(
                label = event.coverColorName.ifBlank { "Evento" },
                accent = if (event.status == EventStatus.ComingSoon) PremiumAmber else PremiumBrand,
                filled = event.status == EventStatus.Open,
            )
            EventStatusChip(status = event.status)
            if (event.status == EventStatus.Open) {
                PremiumChip(
                    label = "Destaque",
                    icon = Icons.Outlined.Star,
                    accent = PremiumAmber,
                    filled = true,
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.68f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(13.dp), tint = PremiumBrand)
                Text(
                    text = event.dateLabel.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
fun EventStatusChip(
    status: EventStatus,
    modifier: Modifier = Modifier,
) {
    val color = eventStatusColor(status)
    PremiumChip(
        label = status.label,
        modifier = modifier,
        accent = color,
    )
}

@Composable
fun EventMetaLine(event: Event) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventMetaItem(Icons.Outlined.CalendarMonth, event.dateLabel)
            EventMetaItem(Icons.Outlined.Schedule, event.timeLabel)
        }
        EventMetaItem(Icons.Outlined.LocationOn, event.location)
    }
}

@Composable
private fun EventMetaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = PremiumBrand)
        Text(
            text = label,
            color = PremiumZinc400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EventClassPresenceRow(confirmados: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, PremiumZinc800, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(2.dp, PremiumZinc900),
                ) {
                    Image(
                        painter = painterResource(
                            id = if (index % 2 == 0) R.drawable.logo_aaakn else R.drawable.logo_usc
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Column {
            Text(
                text = "PRESENÇA",
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "+$confirmados confirmados",
                color = PremiumBrandAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

fun eventStatusColor(status: EventStatus): Color = when (status) {
    EventStatus.Open -> PremiumBrand
    EventStatus.Closed -> PremiumZinc500
    EventStatus.SoldOut -> PremiumRed
    EventStatus.ComingSoon -> PremiumAmber
}

private fun eventImage(event: Event): Int = when (event.status) {
    EventStatus.Open -> R.drawable.battle_forest
    EventStatus.ComingSoon -> R.drawable.logo_usc_wide
    EventStatus.SoldOut -> R.drawable.carteirinha_bg
    EventStatus.Closed -> R.drawable.logo_platform_web
}
