package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        subtitle = "Crie seu acesso para a atlética. Algumas contas precisam de convite ou aprovação da diretoria.",
    ) {
        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome completo") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = state.inviteCode,
            onValueChange = onInviteCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Código de convite") },
            singleLine = true,
        )

        state.errorMessage?.let { message ->
            AuthInlineMessage(text = message)
        }

        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmitRegister,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Enviar cadastro")
            }
        }

        TextButton(
            onClick = onBackToLoginClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Já tenho conta")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    UscTheme {
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
