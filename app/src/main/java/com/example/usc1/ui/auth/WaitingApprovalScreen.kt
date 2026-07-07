package com.example.usc1.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumSecondaryButton
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
        subtitle = "Seu cadastro foi recebido. A diretoria precisa liberar o acesso interno.",
    ) {
        AuthInlineMessage(
            text = "Atlética: $tenantName\nStatus: análise da diretoria",
        )
        PremiumPrimaryButton(
            text = "Atualizar status",
            onClick = onRefreshClick,
            enabled = !state.isLoading,
            loading = state.isLoading,
            accent = PremiumAmber,
            icon = Icons.Outlined.Refresh,
        )
        PremiumSecondaryButton(
            text = "Sair da conta",
            onClick = onSignOutClick,
            accent = PremiumAmber,
            icon = Icons.Outlined.Logout,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF02050D)
@Composable
fun WaitingApprovalScreenPreview() {
    UscTheme(darkTheme = true) {
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
