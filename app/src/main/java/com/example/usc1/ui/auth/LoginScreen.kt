package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.Login,
        title = "Entrar na sua atlética",
        subtitle = "Acesse eventos, loja, carteirinha, treinos e comunidade em uma experiência Android nativa.",
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        state.errorMessage?.let { message ->
            AuthInlineMessage(text = message)
        }

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmitLogin,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Entrar")
            }
        }

        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Criar conta")
        }

        TextButton(
            onClick = onSecurityClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Segurança da conta")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Cenários mockados",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onMockWaitingClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Usuário aguardando aprovação")
            }
            OutlinedButton(
                onClick = onMockInviteClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Convite necessário")
            }
            OutlinedButton(
                onClick = onMockBannedClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Usuário banido")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    UscTheme {
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
