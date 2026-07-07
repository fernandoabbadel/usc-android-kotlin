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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
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
) {
    val accent = communityStatusColor(post.status)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900.copy(alpha = if (post.status == CommunityPostStatus.Blocked) 0.62f else 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            if (post.status == CommunityPostStatus.Blocked) {
                PremiumChip(label = "Post bloqueado", icon = Icons.Outlined.Flag, accent = PremiumRed)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(2.dp, accent),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_aaakn),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${post.authorRole} • ${post.timeLabel}",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                PremiumChip(
                    label = if (post.status == CommunityPostStatus.Pinned) "Fixado" else post.category,
                    icon = if (post.status == CommunityPostStatus.Pinned) Icons.Outlined.PushPin else Icons.Outlined.Groups,
                    accent = accent,
                )
            }
            Text(
                text = post.title,
                color = Color.White,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = post.body,
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.dp, PremiumZinc800, RoundedCornerShape(20.dp)),
            ) {
                Image(
                    painter = painterResource(id = post.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = if (post.status == CommunityPostStatus.Blocked) 0.30f else 0.78f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            ),
                        ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommunityMetric(Icons.Outlined.FavoriteBorder, "${post.likes}", PremiumAmber)
                CommunityMetric(Icons.Outlined.ChatBubbleOutline, "${post.comments}", PremiumBrand)
                CommunityMetric(Icons.Outlined.Flag, "${post.reports}", if (post.reports > 0) PremiumRed else PremiumZinc500)
            }
        }
    }
}

@Composable
private fun CommunityMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = color)
        Text(text = value, color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
