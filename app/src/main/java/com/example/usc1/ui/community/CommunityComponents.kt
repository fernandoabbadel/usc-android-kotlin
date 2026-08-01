package com.example.usc1.ui.community

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onHypeClick: (String) -> Unit = {},
) {
    val accent = communityStatusColor(post.status)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, PremiumZinc800.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            if (post.status == CommunityPostStatus.Blocked) {
                PremiumChip(label = "Post bloqueado", icon = Icons.Outlined.Flag, accent = PremiumRed)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(enabled = post.userId.isNotBlank()) { onAuthorClick(post.userId) },
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(2.dp, accent),
                ) {
                    if (!post.authorAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = post.imageRes),
                            fallback = painterResource(id = post.imageRes),
                            error = painterResource(id = post.imageRes),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = post.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = post.userId.isNotBlank()) { onAuthorClick(post.userId) },
                ) {
                    Text(
                        text = post.authorName,
                        color = communityPlanColor(post.planColorKey),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(post.handle, post.authorRole, post.timeLabel)
                            .filter(String::isNotBlank)
                            .joinToString(" • "),
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (post.status == CommunityPostStatus.Pinned) {
                    PremiumChip(label = "Fixado", icon = Icons.Outlined.PushPin, accent = accent)
                }
            }
            Text(
                text = post.body,
                color = Color(0xFFD4D4D8),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
            if (!post.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(1.dp, PremiumZinc800, RoundedCornerShape(18.dp)),
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (post.status == CommunityPostStatus.Blocked) 0.30f else 1f,
                        placeholder = painterResource(id = post.imageRes),
                        fallback = painterResource(id = post.imageRes),
                        error = painterResource(id = post.imageRes),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                                ),
                            ),
                    )
                }
            }
            // Barra de ações do web: comentar | curtir | hype | denúncias.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommunityMetric(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    value = if (post.commentsDisabled) "${post.comments} 🔒" else "${post.comments}",
                    color = PremiumZinc500,
                    onClick = { onCommentClick(post.id) },
                )
                CommunityMetric(
                    icon = if (post.likedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    value = "${post.likes}",
                    color = if (post.likedByMe) PremiumRed else PremiumZinc500,
                    valueColor = if (post.likedByMe) PremiumRed else PremiumZinc400,
                    onClick = { onLikeClick(post.id) },
                )
                CommunityMetric(
                    icon = Icons.Outlined.LocalFireDepartment,
                    value = "${post.hype}",
                    color = if (post.hypedByMe) PremiumAmber else PremiumZinc500,
                    valueColor = if (post.hypedByMe) PremiumAmber else PremiumZinc400,
                    onClick = { onHypeClick(post.id) },
                )
                CommunityMetric(
                    icon = Icons.Outlined.Flag,
                    value = if (post.reports > 0) "${post.reports}" else "",
                    color = if (post.reports > 0) PremiumRed else PremiumZinc500.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun CommunityMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color,
    valueColor: Color = PremiumZinc400,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        if (value.isNotBlank()) {
            Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Espelha `resolvePlanTextClass` do web-reference. */
fun communityPlanColor(colorKey: String): Color {
    val key = colorKey.lowercase()
    return when {
        key.contains("emerald") || key.contains("green") -> PremiumBrand
        key.contains("amber") || key.contains("yellow") || key.contains("gold") -> PremiumAmber
        key.contains("red") || key.contains("rose") -> PremiumRed
        key.contains("blue") || key.contains("cyan") -> Color(0xFF60A5FA)
        key.contains("purple") || key.contains("violet") -> Color(0xFFA855F7)
        key.contains("orange") -> Color(0xFFF97316)
        else -> Color.White
    }
}

@Composable
fun CommunityCommentRow(
    comment: CommunityComment,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clickable(enabled = comment.userId.isNotBlank()) { onAuthorClick(comment.userId) },
            shape = CircleShape,
            color = PremiumZinc900,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            if (!comment.authorAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = comment.authorAvatarUrl,
                    contentDescription = comment.authorName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        color = PremiumBrand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = comment.authorName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.timeLabel.isNotBlank()) {
                    Text(
                        text = comment.timeLabel,
                        color = PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = comment.body,
                color = Color(0xFFD4D4D8),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

fun communityStatusColor(status: CommunityPostStatus): Color = when (status) {
    CommunityPostStatus.Published -> PremiumBrand
    CommunityPostStatus.Pinned -> PremiumAmber
    CommunityPostStatus.Blocked -> PremiumRed
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CommunityPostCardPreview() {
    UscTheme(darkTheme = true) {
        CommunityPostCard(
            post = CommunityMockData.posts.first(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
