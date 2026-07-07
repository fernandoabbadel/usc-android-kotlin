package com.example.usc1.ui.auth

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.ui.theme.UscTheme

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.PersonAdd,
        title = "Cadastro USC",
        subtitle = "Complete sua ficha para entrar na atlética. Convites e aprovação seguem mockados nesta fase.",
    ) {
        PremiumTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = "Nome completo",
            leadingIcon = Icons.Outlined.Person,
        )
        PremiumTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "E-mail",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = Icons.Outlined.Email,
        )
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
            text = "Enviar cadastro",
            onClick = onRegisterClick,
            enabled = state.canSubmitRegister,
            loading = state.isLoading,
            icon = Icons.Outlined.PersonAdd,
        )

        PremiumSecondaryButton(
            text = "Já tenho conta",
            onClick = onBackToLoginClick,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF02050D)
@Composable
fun RegisterScreenPreview() {
    UscTheme(darkTheme = true) {
        RegisterScreen(
            state = AuthUiState(
                fullName = "Fernando Abbadel",
                email = "fernando@usc.app",
                inviteCode = "AAAKN-2026",
            ),
            onFullNameChange = {},
            onEmailChange = {},
            onInviteCodeChange = {},
            onRegisterClick = {},
            onBackToLoginClick = {},
        )
    }
}
