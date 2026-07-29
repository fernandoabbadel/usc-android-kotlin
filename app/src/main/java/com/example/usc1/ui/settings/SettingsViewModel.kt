package com.example.usc1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseSettingsRepository
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository = SupabaseSettingsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var lastInviteDashboardKey: String = ""
    private var lastMentorshipKey: String = ""

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setInviteState(invite: SettingsInviteUiModel) {
        _uiState.update { it.copy(invitePanel = invite) }
    }

    fun setAccountActionLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isAccountActionLoading = isLoading) }
    }

    fun loadInviteDashboard(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastInviteDashboardKey = ""
            _uiState.update {
                it.copy(
                    inviteDashboard = SettingsInviteDashboard(),
                    isInviteDashboardLoading = false,
                    inviteDashboardError = "",
                )
            }
            return
        }
        if (!forceRefresh && key == lastInviteDashboardKey && _uiState.value.inviteDashboard.entries.isNotEmpty()) {
            return
        }
        lastInviteDashboardKey = key
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInviteDashboardLoading = true,
                    inviteDashboardError = "",
                )
            }
            runCatching {
                repository.getInviteDashboard(tenantId = tenantId, userId = userId)
            }.onSuccess { dashboard ->
                _uiState.update {
                    it.copy(
                        inviteDashboard = dashboard,
                        isInviteDashboardLoading = false,
                        inviteDashboardError = "",
                        invitePanel = it.invitePanel.copy(
                            remainingToday = dashboard.remainingToday,
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isInviteDashboardLoading = false,
                        inviteDashboardError = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar seus convites agora."
                        },
                    )
                }
            }
        }
    }

    fun loadMentorshipHub(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastMentorshipKey = ""
            _uiState.update {
                it.copy(
                    mentorshipHub = SettingsMentorshipHub(),
                    isMentorshipLoading = false,
                    mentorshipError = "",
                )
            }
            return
        }
        if (!forceRefresh && key == lastMentorshipKey && _uiState.value.mentorshipHub.hasContent) {
            return
        }
        lastMentorshipKey = key
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMentorshipLoading = true,
                    mentorshipError = "",
                )
            }
            runCatching {
                repository.getMentorshipHub(tenantId = tenantId, userId = userId)
            }.onSuccess { hub ->
                _uiState.update {
                    it.copy(
                        mentorshipHub = hub,
                        isMentorshipLoading = false,
                        mentorshipError = "",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isMentorshipLoading = false,
                        mentorshipError = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o apadrinhamento agora."
                        },
                    )
                }
            }
        }
    }
}

private val SettingsMentorshipHub.hasContent: Boolean
    get() = mentor != null || mentee != null || incoming.isNotEmpty() || outgoing.isNotEmpty()
