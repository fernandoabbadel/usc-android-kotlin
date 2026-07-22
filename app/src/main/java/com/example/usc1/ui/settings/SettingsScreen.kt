package com.example.usc1.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.tenant.TenantPalette

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onItemClick: (SettingsItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNotificationPreferenceChange: (Boolean) -> Unit = {},
    onCreateInviteClick: () -> Unit = {},
    onOpenInvitesClick: (() -> Unit)? = null,
    onCopyInviteClick: (String) -> Unit = {},
    onToggleAccountClick: () -> Unit = {},
    onSignOutClick: (() -> Unit)? = null,
    onDeleteAccountClick: () -> Unit = {},
) {
    var notificationsEnabled by rememberSaveable(state.notificationsEnabled) {
        mutableStateOf(state.notificationsEnabled)
    }

    val membershipItem = SettingsItemUiModel(
        title = "Carteirinha",
        description = "Identidade digital",
        action = SettingsAction.Membership,
    )
    val invitesItem = state.sections
        .flatMap(SettingsSectionUiModel::items)
        .firstOrNull { it.action == SettingsAction.Invites }
        ?: SettingsItemUiModel("Meus Convites", action = SettingsAction.Invites)
    val signOutItem = SettingsItemUiModel("Sair da conta", action = SettingsAction.SignOut)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxSize(),
        ) {
            SettingsStickyHeader(onBackClick = onBackClick)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 112.dp,
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
            ) {
                item(key = "settings-profile") {
                    SettingsProfileCard(
                        state = state,
                        onMembershipClick = { onItemClick(membershipItem) },
                    )
                }

                if (state.invitePanel.isVisible) {
                    item(key = "settings-invite") {
                        SettingsInvitePanel(
                            state = state.invitePanel,
                            tenantName = state.tenantName,
                            onCreateInviteClick = onCreateInviteClick,
                            onOpenInvitesClick = {
                                onOpenInvitesClick?.invoke() ?: onItemClick(invitesItem)
                            },
                            onCopyInviteClick = onCopyInviteClick,
                        )
                    }
                }

                state.sections.forEachIndexed { index, section ->
                    item(key = "settings-section-$index-${section.title}") {
                        SettingsSection(
                            section = section,
                            notificationsEnabled = notificationsEnabled,
                            onNotificationChange = { enabled ->
                                notificationsEnabled = enabled
                                onNotificationPreferenceChange(enabled)
                            },
                            onItemClick = onItemClick,
                        )
                    }
                }

                item(key = "settings-risk-zone") {
                    SettingsRiskZone(
                        state = state,
                        onToggleAccountClick = onToggleAccountClick,
                        onSignOutClick = {
                            onSignOutClick?.invoke() ?: onItemClick(signOutItem)
                        },
                        onDeleteAccountClick = onDeleteAccountClick,
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = state.tenantPalette.settingsAccent(),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(
        state = SettingsUiState(
            userName = "Fernando Lopes Abbade",
            userEmail = "fernando@example.com",
            userInitials = "FL",
            classLabel = "T2",
            tenantName = "Atlética de Medicina",
            tenantPalette = TenantPalette.Green,
            roleLabel = "Master",
            planLabel = "Atleta",
            planColorKey = "emerald",
            statusLabel = "ativo",
            userIdLabel = "3E4FC3CA",
            invitePanel = SettingsInviteUiModel(
                isVisible = true,
                remainingToday = 5,
            ),
        ),
        onItemClick = {},
    )
}
