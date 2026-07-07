package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun BannedUserScreen(
    state: AuthUiState,
    onSupportClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.Block,
        title = "Conta bloqueada",
        subtitle = "O acesso desta conta foi suspenso. Dados sensíveis e áreas internas ficam indisponíveis.",
    ) {
        AuthInlineMessage(
            text = "Usuário: ${state.session.user?.email ?: "não informado"}\nStatus: bloqueado pela moderação",
        )
        Button(
            onClick = onSupportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Falar com suporte")
        }
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sair da conta")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BannedUserScreenPreview() {
    UscTheme {
        BannedUserScreen(
            state = AuthUiState(
                session = UserSession(
                    user = AuthUser(
                        id = "preview",
                        name = "Usuário bloqueado",
                        email = "banido@usc.app",
                        status = UserStatus.Banned,
                    ),
                    status = AuthStatus.Banned,
                ),
            ),
            onSupportClick = {},
            onSignOutClick = {},
        )
    }
}
