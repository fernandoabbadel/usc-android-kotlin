package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.ui.theme.UscTheme

@Composable
fun AccountSecurityScreen(
    onBackClick: () -> Unit,
    onRecoverAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.Security,
        title = "Segurança da conta",
        subtitle = "Recuperação de acesso, validação de sessão e boas práticas antes da integração real com Supabase.",
    ) {
        AuthInlineMessage(
            text = "Na versão real, a recuperação será feita por fluxo seguro do Supabase Auth. Nenhuma senha ou segredo será salvo no app.",
        )
        Button(
            onClick = onRecoverAccountClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Recuperar acesso")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Voltar")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSecurityScreenPreview() {
    UscTheme {
        AccountSecurityScreen(
            onBackClick = {},
            onRecoverAccountClick = {},
        )
    }
}
