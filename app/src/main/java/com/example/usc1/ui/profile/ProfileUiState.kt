package com.example.usc1.ui.profile

import com.example.usc1.core.session.UserSession
import com.example.usc1.domain.model.ProfileEventItem
import com.example.usc1.domain.model.ProfileFollowUser
import com.example.usc1.domain.model.ProfileLeagueItem
import com.example.usc1.domain.model.ProfilePost
import com.example.usc1.domain.model.ProfileTrainingItem
import com.example.usc1.domain.model.UserProfileDetail

enum class ProfileTab(val label: String) {
    Posts("Posts"),
    Eventos("Eventos"),
    Treinos("Treinos"),
    Ligas("Ligas"),
}

enum class ProfileFollowListMode {
    Followers,
    Following,
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profile: ProfileUserUiModel = ProfileUserUiModel(),
    val detail: UserProfileDetail? = null,
    val activeTab: ProfileTab = ProfileTab.Posts,
    val posts: List<ProfilePost> = emptyList(),
    val eventos: List<ProfileEventItem> = emptyList(),
    val treinos: List<ProfileTrainingItem> = emptyList(),
    val ligas: List<ProfileLeagueItem> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = true,
    val affinitySent: Boolean = false,
    val isSubmittingFollow: Boolean = false,
    val isSubmittingAffinity: Boolean = false,
    val actionMessage: String? = null,
    val followListMode: ProfileFollowListMode? = null,
    val followListLoading: Boolean = false,
    val followList: List<ProfileFollowUser> = emptyList(),
    val shortcuts: List<ProfileShortcutUiModel> = emptyList(),
) {
    val preferenceBadges: List<ProfilePreferenceBadge>
        get() {
            val source = detail ?: return emptyList()
            val badges = mutableListOf<ProfilePreferenceBadge>()
            source.musicaPreferida.forEach { badges += ProfilePreferenceBadge("🎵", it) }
            source.comidaPreferida.forEach { badges += ProfilePreferenceBadge("🍽️", it) }
            source.lugarEspecial.forEach { badges += ProfilePreferenceBadge("📍", it) }
            source.esportes.forEach { badges += ProfilePreferenceBadge("🏆", it) }
            if (source.corPreferida.isNotBlank()) {
                badges += ProfilePreferenceBadge("🎨", source.corPreferida)
            }
            if (source.pets.isNotBlank()) {
                badges += ProfilePreferenceBadge("🐾", source.pets)
            }
            return badges
        }

    val age: Int?
        get() = detail?.dataNascimento?.let(::calculateAge)

    val showAge: Boolean
        get() = detail?.let { it.idadePublica || isOwnProfile } == true

    val showInstagram: Boolean
        get() = detail?.let { it.instagram.isNotBlank() && (it.instagramPublico || isOwnProfile) } == true

    val showWhatsapp: Boolean
        get() = detail?.let { it.telefone.isNotBlank() && (it.whatsappPublico || isOwnProfile) } == true

    val showRelationship: Boolean
        get() = detail?.let {
            it.statusRelacionamento.isNotBlank() && (it.relacionamentoPublico || isOwnProfile)
        } == true

    val showSign: Boolean
        get() = detail?.let { it.signo.isNotBlank() && (it.signoPublico || isOwnProfile) } == true
}

data class ProfilePreferenceBadge(
    val icon: String,
    val label: String,
)

data class ProfileUserUiModel(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val initials: String = "",
    val course: String = "",
    val className: String = "",
    val classPhotoUrl: String? = null,
    val tenantName: String = "",
    val role: String = "",
    val accountStatus: String = "",
    val activePlan: String = "",
    val planColorKey: String = "zinc",
    val memberSince: String = "",
)

data class ProfileShortcutUiModel(
    val title: String,
    val description: String,
    val route: String,
)

fun ProfileUiState.withSession(session: UserSession): ProfileUiState {
    val user = session.user ?: return copy(profile = ProfileUserUiModel())
    val name = user.name.ifBlank { user.email.substringBefore("@") }
    return copy(
        profile = profile.copy(
            name = profile.name.ifBlank { name },
            email = user.email,
            avatarUrl = profile.avatarUrl ?: user.avatarUrl,
            initials = name.initials(),
            className = profile.className.ifBlank { user.classCode },
            classPhotoUrl = profile.classPhotoUrl ?: user.classPhotoUrl,
            tenantName = session.tenant?.name.orEmpty(),
            role = profile.role.ifBlank { user.role.remoteValue },
            accountStatus = profile.accountStatus.ifBlank { user.status.remoteValue },
            activePlan = profile.activePlan.ifBlank { user.planName },
            planColorKey = user.planColorKey,
        ),
    )
}

internal fun UserProfileDetail.toUiModel(fallbackEmail: String): ProfileUserUiModel {
    return ProfileUserUiModel(
        name = nome,
        email = fallbackEmail,
        avatarUrl = foto,
        initials = nome.initials(),
        className = turma,
        role = role,
        accountStatus = status,
        activePlan = plano,
        planColorKey = planoCor.ifBlank { "zinc" },
    )
}

private fun String.initials(): String {
    return trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "US" }
}

/** Espelha `calculateAgeFromBirthDate` do web-reference. */
internal fun calculateAge(birthDate: String): Int? {
    val clean = birthDate.trim()
    if (clean.isBlank()) return null
    val parsed = runCatching { java.time.LocalDate.parse(clean.take(10)) }
        .getOrElse {
            runCatching {
                val parts = clean.split("/")
                if (parts.size != 3) return@runCatching null
                java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }.getOrNull()
        } ?: return null
    val today = java.time.LocalDate.now()
    if (parsed.isAfter(today)) return null
    return java.time.Period.between(parsed, today).years.takeIf { it in 0..130 }
}
