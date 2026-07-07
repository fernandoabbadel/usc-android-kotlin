package com.example.usc1.ui.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun CommunityScreen(
    state: CommunityUiState,
    onTabClick: (String) -> Unit,
    onPostClick: (CommunityPost) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        horizontalPadding = 16.dp,
        bottomPadding = 116.dp,
    ) {
        CommunityHero(state = state)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.tabs.forEach { tab ->
                CommunityTabPill(
                    label = tab,
                    selected = state.activeTab == tab,
                    onClick = { onTabClick(tab) },
                )
            }
        }

        if (state.isUserBanned) {
            PremiumCard(accent = Color(0xFFEF4444)) {
                PremiumChip(label = "Usuário banido", icon = Icons.Outlined.Block, accent = Color(0xFFEF4444))
                Text(
                    text = "Você pode ler avisos públicos, mas não pode publicar, comentar ou reagir até revisão da moderação.",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            PremiumCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PUBLICAR NO FEED",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Entrada mockada para manter visual da comunidade.",
                            color = PremiumZinc500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                PremiumPrimaryButton(text = "Nova publicação", onClick = {}, icon = Icons.Outlined.Send)
            }
        }

        NativeSectionTitle(title = "Feed")
        state.posts.forEach { post ->
            CommunityPostCard(
                post = post,
                onClick = { onPostClick(post) },
            )
        }
    }
}

@Composable
fun CommunityPostDetailScreen(
    post: CommunityPost,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Publicação",
            subtitle = "${post.category} • ${post.timeLabel}",
            icon = Icons.Outlined.Groups,
            accent = communityStatusColor(post.status),
            onBackClick = onBackClick,
        )
        CommunityPostCard(post = post, onClick = {})
        PremiumCard(accent = communityStatusColor(post.status)) {
            Text(
                text = "INTERAÇÕES",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${post.likes} hypes • ${post.comments} comentários • ${post.reports} denúncias",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Comentários e denúncias estão mockados neste bloco, sem rede e sem moderação real.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CommunityHero(state: CommunityUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.battle_forest),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.70f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp)
                .then(
                    Modifier,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .clickable(enabled = false) {},
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        Modifier,
                    ),
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.28f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .backgroundOverlay(),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PremiumChip(label = "Feed USC", icon = Icons.Outlined.Groups, accent = PremiumBrand, filled = true)
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 31.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    )
                    Text(
                        text = state.subtitle,
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun Modifier.backgroundOverlay(): Modifier =
    this.then(
        Modifier
            .fillMaxSize()
            .padding(0.dp),
    )

@Composable
private fun CommunityTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) PremiumBrand else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) PremiumBrand else PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color.Black else PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CommunityScreenPreview() {
    UscTheme(darkTheme = true) {
        CommunityScreen(
            state = CommunityUiState(),
            onTabClick = {},
            onPostClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CommunityPostDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        CommunityPostDetailScreen(
            post = CommunityMockData.posts.first(),
            onBackClick = {},
        )
    }
}
