package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc500

@Composable
fun LoginScreen(
    state: AuthUiState,
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.Login,
        title = "Acesso",
        subtitle = "O login por email institucional foi pausado. Entre com Google para continuar.",
    ) {
        state.errorMessage?.let { message ->
            AuthInlineMessage(text = message)
        }
        state.statusMessage?.let { message ->
            AuthInlineMessage(text = message)
        }

        PremiumPrimaryButton(
            text = "Entrar com Google",
            onClick = onGoogleClick,
            enabled = !state.isLoading && !state.isWaitingForOAuthRedirect,
            loading = state.isLoading,
            icon = Icons.Outlined.Login,
        )

        PremiumSecondaryButton(
            text = "Apenas dar uma espiadinha (Visitante)",
            onClick = onGuestClick,
            icon = Icons.Outlined.Visibility,
        )

        Text(
            text = "Ao entrar, você concorda com nossos Termos de Serviço e Política de Privacidade",
            color = PremiumZinc500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
