package com.example.usc1.domain.model

/** `RankingUserRecord` de `web-reference/src/lib/rankingService.ts`. */
data class RankingUser(
    val id: String,
    val name: String,
    val nickname: String = "",
    val photoUrl: String? = null,
    val className: String = "GERAL",
    val xp: Int = 0,
) {
    /** `apelido || nome.split(' ')[0]` do pódio de `/ranking`. */
    val podiumLabel: String
        get() = nickname.ifBlank { name.trim().substringBefore(' ') }.ifBlank { name }
}

/** Agregado por turma calculado na própria página `/ranking`. */
data class RankingClass(
    val id: String,
    val name: String,
    val points: Int,
    val members: Int,
)

object RankingCatalog {
    const val MaxGlobalResults = 100
    const val MaxClassResults = 50

    /** `turmasMap` da aba "Por Turma": soma o xp e conta membros por turma. */
    fun aggregateClasses(users: List<RankingUser>): List<RankingClass> {
        val grouped = LinkedHashMap<String, RankingClass>()
        users.forEach { user ->
            val key = user.className.trim().uppercase().ifBlank { "GERAL" }
            val current = grouped[key] ?: RankingClass(id = key, name = key, points = 0, members = 0)
            grouped[key] = current.copy(
                points = current.points + user.xp,
                members = current.members + 1,
            )
        }
        return grouped.values.sortedByDescending { it.points }
    }
}
