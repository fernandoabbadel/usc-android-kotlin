package com.example.usc1.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setInviteState(invite: SettingsInviteUiModel) {
        _uiState.update { it.copy(invitePanel = invite) }
    }

    fun setAccountActionLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isAccountActionLoading = isLoading) }
    }
}
