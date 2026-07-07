package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.ui.theme.UscTheme

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onMockWaitingClick: () -> Unit,
    onMockInviteClick: () -> Unit,
    onMockBannedClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.Login,
        title = "Acesso USC",
        subtitle = "Entre para acessar eventos, carteirinha, loja, treinos e comunidade.",
    ) {
        PremiumTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "E-mail",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = Icons.Outlined.Email,
        )
        PremiumTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Senha",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = Icons.Outlined.Password,
        )

        state.errorMessage?.let { message ->
            AuthInlineMessage(text = message)
        }

        PremiumPrimaryButton(
            text = "Entrar",
            onClick = onLoginClick,
            enabled = state.canSubmitLogin,
            loading = state.isLoading,
            icon = Icons.Outlined.Login,
        )

        PremiumSecondaryButton(
            text = "Criar conta",
            onClick = onRegisterClick,
            icon = Icons.Outlined.PersonAdd,
        )

        PremiumSecondaryButton(
            text = "Segurança da conta",
            onClick = onSecurityClick,
            icon = Icons.Outlined.Security,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthInlineMessage(text = "Cenários mockados para validar RouteGuard sem Supabase real.")
            PremiumSecondaryButton(
                text = "Usuário aguardando aprovação",
                onClick = onMockWaitingClick,
                icon = Icons.Outlined.HourglassTop,
            )
            PremiumSecondaryButton(
                text = "Convite necessário",
                onClick = onMockInviteClick,
                icon = Icons.Outlined.MarkEmailUnread,
            )
            PremiumSecondaryButton(
                text = "Usuário banido",
                onClick = onMockBannedClick,
                icon = Icons.Outlined.Block,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF02050D)
@Composable
fun LoginScreenPreview() {
    UscTheme(darkTheme = true) {
        LoginScreen(
            state = AuthUiState(
                email = "membro@usc.app",
                password = "123456",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onRegisterClick = {},
            onSecurityClick = {},
            onMockWaitingClick = {},
            onMockInviteClick = {},
            onMockBannedClick = {},
        )
    }
}
