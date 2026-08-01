package com.example.usc1.domain.model

/**
 * Contrato do perfil público, espelhando `PROFILE_USER_SELECT_COLUMNS`
 * de `web-reference/src/lib/profilePublicService.ts`.
 */
data class UserProfileDetail(
    val uid: String = "",
    val nome: String = "",
    val apelido: String = "",
    val foto: String? = null,
    val turma: String = "",
    val bio: String = "",
    val cidadeOrigem: String = "",
    val dataNascimento: String = "",
    val instagram: String = "",
    val instagramPublico: Boolean = false,
    val telefone: String = "",
    val whatsappPublico: Boolean = false,
    val idadePublica: Boolean = true,
    val relacionamentoPublico: Boolean = false,
    val statusRelacionamento: String = "",
    val signo: String = "",
    val signoPublico: Boolean = false,
    val ascendente: String = "",
    val ascendentePublico: Boolean = false,
    val lugarEspecial: List<String> = emptyList(),
    val comidaPreferida: List<String> = emptyList(),
    val musicaPreferida: List<String> = emptyList(),
    val corPreferida: String = "",
    val esportes: List<String> = emptyList(),
    val pets: String = "",
    val role: String = "",
    val tenantId: String = "",
    val tenantRole: String = "",
    val status: String = "",
    val profilePublic: Boolean = true,
    val profilePhotoPublic: Boolean = true,
    val allowProfileDiscovery: Boolean = true,
    val plano: String = "",
    val planoCor: String = "",
    val planoIcon: String = "",
    val patente: String = "",
    val patenteIcon: String = "",
    val patenteCor: String = "",
    val tier: String = "",
    val level: Int = 0,
    val xp: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val arenaWins: Int = 0,
    val arenaLosses: Int = 0,
) {
    val displayName: String
        get() = apelido.trim().ifBlank { nome.trim().substringBefore(" ") }.ifBlank { "Atleta" }

    val isPaused: Boolean
        get() = status.equals("paused", ignoreCase = true)

    val isMiniVendor: Boolean
        get() = tenantRole.equals("mini_vendor", ignoreCase = true)

    val isAdminLike: Boolean
        get() = AdminLikeRoles.any { it.equals(role, ignoreCase = true) || it.equals(tenantRole, ignoreCase = true) }

    private companion object {
        val AdminLikeRoles = listOf(
            "admin",
            "master",
            "diretoria",
            "presidente",
            "financeiro",
            "tenant_admin",
            "owner",
        )
    }
}

data class ProfilePost(
    val id: String,
    val texto: String,
    val imagem: String? = null,
    val likes: Int = 0,
    val comentarios: Int = 0,
    val createdAt: String = "",
    val timeLabel: String = "",
)

data class ProfileEventItem(
    val id: String,
    val titulo: String,
    val data: String = "",
    val local: String = "",
    val imagem: String? = null,
)

data class ProfileTrainingItem(
    val id: String,
    val modalidade: String,
    val dia: String = "",
    val horario: String = "",
    val local: String = "",
    val imagem: String? = null,
)

data class ProfileLeagueItem(
    val id: String,
    val nome: String,
    val sigla: String = "",
    val logo: String? = null,
    val membros: Int = 0,
)

data class ProfileFollowUser(
    val uid: String,
    val nome: String,
    val foto: String? = null,
    val turma: String = "",
)

data class ProfileBundle(
    val profile: UserProfileDetail,
    val posts: List<ProfilePost> = emptyList(),
    val eventos: List<ProfileEventItem> = emptyList(),
    val treinos: List<ProfileTrainingItem> = emptyList(),
    val ligas: List<ProfileLeagueItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val affinitySent: Boolean = false,
)
