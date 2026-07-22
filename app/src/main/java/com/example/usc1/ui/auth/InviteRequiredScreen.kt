package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton

@Composable
fun InviteRequiredScreen(
    state: AuthUiState,
    onRegisterClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.MarkEmailUnread,
        title = "Convite necessário",
        subtitle = "Esta atlética exige link ou código válido antes de liberar cadastro e módulos internos.",
    ) {
        AuthInlineMessage(
            text = "Peça um convite para a diretoria ou use o link enviado pela sua turma.",
        )
        PremiumPrimaryButton(
            text = "Inserir convite",
            onClick = onRegisterClick,
            icon = Icons.Outlined.PersonAdd,
        )
        PremiumSecondaryButton(
            text = "Voltar para login",
            onClick = onSignOutClick,
            icon = Icons.Outlined.Login,
        )
    }
}
