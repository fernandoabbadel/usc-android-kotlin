package com.example.usc1.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminArenaUser
import com.example.usc1.domain.model.AdminGamesCatalog

@Composable
fun AdminGamesScreen(
    state: AdminGamesUiState,
    onSearchChange: (String) -> Unit,
    onUserClick: (AdminArenaUser) -> Unit,
    onCloseUserClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.users.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = "Admin Arena",
            subtitle = "Em breve",
            icon = Icons.Outlined.SportsEsports,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )
        PremiumChip(label = "Em breve", icon = Icons.Outlined.Schedule, accent = PremiumAmber)
        state.errorMessage?.let { message ->
            Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        PremiumTextField(value = state.searchTerm, onValueChange = onSearchChange, label = "Buscar atleta...")
        if (state.filteredUsers.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum atleta encontrado.",
                subtitle = "A consulta do tenant ativo não retornou usuários para este filtro.",
                icon = Icons.Outlined.SportsEsports,
                accent = PremiumAmber,
            )
        } else {
            state.filteredUsers.forEach { user ->
                AdminArenaUserRow(user = user, onClick = { onUserClick(user) })
            }
        }
        state.selectedUser?.let { user ->
            AdminArenaUserStatsCard(user = user, onCloseClick = onCloseUserClick)
        }
        PremiumSecondaryButton(text = "Atualizar", onClick = onRefreshClick, icon = Icons.Outlined.Refresh)
    }
}

@Composable
private fun AdminArenaUserRow(
    user: AdminArenaUser,
    onClick: () -> Unit,
) {
    PremiumCard(
        accent = PremiumAmber,
        containerColor = PremiumZinc900,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = user.className, color = PremiumZinc500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "Ver Stats >", color = Color(0xFF10B981), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun AdminArenaUserStatsCard(
    user: AdminArenaUser,
    onCloseClick: () -> Unit,
) {
    val stats = AdminGamesCatalog.calculateStats(user)
    PremiumCard(accent = PremiumAmber, containerColor = PremiumZinc900) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = user.name.uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(text = "Fechar", onClick = onCloseClick, icon = Icons.Outlined.Close, modifier = Modifier.weight(1f))
        }
        stats.asRows().forEach { (stat, value) ->
            PremiumCard(accent = Color(0xFF10B981), containerColor = Color.Black) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stat.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value.toString(),
                        color = Color(0xFF10B981),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = "Baseado em: ${user.stats}",
                    color = PremiumZinc400,
                    fontSize = 10.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
