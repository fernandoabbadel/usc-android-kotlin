package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.ui.theme.UscTheme

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
        subtitle = "Esta atlética exige um convite válido antes de liberar cadastro e acesso aos módulos internos.",
    ) {
        AuthInlineMessage(
            text = "Peça um convite para a diretoria ou use o link enviado pela sua turma.",
        )
        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Inserir convite")
        }
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Voltar para login")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InviteRequiredScreenPreview() {
    UscTheme {
        InviteRequiredScreen(
            state = AuthUiState(
                session = UserSession(
                    user = AuthUser(
                        id = "preview",
                        name = "Visitante",
                        email = "visitante@usc.app",
                    ),
                    status = AuthStatus.InviteRequired,
                ),
            ),
            onRegisterClick = {},
            onSignOutClick = {},
        )
    }
}
