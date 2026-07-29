package com.example.usc1.ui.games

data class RankingEntry(
    val name: String,
    val score: String,
    val subtitle: String,
)

data class Achievement(
    val title: String,
    val description: String,
    val progress: Float,
    val unlocked: Boolean,
)

data class LoyaltyReward(
    val title: String,
    val costLabel: String,
    val progress: Float,
)

data class GamesUiState(
    val xpLabel: String = "0 XP",
    val levelLabel: String = "Cardume",
    val rankings: List<RankingEntry> = GamesMockData.rankings,
    val achievements: List<Achievement> = GamesMockData.achievements,
    val rewards: List<LoyaltyReward> = GamesMockData.rewards,
    val matchesLabel: String = "0",
    val winsLabel: String = "0",
    val streakLabel: String = "0 dias",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

object GamesMockData {
    val rankings = listOf(
        RankingEntry("Fernando USC", "9.840 XP", "1º no Boardround"),
        RankingEntry("Ana Costa", "8.720 XP", "Sequência de 12 dias"),
        RankingEntry("Lívia Martins", "7.410 XP", "Top comunidade"),
    )

    val achievements = listOf(
        Achievement("Barracuda", "Alcance 2.000 XP em eventos, loja e treinos.", 1f, true),
        Achievement("Elite Verde", "Chegue a 15.000 XP no ecossistema USC.", 0.49f, false),
        Achievement("Megalodon", "Patente máxima da sala de troféus.", 0.15f, false),
    )

    val rewards = listOf(
        LoyaltyReward("Selo Cardume", "1.000 XP", 1f),
        LoyaltyReward("Cupom Loja", "2.500 XP", 0.74f),
        LoyaltyReward("Fila Premium", "5.000 XP", 0.38f),
    )
}
