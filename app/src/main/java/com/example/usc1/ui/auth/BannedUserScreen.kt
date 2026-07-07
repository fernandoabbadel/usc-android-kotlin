package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumSecondaryButton
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
        subtitle = "O acesso desta conta foi suspenso. Áreas internas e dados sensíveis ficam indisponíveis.",
    ) {
        AuthInlineMessage(
            text = "Usuário: ${state.session.user?.email ?: "não informado"}\nStatus: bloqueado pela moderação",
        )
        PremiumPrimaryButton(
            text = "Falar com suporte",
            onClick = onSupportClick,
            accent = PremiumRed,
            icon = Icons.Outlined.Chat,
        )
        PremiumSecondaryButton(
            text = "Sair da conta",
            onClick = onSignOutClick,
            accent = PremiumRed,
            icon = Icons.Outlined.Logout,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF02050D)
@Composable
fun BannedUserScreenPreview() {
    UscTheme(darkTheme = true) {
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
