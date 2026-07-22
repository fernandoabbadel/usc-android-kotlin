package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onInviteCodeChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.PersonAdd,
        title = "Convite USC",
        subtitle = "Insira o código enviado pela atlética para solicitar acesso ao tenant correto.",
    ) {
        PremiumTextField(
            value = state.inviteCode,
            onValueChange = onInviteCodeChange,
            label = "Código de convite",
            leadingIcon = Icons.Outlined.Badge,
        )

        state.errorMessage?.let { message ->
            AuthInlineMessage(text = message)
        }

        PremiumPrimaryButton(
            text = "Enviar convite",
            onClick = onRegisterClick,
            enabled = state.canSubmitRegister,
            loading = state.isLoading,
            icon = Icons.Outlined.PersonAdd,
        )

        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackToLoginClick,
        )
    }
}
