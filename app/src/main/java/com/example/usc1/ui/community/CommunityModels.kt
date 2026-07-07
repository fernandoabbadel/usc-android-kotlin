package com.example.usc1.ui.community

import com.example.usc1.R

enum class CommunityPostStatus(val label: String) {
    Published("Publicado"),
    Pinned("Fixado"),
    Blocked("Bloqueado"),
}

data class CommunityPost(
    val id: String,
    val authorName: String,
    val authorRole: String,
    val category: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val status: CommunityPostStatus,
    val likes: Int,
    val comments: Int,
    val reports: Int,
    val imageRes: Int,
)

data class CommunityUiState(
    val title: String = "Comunidade da Atlética",
    val subtitle: String = "Feed, avisos e interações da base",
    val isUserBanned: Boolean = false,
    val activeTab: String = "Todos",
    val tabs: List<String> = listOf("Todos", "Avisos", "Social", "Treinos", "Loja"),
    val posts: List<CommunityPost> = CommunityMockData.posts,
)

object CommunityMockData {
    val posts = listOf(
        CommunityPost(
            id = "post-001",
            authorName = "Fernando USC",
            authorRole = "Membro ativo",
            category = "Avisos",
            title = "Intermed USC com vendas abertas",
            body = "O lote atual está no ar. Garanta o ingresso, acompanhe pedidos e confira regras de retirada no app.",
            timeLabel = "Agora",
            status = CommunityPostStatus.Pinned,
            likes = 128,
            comments = 24,
            reports = 0,
            imageRes = R.drawable.battle_forest,
        ),
        CommunityPost(
            id = "post-002",
            authorName = "Comissão Atlética",
            authorRole = "Moderação",
            category = "Treinos",
            title = "Check-in do treino liberado",
            body = "Quem estiver no ginásio já pode validar presença pelo scanner visual. Pontuação entra no ranking mockado.",
            timeLabel = "12 min",
            status = CommunityPostStatus.Published,
            likes = 76,
            comments = 11,
            reports = 0,
            imageRes = R.drawable.logo_usc_wide,
        ),
        CommunityPost(
            id = "post-003",
            authorName = "Usuário bloqueado",
            authorRole = "Conta restrita",
            category = "Social",
            title = "Publicação moderada",
            body = "Conteúdo ocultado por denúncia e regra da comunidade. Mantido visualmente para revisão.",
            timeLabel = "Ontem",
            status = CommunityPostStatus.Blocked,
            likes = 4,
            comments = 0,
            reports = 3,
            imageRes = R.drawable.logo_aaakn,
        ),
    )

    fun postById(id: String): CommunityPost =
        posts.firstOrNull { it.id == id } ?: posts.first()
}
