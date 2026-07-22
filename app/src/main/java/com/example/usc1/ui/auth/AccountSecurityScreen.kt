package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton

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
