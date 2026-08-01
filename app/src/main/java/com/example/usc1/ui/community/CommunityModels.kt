package com.example.usc1.ui.community

import com.example.usc1.R

enum class CommunityPostStatus(val label: String) {
    Published("Publicado"),
    Pinned("Fixado"),
    Blocked("Bloqueado"),
}

enum class CommunityFeedFilter(val label: String) {
    Recent("Recentes"),
    Likes("Em alta"),
    Comments("Polêmicos"),
    Hype("Hypados"),
}

data class CommunityPost(
    val id: String,
    val userId: String = "",
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val handle: String = "",
    val authorRole: String,
    val category: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val status: CommunityPostStatus,
    val likes: Int,
    val hype: Int = 0,
    val comments: Int,
    val reports: Int,
    val commentsDisabled: Boolean = false,
    val imageUrl: String? = null,
    val imageRes: Int = R.drawable.logo_usc,
    val likedByMe: Boolean = false,
    val hypedByMe: Boolean = false,
    val planColorKey: String = "",
    val patente: String = "",
)

data class CommunityComment(
    val id: String,
    val postId: String,
    val userId: String = "",
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val authorRole: String = "",
    val body: String,
    val timeLabel: String = "",
    val likes: Int = 0,
)

data class CommunityUiState(
    val title: String = "Comunidade da Atlética",
    val subtitle: String = "Espaço oficial da atlética",
    val coverImageUrl: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val currentUserAvatarUrl: String? = null,
    val isUserBanned: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val postDraft: String = "",
    val isSubmittingPost: Boolean = false,
    val postError: String? = null,
    val activeTab: String = DefaultCommunityCategories.first(),
    val activeFilter: CommunityFeedFilter = CommunityFeedFilter.Recent,
    val maxVisiblePosts: Int = 20,
    val tabs: List<String> = DefaultCommunityCategories,
    val allPosts: List<CommunityPost> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val openCommentsPostId: String? = null,
    val comments: List<CommunityComment> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentDraft: String = "",
    val isSubmittingComment: Boolean = false,
    val commentError: String? = null,
) {
    /** Limite de caracteres do textarea do web (`maxLength={150}`). */
    val postDraftLimit: Int get() = 150

    val canPublish: Boolean
        get() = !isSubmittingPost && !isUserBanned && currentUserId.isNotBlank() && postDraft.isNotBlank()
}

val DefaultCommunityCategories = listOf(
    "Geral",
    "Futebol",
    "Vôlei",
    "Basquete",
    "Handebol",
    "Sinuca",
    "Truco",
    "Natação",
    "Bateria",
    "Cheerleaders",
    "Sugestões",
)

object CommunityMockData {
    val posts = listOf(
        CommunityPost(
            id = "post-001",
            userId = "user-001",
            authorName = "Fernando USC",
            handle = "@fernando",
            authorRole = "Membro ativo",
            category = "Geral",
            title = "Post fixado",
            body = "O lote atual está no ar. Garanta o ingresso, acompanhe pedidos e confira regras de retirada no app.",
            timeLabel = "Agora",
            status = CommunityPostStatus.Pinned,
            likes = 128,
            hype = 41,
            comments = 24,
            reports = 0,
            imageRes = R.drawable.battle_forest,
        ),
        CommunityPost(
            id = "post-002",
            userId = "user-002",
            authorName = "Comissão Atlética",
            handle = "@atletica",
            authorRole = "Moderação",
            category = "Vôlei",
            title = "Check-in do treino liberado",
            body = "Quem estiver no ginásio já pode validar presença pelo scanner visual. Pontuação entra no ranking.",
            timeLabel = "12 min",
            status = CommunityPostStatus.Published,
            likes = 76,
            hype = 12,
            comments = 11,
            reports = 0,
            imageRes = R.drawable.logo_usc_wide,
        ),
        CommunityPost(
            id = "post-003",
            userId = "user-003",
            authorName = "Usuário bloqueado",
            handle = "@moderacao",
            authorRole = "Conta restrita",
            category = "Social",
            title = "Publicação moderada",
            body = "Conteúdo ocultado por denúncia e regra da comunidade. Mantido visualmente para revisão.",
            timeLabel = "Ontem",
            status = CommunityPostStatus.Blocked,
            likes = 4,
            hype = 0,
            comments = 0,
            reports = 3,
            imageRes = R.drawable.logo_usc,
        ),
    )

    val previewState = CommunityUiState(
        currentUserName = "Fernando Lopes Abbade",
        activeTab = "Geral",
        activeFilter = CommunityFeedFilter.Recent,
        tabs = DefaultCommunityCategories,
        allPosts = posts,
        posts = posts.filter { it.category == "Geral" },
    )

    fun postById(id: String): CommunityPost =
        posts.firstOrNull { it.id == id } ?: posts.first()
}
