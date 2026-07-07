package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
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
        subtitle = "Recuperação de acesso e validação de sessão sem salvar senhas ou segredos no app.",
    ) {
        AuthInlineMessage(
            text = "Na versão real, a recuperação será feita por fluxo seguro do Supabase Auth. Nenhuma senha, token ou segredo será salvo no app.",
        )
        PremiumPrimaryButton(
            text = "Recuperar acesso",
            onClick = onRecoverAccountClick,
            icon = Icons.Outlined.RestartAlt,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF02050D)
@Composable
private fun AccountSecurityScreenPreview() {
    UscTheme(darkTheme = true) {
        AccountSecurityScreen(
            onBackClick = {},
            onRecoverAccountClick = {},
        )
    }
}
