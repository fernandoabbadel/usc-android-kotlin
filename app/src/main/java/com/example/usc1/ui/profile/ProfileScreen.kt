package com.example.usc1.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumMenuRow
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.ui.theme.UscTheme

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onShortcutClick: (ProfileShortcutUiModel) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando perfil", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Perfil",
                subtitle = "Não foi possível carregar seus dados",
                icon = Icons.Outlined.Person,
            )
            PremiumEmptyState(
                title = "Erro no perfil",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Person,
            )
            com.example.usc1.core.ui.PremiumPrimaryButton(
                text = "Tentar novamente",
                onClick = onRetryClick,
            )
        }
        else -> ProfileLoadedContent(
            state = state,
            onShortcutClick = onShortcutClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileLoadedContent(
    state: ProfileUiState,
    onShortcutClick: (ProfileShortcutUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Perfil",
            subtitle = "Dados do sócio USC",
            icon = Icons.Outlined.Person,
        )

        ProfileHeroCard(profile = profile)

        PremiumCard {
            PremiumInfoRow("Curso", profile.course)
            PremiumInfoRow("Turma", profile.className)
            PremiumInfoRow("Atlética", profile.tenantName)
            PremiumInfoRow("Plano ativo", profile.activePlan)
            PremiumInfoRow("Membro desde", profile.memberSince)
        }

        Text(
            text = "ATALHOS DO PERFIL",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 2.dp),
        )
        state.shortcuts.forEach { shortcut ->
            PremiumMenuRow(
                title = shortcut.title,
                subtitle = shortcut.description,
                icon = shortcutIcon(shortcut),
                onClick = { onShortcutClick(shortcut) },
            )
        }
    }
}

@Composable
private fun ProfileHeroCard(profile: ProfileUserUiModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF18181B),
                        Color.Black,
                    ),
                ),
            )
            .border(1.dp, PremiumZinc800, RoundedCornerShape(32.dp))
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(130.dp)
                .clip(CircleShape)
                .background(PremiumBrand.copy(alpha = 0.10f)),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(86.dp),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(3.dp, PremiumBrand),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_aaakn),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(2.dp, Color.Black),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_usc),
                        contentDescription = "Turma",
                        modifier = Modifier.padding(3.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = profile.email,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PremiumChip(label = profile.role)
                    PremiumChip(label = profile.accountStatus)
                }
            }
        }
    }
}

private fun shortcutIcon(shortcut: ProfileShortcutUiModel) =
    when {
        shortcut.title.contains("Carteirinha", ignoreCase = true) -> Icons.Outlined.CreditCard
        shortcut.title.contains("Plano", ignoreCase = true) -> Icons.Outlined.CreditCard
        shortcut.title.contains("Evento", ignoreCase = true) -> Icons.Outlined.Event
        shortcut.title.contains("Loja", ignoreCase = true) -> Icons.Outlined.Storefront
        shortcut.title.contains("Config", ignoreCase = true) -> Icons.Outlined.Settings
        else -> Icons.Outlined.Person
    }

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun ProfileScreenPreview() {
    UscTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(),
            onShortcutClick = {},
            onRetryClick = {},
        )
    }
}
