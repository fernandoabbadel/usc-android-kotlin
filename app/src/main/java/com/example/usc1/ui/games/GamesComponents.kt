package com.example.usc1.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.NativeProgressBar
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun RankingCard(entry: RankingEntry, index: Int, modifier: Modifier = Modifier) {
    val accent = if (index == 0) PremiumAmber else PremiumBrand
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = PremiumZinc900, border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Row(modifier = Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${index + 1}", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = entry.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(text = entry.subtitle, color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = entry.score, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val accent = if (achievement.unlocked) PremiumBrand else PremiumPurple
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = PremiumZinc900, border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (achievement.unlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Star, contentDescription = null, tint = accent)
                Text(text = achievement.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                PremiumChip(label = if (achievement.unlocked) "Conquistado" else "Bloqueado", accent = accent)
            }
            Text(text = achievement.description, color = PremiumZinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            NativeProgressBar(progress = achievement.progress, accent = accent)
        }
    }
}

@Composable
fun LoyaltyCard(reward: LoyaltyReward, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = PremiumZinc900, border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.26f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = reward.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                PremiumChip(label = reward.costLabel, icon = Icons.Outlined.EmojiEvents, accent = PremiumAmber)
            }
            NativeProgressBar(progress = reward.progress)
            Text(text = "Progresso de fidelidade", color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun AchievementCardPreview() {
    UscTheme(darkTheme = true) {
        AchievementCard(GamesMockData.achievements.first(), modifier = Modifier.padding(16.dp))
    }
}
