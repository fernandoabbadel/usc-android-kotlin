package com.example.usc1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseSettingsRepository
import com.example.usc1.domain.model.SettingsSupportCategory
import com.example.usc1.domain.model.SettingsSupportTicket
import com.example.usc1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsSupportUiState(
    val tickets: List<SettingsSupportTicket> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String = "",
    val statusMessage: String = "",
)

class SettingsSupportViewModel(
    private val repository: SettingsRepository = SupabaseSettingsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsSupportUiState(isLoading = true))
    val uiState: StateFlow<SettingsSupportUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val userId = session.user?.id.orEmpty().trim()
        if (userId.isBlank()) {
            lastLoadKey = ""
            _uiState.update { it.copy(tickets = emptyList(), isLoading = false) }
            return
        }
        if (!forceRefresh && userId == lastLoadKey && !_uiState.value.isLoading) return
        lastLoadKey = userId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            runCatching { repository.getSupportTickets(userId = userId) }
                .onSuccess { tickets ->
                    _uiState.update { it.copy(tickets = tickets, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            tickets = emptyList(),
                            isLoading = false,
                            errorMessage = error.message.orEmpty()
                                .ifBlank { "Não foi possível carregar seus chamados." },
                        )
                    }
                }
        }
    }

    fun submit(
        session: UserSession,
        category: SettingsSupportCategory,
        subject: String,
        message: String,
    ) {
        val user = session.user ?: return
        if (_uiState.value.isSending) return
        if (subject.isBlank() || message.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha assunto e mensagem.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = "") }
            runCatching {
                repository.submitSupportTicket(
                    tenantId = session.tenant?.id.orEmpty(),
                    userId = user.id,
                    userName = user.name,
                    userEmail = user.email,
                    category = category,
                    subject = subject,
                    message = message,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isSending = false, statusMessage = "Chamado enviado com sucesso.")
                }
                load(session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message.orEmpty()
                            .ifBlank { "Erro ao enviar chamado." },
                    )
                }
            }
        }
    }

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = "") }
    }
}
