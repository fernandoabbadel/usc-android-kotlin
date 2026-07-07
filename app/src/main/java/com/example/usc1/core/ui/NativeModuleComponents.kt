package com.example.usc1.core.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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

data class NativeAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color = PremiumBrand,
    val badge: String? = null,
)

@Composable
fun NativeModuleHeroCard(
    title: String,
    subtitle: String,
    body: String,
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    status: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(252.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(30.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.64f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.58f),
                            Color.Black,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumChip(label = subtitle, accent = accent, filled = true)
                    if (status != null) {
                        PremiumChip(label = status, accent = accent)
                    }
                }
                if (onClick != null) {
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 31.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun NativeActionCard(
    action: NativeAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumMenuRow(
        title = action.title,
        subtitle = action.subtitle,
        icon = action.icon,
        accent = action.accent,
        badge = action.badge,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun NativeStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    icon: ImageVector = Icons.Outlined.CheckCircle,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
fun NativeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Text(
        text = title.uppercase(),
        color = accent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = modifier.padding(start = 2.dp),
    )
}

@Composable
fun NativeProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp),
    ) {
        val radius = CornerRadius(20f, 20f)
        drawRoundRect(color = PremiumZinc800, cornerRadius = radius)
        drawRoundRect(
            color = accent,
            size = size.copy(width = size.width * progress.coerceIn(0f, 1f)),
            cornerRadius = radius,
        )
    }
}
