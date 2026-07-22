package com.example.usc1.domain.model

data class AdminArenaUser(
    val id: String,
    val name: String,
    val nickname: String,
    val className: String,
    val photoUrl: String,
    val xp: Int,
    val sharkCoins: Int,
    val planBadge: String,
    val stats: Map<String, Double>,
)

data class AdminArenaStats(
    val strength: Int,
    val intelligence: Int,
    val stamina: Int,
    val hp: Int,
    val attack: Int,
    val defense: Int,
) {
    fun asRows(): List<Pair<String, Int>> = listOf(
        "forca" to strength,
        "inteligencia" to intelligence,
        "stamina" to stamina,
        "hp" to hp,
        "ataque" to attack,
        "defesa" to defense,
    )
}

object AdminGamesCatalog {
    const val MaxUsers = 80

    fun calculateLevel(xp: Int): Int {
        return when {
            xp < 50 -> 1
            xp < 150 -> 2
            xp < 350 -> 3
            xp < 750 -> 4
            xp < 1350 -> 5
            xp < 2150 -> 6
            else -> 6 + ((xp - 2150) / 1000)
        }
    }

    fun calculateStats(user: AdminArenaUser): AdminArenaStats {
        val stats = user.stats
        val currentLevel = calculateLevel(user.xp)
        fun value(key: String): Double = stats[key] ?: 0.0
        fun calc(base: Int, bonus: Double): Int = kotlin.math.floor(base + bonus).toInt()
        return AdminArenaStats(
            strength = calc(20, value("gymCheckins") * 0.1 + value("confirmedTrainings")),
            intelligence = calc(
                20,
                value("postsCount") * 0.1 +
                    value("commentsCount") * 0.1 +
                    value("albumCollected") * 0.1 +
                    value("followingCount") * 0.1,
            ),
            stamina = calc(
                50,
                value("loginCount") * 0.1 +
                    value("streak7Cycles") +
                    value("streak30Cycles") * 5 +
                    (value("eventsBought") + value("eventsAttended")) * 5 +
                    value("confirmedTrainings"),
            ),
            defense = calc(
                20,
                value("storeSpent") * 0.1 +
                    value("followersCount") * 0.1 +
                    if (user.planBadge.isNotBlank()) 30.0 else 0.0,
            ),
            attack = calc(25, value("arenaWins") * 0.1 - value("arenaLosses") * 0.05),
            hp = calc(200, currentLevel * 50.0),
        )
    }
}
