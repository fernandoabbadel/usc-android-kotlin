package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.ui.theme.UscTheme

@Composable
fun WaitingApprovalScreen(
    state: AuthUiState,
    onRefreshClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tenantName = state.session.tenant?.name ?: "sua atlética"

    AuthScreenShell(
        modifier = modifier,
        icon = Icons.Outlined.HourglassTop,
        title = "Aguardando aprovação",
        subtitle = "Seu cadastro foi recebido e precisa ser aprovado antes de liberar o app.",
    ) {
        AuthInlineMessage(
            text = "Atlética: $tenantName\nStatus: análise da diretoria",
        )
        Button(
            onClick = onRefreshClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text("Atualizar status")
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
fun WaitingApprovalScreenPreview() {
    UscTheme {
        WaitingApprovalScreen(
            state = AuthUiState(
                session = UserSession(
                    user = AuthUser(
                        id = "preview",
                        name = "Membro em análise",
                        email = "pendente@usc.app",
                    ),
                    tenant = TenantContext(
                        id = "aaakn",
                        slug = "aaakn",
                        name = "AAAKN USC",
                        membershipStatus = TenantMembershipStatus.Pending,
                    ),
                    status = AuthStatus.WaitingApproval,
                ),
            ),
            onRefreshClick = {},
            onSignOutClick = {},
        )
    }
}
