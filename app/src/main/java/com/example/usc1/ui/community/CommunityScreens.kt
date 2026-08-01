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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun CommunityScreen(
    state: CommunityUiState,
    onTabClick: (String) -> Unit,
    onFilterClick: (CommunityFeedFilter) -> Unit,
    onPostDraftChange: (String) -> Unit,
    onSubmitPost: () -> Unit,
    onPostClick: (CommunityPost) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onHypeClick: (String) -> Unit = {},
    onCommentDraftChange: (String) -> Unit = {},
    onSubmitComment: () -> Unit = {},
    onCloseComments: () -> Unit = {},
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando comunidade", modifier = modifier)
        return
    }

    if (state.openCommentsPostId != null) {
        CommunityCommentsScreen(
            state = state,
            onBackClick = onCloseComments,
            onAuthorClick = onAuthorClick,
            onCommentDraftChange = onCommentDraftChange,
            onSubmitComment = onSubmitComment,
            modifier = modifier,
        )
        return
    }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CommunityFeedFilter.entries.forEach { filter ->
                CommunityFilterPill(
                    filter = filter,
                    selected = state.activeFilter == filter,
                    onClick = { onFilterClick(filter) },
                    modifier = Modifier.weight(1f),
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
            CommunityComposer(
                state = state,
                onPostDraftChange = onPostDraftChange,
                onSubmitPost = onSubmitPost,
            )
        }

        NativeSectionTitle(title = "Feed")
        when {
            state.errorMessage != null && state.allPosts.isEmpty() -> {
                PremiumEmptyState(
                    title = "Erro ao carregar feed",
                    subtitle = state.errorMessage,
                    icon = Icons.Outlined.Block,
                    accent = Color(0xFFEF4444),
                )
            }
            state.posts.isEmpty() -> {
                PremiumEmptyState(
                    title = "Nenhum post em ${state.activeTab}",
                    subtitle = "Seja o primeiro a postar nessa categoria! 🚀",
                    icon = Icons.Outlined.ChatBubbleOutline,
                )
            }
            else -> state.posts.forEach { post ->
                CommunityPostCard(
                    post = post,
                    onClick = { onPostClick(post) },
                    onAuthorClick = onAuthorClick,
                    onCommentClick = onCommentClick,
                    onLikeClick = onLikeClick,
                    onHypeClick = onHypeClick,
                )
            }
        }
    }
}

/** Composer inline do web: avatar + textarea com contador + botão publicar. */
@Composable
private fun CommunityComposer(
    state: CommunityUiState,
    onPostDraftChange: (String) -> Unit,
    onSubmitPost: () -> Unit,
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = PremiumZinc900,
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                if (!state.currentUserAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.currentUserAvatarUrl,
                        contentDescription = "Sua foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.currentUserName.take(1).uppercase().ifBlank { "U" },
                            color = PremiumBrand,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PremiumTextField(
                    value = state.postDraft,
                    onValueChange = onPostDraftChange,
                    label = if (state.currentUserId.isBlank()) {
                        "Entre com sua conta para postar"
                    } else {
                        "Mandar um salve na aba ${state.activeTab}..."
                    },
                    singleLine = false,
                    leadingIcon = Icons.Outlined.ChatBubbleOutline,
                )
                Text(
                    text = "${state.postDraft.length}/${state.postDraftLimit}",
                    color = if (state.postDraft.length >= state.postDraftLimit - 10) {
                        Color(0xFFEF4444)
                    } else {
                        PremiumZinc500
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }

        state.postError?.takeIf(String::isNotBlank)?.let { error ->
            Text(
                text = error,
                color = Color(0xFFF59E0B),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        PremiumPrimaryButton(
            text = if (state.isSubmittingPost) "Enviando..." else "Publicar",
            onClick = onSubmitPost,
            enabled = state.canPublish,
            icon = Icons.AutoMirrored.Outlined.Send,
        )
    }
}

/** Modal de comentários do web, como tela cheia no Android. */
@Composable
private fun CommunityCommentsScreen(
    state: CommunityUiState,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = state.allPosts.firstOrNull { it.id == state.openCommentsPostId }

    PremiumScreen(modifier = modifier, horizontalPadding = 16.dp, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Comentários",
            subtitle = post?.authorName?.let { "Publicação de $it" } ?: "Mural da comunidade",
            icon = Icons.Outlined.ChatBubbleOutline,
            onBackClick = onBackClick,
        )

        if (post != null) {
            CommunityPostCard(post = post, onClick = {}, onAuthorClick = onAuthorClick)
        }

        if (post?.commentsDisabled == true) {
            PremiumEmptyState(
                title = "Comentários trancados",
                subtitle = "A moderação desativou os comentários desta publicação.",
                icon = Icons.Outlined.Block,
                accent = Color(0xFFEF4444),
            )
            return@PremiumScreen
        }

        PremiumCard {
            PremiumTextField(
                value = state.commentDraft,
                onValueChange = onCommentDraftChange,
                label = "Escreva um comentário...",
                singleLine = false,
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
            )
            state.commentError?.takeIf(String::isNotBlank)?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            PremiumPrimaryButton(
                text = if (state.isSubmittingComment) "Enviando..." else "Comentar",
                onClick = onSubmitComment,
                enabled = !state.isSubmittingComment &&
                    state.currentUserId.isNotBlank() &&
                    state.commentDraft.isNotBlank(),
                icon = Icons.AutoMirrored.Outlined.Send,
            )
        }

        when {
            state.commentsLoading -> PremiumLoadingState(text = "Carregando comentários")
            state.comments.isEmpty() -> PremiumEmptyState(
                title = "Seja o primeiro a comentar",
                subtitle = "Ninguém comentou nesta publicação ainda.",
                icon = Icons.Outlined.ChatBubbleOutline,
            )
            else -> state.comments.forEach { comment ->
                CommunityCommentRow(comment = comment, onAuthorClick = onAuthorClick)
            }
        }
    }
}

@Composable
fun CommunityPostUnavailableScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Publicação",
            subtitle = "Post não encontrado no feed carregado",
            icon = Icons.Outlined.Groups,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Post indisponível",
            subtitle = "Volte para a comunidade e tente abrir a publicação novamente.",
            icon = Icons.Outlined.ChatBubbleOutline,
        )
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
                text = if (post.commentsDisabled) {
                    "Comentários trancados pela moderação desta publicação."
                } else {
                    "Interações carregadas da tabela posts. Comentários detalhados entram no próximo bloco nativo."
                },
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
        if (!state.coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = state.coverImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.70f,
                placeholder = painterResource(id = R.drawable.battle_forest),
                fallback = painterResource(id = R.drawable.battle_forest),
                error = painterResource(id = R.drawable.battle_forest),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.battle_forest),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.70f,
            )
        }
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
private fun CommunityFilterPill(
    filter: CommunityFeedFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (filter) {
        CommunityFeedFilter.Recent -> PremiumBrand
        CommunityFeedFilter.Likes -> Color(0xFFEF4444)
        CommunityFeedFilter.Comments -> Color(0xFF3B82F6)
        CommunityFeedFilter.Hype -> Color(0xFFF97316)
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else PremiumZinc900.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, if (selected) accent else PremiumZinc800),
    ) {
        Text(
            text = filter.label.uppercase(),
            color = if (selected) accent else PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
        )
    }
}

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
            state = CommunityMockData.previewState,
            onTabClick = {},
            onFilterClick = {},
            onPostDraftChange = {},
            onSubmitPost = {},
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
