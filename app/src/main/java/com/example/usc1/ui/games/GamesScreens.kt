package com.example.usc1.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.R
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.ui.theme.UscTheme

@Composable
fun GamesScreen(state: GamesUiState, onBoardroundClick: () -> Unit, onAchievementsClick: () -> Unit, onLoyaltyClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Games", subtitle = "XP, ranking e progresso", icon = Icons.Outlined.Games)
        NativeModuleHeroCard("ARENA USC", state.levelLabel, "Gamificação, boardround, conquistas e fidelidade.", R.drawable.battle_forest)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard("XP", state.xpLabel, icon = Icons.Outlined.Star, modifier = Modifier.weight(1f))
            NativeStatCard("Patente", state.levelLabel, icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber, modifier = Modifier.weight(1f))
        }
        NativeActionCard(NativeAction("Boardround", "Ranking, estatísticas e quizzes.", Icons.Outlined.Leaderboard), onBoardroundClick)
        NativeActionCard(NativeAction("Conquistas", "Badges, patentes e progresso.", Icons.Outlined.EmojiEvents), onAchievementsClick)
        NativeActionCard(NativeAction("Fidelidade", "Selos, prêmios e resgates.", Icons.Outlined.Star, PremiumAmber), onLoyaltyClick)
    }
}

@Composable
fun BoardroundScreen(state: GamesUiState, onRankingClick: () -> Unit, onStatsClick: () -> Unit, onRulesClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Boardround", subtitle = "Ranking e estatísticas", icon = Icons.Outlined.Leaderboard)
        state.rankings.forEachIndexed { index, entry -> RankingCard(entry = entry, index = index) }
        NativeActionCard(NativeAction("Ranking completo", "Classificação geral.", Icons.Outlined.Leaderboard), onRankingClick)
        NativeActionCard(NativeAction("Estatísticas", "Vitórias, sequência e taxa.", Icons.Outlined.BarChart), onStatsClick)
        NativeActionCard(NativeAction("Regras", "Resumo de pontuação.", Icons.Outlined.Games), onRulesClick)
    }
}

@Composable
fun BoardroundRankingScreen(state: GamesUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Ranking", subtitle = "Boardround", icon = Icons.Outlined.Leaderboard, onBackClick = onBackClick)
        state.rankings.forEachIndexed { index, entry -> RankingCard(entry = entry, index = index) }
    }
}

@Composable
fun BoardroundStatsScreen(state: GamesUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Estatísticas", subtitle = "Performance mockada", icon = Icons.Outlined.BarChart, onBackClick = onBackClick)
        NativeStatCard("Partidas", "48", icon = Icons.Outlined.Games)
        NativeStatCard("Vitórias", "31", icon = Icons.Outlined.EmojiEvents)
        NativeStatCard("Sequência", "7 dias", icon = Icons.Outlined.Star, accent = PremiumAmber)
    }
}

@Composable
fun AchievementsScreen(state: GamesUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Conquistas", subtitle = state.levelLabel, icon = Icons.Outlined.EmojiEvents, onBackClick = onBackClick)
        state.achievements.forEach { achievement -> AchievementCard(achievement = achievement) }
    }
}

@Composable
fun LoyaltyScreen(state: GamesUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Fidelidade", subtitle = "Selos e prêmios", icon = Icons.Outlined.Star, onBackClick = onBackClick)
        state.rewards.forEach { reward -> LoyaltyCard(reward = reward) }
    }
}

@Composable
fun GameRulesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Regras", subtitle = "GAME_RULES.md resumido", icon = Icons.Outlined.Games, onBackClick = onBackClick)
        NativeSectionTitle(title = "Pontuação")
        NativeStatCard("Vitória", "+120 XP", icon = Icons.Outlined.EmojiEvents)
        NativeStatCard("Participação", "+30 XP", icon = Icons.Outlined.Games)
        NativeStatCard("Sequência", "Bônus progressivo", icon = Icons.Outlined.Star)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun GamesScreenPreview() {
    UscTheme(darkTheme = true) {
        GamesScreen(GamesUiState(), {}, {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun BoardroundScreenPreview() {
    UscTheme(darkTheme = true) {
        BoardroundScreen(GamesUiState(), {}, {}, {})
    }
}
