package com.example.usc1.ui.membershipCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc500

@Composable
fun MembershipCardScreen(
    state: MembershipCardUiState,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando identidade", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Carteirinha",
                subtitle = "Não foi possível carregar a identidade",
                icon = Icons.Outlined.CreditCard,
                onBackClick = onBackClick,
            )
            PremiumCard(accent = PremiumZinc500) {
                PremiumInfoRow("Erro", state.errorMessage)
            }
        }
        state.card.userName.isBlank() -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Carteirinha",
                subtitle = "Carteirinha digital oficial",
                icon = Icons.Outlined.CreditCard,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Carteirinha não carregada",
                subtitle = "Entre com Google e aguarde a sessão real do Supabase.",
                icon = Icons.Outlined.CreditCard,
            )
        }
        else -> PremiumScreen(
            modifier = modifier,
            bottomPadding = 92.dp,
        ) {
            PremiumHeader(
                title = "Identidade",
                subtitle = "Carteirinha digital oficial",
                icon = Icons.Outlined.CreditCard,
                onBackClick = onBackClick,
            )

            MembershipCard(
                card = state.card,
                modifier = Modifier.fillMaxWidth(),
            )

            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardInfoRowIfPresent("Status", state.card.memberStatus)
                    CardInfoRowIfPresent("Plano", state.card.planName)
                    CardInfoRowIfPresent("Validade", state.card.validUntil)
                    CardInfoRowIfPresent("Documento", state.card.memberCode)
                }
            }

            PremiumSecondaryButton(
                text = "Atualizar validação",
                onClick = onRefreshClick,
                icon = Icons.Outlined.VerifiedUser,
            )
            PremiumSecondaryButton(
                text = "Voltar",
                onClick = onBackClick,
                icon = Icons.Outlined.ArrowBack,
            )
        }
    }
}

@Composable
private fun CardInfoRowIfPresent(label: String, value: String) {
    if (value.isNotBlank()) {
        PremiumInfoRow(label, value)
    }
}
